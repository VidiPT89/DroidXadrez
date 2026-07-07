package com.vidi.droidxadrez.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidi.droidxadrez.SoundEngine
import com.vidi.droidxadrez.engine.BotDifficulty
import com.vidi.droidxadrez.engine.ChessAI
import com.vidi.droidxadrez.engine.ChessGame
import com.vidi.droidxadrez.engine.GameStatus
import com.vidi.droidxadrez.engine.Move
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.engine.MoveRecord
import com.vidi.droidxadrez.engine.PieceType
import com.vidi.droidxadrez.engine.Square
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameMode { ONE_V_ONE, BOT }

private const val BOT_MIN_DELAY_MS = 550L

class GameViewModel : ViewModel() {
    companion object {
        val BOT_COLOR = PieceColor.BLACK
    }

    // neverEqualPolicy: ChessGame is mutated in place, so every reassignment must notify observers
    // even when the reference is unchanged.
    var game by mutableStateOf(ChessGame(), neverEqualPolicy())
        private set
    var pieces by mutableStateOf<List<PieceInstance>>(emptyList())
        private set
    var selected by mutableStateOf<Square?>(null)
        private set
    var legalTargets by mutableStateOf<List<Move>>(emptyList())
        private set
    var flipped by mutableStateOf(false)
    var lastMove by mutableStateOf<Pair<Square, Square>?>(null)
        private set
    var mode by mutableStateOf(GameMode.ONE_V_ONE)
        private set
    var botLevel by mutableStateOf(BotDifficulty.MEDIUM)
        private set
    var thinking by mutableStateOf(false)
        private set
    var promotionCandidates by mutableStateOf<List<Move>?>(null)
        private set
    var showResult by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
        private set

    private var requestToken: UUID = UUID.randomUUID()
    private val redoStack = ArrayDeque<List<MoveRecord>>()

    fun newGame(mode: GameMode, level: BotDifficulty = BotDifficulty.MEDIUM) {
        this.mode = mode
        this.botLevel = level
        requestToken = UUID.randomUUID()
        game = ChessGame()
        pieces = PieceInstance.fresh(game.board)
        selected = null
        legalTargets = emptyList()
        lastMove = null
        thinking = false
        promotionCandidates = null
        showResult = false
        redoStack.clear()
        canRedo = false
    }

    fun statusText(): GameStatus = game.gameStatusText()

    fun kingInCheckSquare(): Square? = if (game.isInCheck(game.turn)) game.findKing(game.turn) else null

    fun tapSquare(square: Square) {
        if (thinking || game.isGameOver) return
        val sel = selected
        if (sel != null) {
            val candidates = legalTargets.filter { it.to == square }
            if (candidates.isNotEmpty()) {
                attemptMove(candidates)
                return
            }
        }
        val piece = game.pieceAt(square.r, square.c)
        if (piece != null && piece.color == game.turn) {
            selected = square
            legalTargets = game.legalMoves(square)
            SoundEngine.playClick()
        } else {
            selected = null
            legalTargets = emptyList()
        }
    }

    private fun attemptMove(candidates: List<Move>) {
        if (candidates.size > 1) {
            promotionCandidates = candidates
            return
        }
        finalize(candidates[0])
    }

    fun choosePromotion(type: PieceType) {
        val candidates = promotionCandidates ?: return
        val chosen = candidates.firstOrNull { it.promotion == type } ?: candidates[0]
        promotionCandidates = null
        finalize(chosen)
    }

    private fun finalize(move: Move) {
        val wasCapture = move.capture
        val record = game.makeMove(move.from, move.to, move.promotion) ?: return
        selected = null
        legalTargets = emptyList()
        lastMove = move.from to move.to
        redoStack.clear()
        canRedo = false
        game = game // re-signal: same reference, but neverEqualPolicy notifies observers
        pieces = applyMoveToPieces(pieces, move)

        when {
            record.status == "check" -> SoundEngine.playCheck()
            game.isGameOver -> SoundEngine.playEnd()
            wasCapture -> SoundEngine.playCapture()
            else -> SoundEngine.playMove()
        }

        if (game.isGameOver) {
            showResult = true
            return
        }
        if (mode == GameMode.BOT && game.turn == BOT_COLOR) {
            requestBotMove()
        }
    }

    /** Returns an updated piece list, preserving identity so Compose animates the moved
     * piece(s) sliding from their old square to the new one instead of popping in/out. */
    private fun applyMoveToPieces(current: List<PieceInstance>, move: Move): List<PieceInstance> {
        var updated = current

        if (move.enPassant) {
            val capR = if (move.piece.color == PieceColor.WHITE) move.to.r + 1 else move.to.r - 1
            val capSquare = Square(capR, move.to.c)
            updated = updated.filterNot { it.square == capSquare }
        } else if (move.capture) {
            updated = updated.filterNot { it.square == move.to }
        }

        updated = updated.map { p ->
            if (p.square == move.from) p.copy(square = move.to, type = move.promotion ?: p.type) else p
        }

        move.castle?.let { castle ->
            val rank = move.from.r
            val rookFromC = if (castle == "K") 7 else 0
            val rookToC = if (castle == "K") 5 else 3
            val rookFrom = Square(rank, rookFromC)
            val rookTo = Square(rank, rookToC)
            updated = updated.map { p -> if (p.square == rookFrom) p.copy(square = rookTo) else p }
        }

        return updated
    }

    private fun requestBotMove() {
        thinking = true
        val token = requestToken
        val snapshot = game.clone()
        val level = botLevel
        viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            val move = withContext(Dispatchers.Default) { ChessAI.pickMove(snapshot, level) }
            // So the bot always feels like it "thought" for a moment, even at Beginner/Easy
            // where the search itself finishes almost instantly.
            val elapsed = System.currentTimeMillis() - startedAt
            delay(maxOf(0, BOT_MIN_DELAY_MS - elapsed))
            if (token != requestToken) return@launch
            thinking = false
            if (move != null) {
                finalize(
                    Move(
                        from = move.from,
                        to = move.to,
                        piece = move.piece,
                        capture = game.pieceAt(move.to.r, move.to.c) != null,
                        promotion = move.promotion,
                    )
                )
            }
        }
    }

    fun undoLastTurn() {
        if (mode != GameMode.BOT || thinking || game.history.isEmpty()) return
        val removeCount = if (game.history.size % 2 == 0) 2 else 1
        redoStack.addLast(game.history.takeLast(removeCount))
        canRedo = true
        requestToken = UUID.randomUUID()
        game.undoPlies(removeCount)
        game = game
        pieces = PieceInstance.fresh(game.board)
        selected = null
        legalTargets = emptyList()
        lastMove = null
        showResult = false
        SoundEngine.playClick()
    }

    fun redoLastTurn() {
        if (mode != GameMode.BOT || thinking) return
        val batch = redoStack.removeLastOrNull() ?: return
        for (record in batch) {
            game.makeMove(record.from, record.to, record.promotion)
        }
        canRedo = redoStack.isNotEmpty()
        game = game
        pieces = PieceInstance.fresh(game.board)
        batch.lastOrNull()?.let { lastMove = it.from to it.to }
        SoundEngine.playClick()
    }

    fun flipBoard() { flipped = !flipped }
}
