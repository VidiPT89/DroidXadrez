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
import com.vidi.droidxadrez.engine.PieceType
import com.vidi.droidxadrez.engine.Square
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class GameMode { ONE_V_ONE, BOT }

class GameViewModel : ViewModel() {
    companion object {
        val BOT_COLOR = PieceColor.BLACK
    }

    // neverEqualPolicy: ChessGame is mutated in place, so every reassignment must notify observers
    // even when the reference is unchanged.
    var game by mutableStateOf(ChessGame(), neverEqualPolicy())
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

    private var requestToken: UUID = UUID.randomUUID()

    fun newGame(mode: GameMode, level: BotDifficulty = BotDifficulty.MEDIUM) {
        this.mode = mode
        this.botLevel = level
        requestToken = UUID.randomUUID()
        game = ChessGame()
        selected = null
        legalTargets = emptyList()
        lastMove = null
        thinking = false
        promotionCandidates = null
        showResult = false
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
        game = game // re-signal: same reference, but neverEqualPolicy notifies observers

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

    private fun requestBotMove() {
        thinking = true
        val token = requestToken
        val snapshot = game.clone()
        val level = botLevel
        viewModelScope.launch {
            val move = withContext(Dispatchers.Default) { ChessAI.pickMove(snapshot, level) }
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

    fun flipBoard() { flipped = !flipped }
}
