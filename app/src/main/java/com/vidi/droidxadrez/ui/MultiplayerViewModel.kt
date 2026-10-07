package com.vidi.droidxadrez.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.engine.ChessGame
import com.vidi.droidxadrez.engine.GameResult
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.multiplayer.ChatMessage
import com.vidi.droidxadrez.multiplayer.MultiplayerError
import com.vidi.droidxadrez.multiplayer.MultiplayerService
import kotlinx.coroutines.launch

/**
 * Thin wrapper that bridges MultiplayerService (Firestore) and GameViewModel (local board
 * state), so GameViewModel itself never has to know about Firebase types.
 */
class MultiplayerViewModel : ViewModel() {
    val service get() = MultiplayerService

    var errorMessage by mutableStateOf<String?>(null)
        private set
    var waitingForOpponent by mutableStateOf(false)
        private set
    var opponentOnline by mutableStateOf(false)
        private set
    var chatMessages by mutableStateOf<List<ChatMessage>>(emptyList())
        private set
    var opponentName by mutableStateOf("")
        private set
    /** What the player typed in the lobby (may be blank — the default name is used then). */
    var playerName by mutableStateOf("")

    fun loadPlayerName(context: Context) {
        playerName = prefs(context).getString(KEY_NAME, "") ?: ""
    }

    /** Called before any room action: remembers the typed name and hands it to the service. */
    private fun commitPlayerName(context: Context) {
        playerName = MultiplayerService.cleanName(playerName)
        prefs(context).edit().putString(KEY_NAME, playerName).apply()
        MultiplayerService.myName = playerName.ifEmpty { Loc.t("mpDefaultName") }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("xadrez_prefs", Context.MODE_PRIVATE)

    private fun begin(gameVM: GameViewModel) {
        MultiplayerService.onRemoteMove = { from, to, promotion -> gameVM.applyRemoteMove(from, to, promotion) }
        MultiplayerService.onGameFinished = { result ->
            // Checkmate/draw endings are detected by the local engine, which already shows them.
            if (result.startsWith("resign-")) gameVM.multiplayerResult = result
        }
        MultiplayerService.onOpponentPresence = { online -> opponentOnline = online }
        MultiplayerService.onOpponentName = { name -> opponentName = name }
        MultiplayerService.onChat = { msg -> chatMessages = chatMessages + msg }
        gameVM.newGame(mode = GameMode.MULTIPLAYER, networkColor = null)
        gameVM.onLocalMove = { record ->
            MultiplayerService.sendMove(record.from, record.to, record.promotion)
            // Whoever played the final move records the ending, which also frees a Quick Play slot.
            endingCode(gameVM.game)?.let { MultiplayerService.finishGame(it) }
        }
        chatMessages = emptyList()
        opponentOnline = false
        opponentName = ""
    }

    fun createRoom(context: Context, gameVM: GameViewModel, onReady: () -> Unit) {
        commitPlayerName(context)
        begin(gameVM)
        errorMessage = null
        waitingForOpponent = false
        MultiplayerService.onOpponentJoined = {
            waitingForOpponent = false
            onReady()
        }
        viewModelScope.launch {
            try {
                MultiplayerService.createRoom()
                seat(gameVM)
                waitingForOpponent = true
            } catch (e: Exception) {
                errorMessage = message(e)
            }
        }
    }

    fun joinRoom(context: Context, code: String, gameVM: GameViewModel, onReady: () -> Unit) {
        commitPlayerName(context)
        begin(gameVM)
        errorMessage = null
        viewModelScope.launch {
            try {
                MultiplayerService.joinRoom(code)
                seat(gameVM)
                onReady()
            } catch (e: Exception) {
                errorMessage = message(e)
            }
        }
    }

    fun quickPlay(context: Context, gameVM: GameViewModel, onReady: () -> Unit) {
        commitPlayerName(context)
        begin(gameVM)
        errorMessage = null
        waitingForOpponent = false
        MultiplayerService.onOpponentJoined = {
            waitingForOpponent = false
            onReady()
        }
        viewModelScope.launch {
            try {
                val result = MultiplayerService.quickPlay()
                seat(gameVM)
                if (result.isHost) {
                    waitingForOpponent = true
                } else {
                    onReady()
                }
            } catch (e: Exception) {
                errorMessage = message(e)
            }
        }
    }

    /** "checkmate-w", "stalemate", "draw-50"… — the same codes the web version writes. */
    private fun endingCode(game: ChessGame): String? = when (game.result) {
        GameResult.CHECKMATE -> "checkmate-" + (game.winner?.code ?: "")
        GameResult.STALEMATE -> "stalemate"
        GameResult.DRAW_50 -> "draw-50"
        GameResult.DRAW_REPETITION -> "draw-repetition"
        GameResult.DRAW_MATERIAL -> "draw-material"
        null -> null
    }

    /** Records my color and puts my pieces at the bottom of the board — the guest plays Black. */
    private fun seat(gameVM: GameViewModel) {
        gameVM.networkColor = MultiplayerService.myColor
        gameVM.flipped = MultiplayerService.myColor == PieceColor.BLACK
    }

    fun leave() {
        MultiplayerService.leaveRoom()
        waitingForOpponent = false
    }

    /** Label for a player tag: names in multiplayer (mine marked "(tu)"), colors otherwise. */
    fun playerLabel(color: PieceColor, myColor: PieceColor?): String = when {
        myColor == null -> Loc.t(if (color == PieceColor.WHITE) "whitePlayer" else "blackPlayer")
        color == myColor -> "${MultiplayerService.myName.ifEmpty { Loc.t("mpDefaultName") }} ${Loc.t("mpYouSuffix")}"
        else -> opponentName.ifEmpty { Loc.t("mpOpponent") }
    }

    private companion object {
        const val KEY_NAME = "player_name"
    }

    private fun message(e: Exception): String = when (e) {
        is MultiplayerError.RoomNotFound -> Loc.t("mpErrorNotFound")
        is MultiplayerError.RoomFull -> Loc.t("mpErrorFull")
        is MultiplayerError.RoomFinished -> Loc.t("mpErrorFinished")
        is MultiplayerError.LobbyFull -> Loc.t("mpErrorLobbyFull")
        is MultiplayerError.NotConfigured -> Loc.t("mpNotConfigured")
        else -> Loc.t("mpErrorGeneric")
    }
}
