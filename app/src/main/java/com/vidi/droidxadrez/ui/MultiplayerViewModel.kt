package com.vidi.droidxadrez.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vidi.droidxadrez.Loc
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

    private fun begin(gameVM: GameViewModel) {
        MultiplayerService.onRemoteMove = { from, to, promotion -> gameVM.applyRemoteMove(from, to, promotion) }
        MultiplayerService.onGameFinished = { result -> gameVM.multiplayerResult = result }
        MultiplayerService.onOpponentPresence = { online -> opponentOnline = online }
        MultiplayerService.onChat = { msg -> chatMessages = chatMessages + msg }
        gameVM.newGame(mode = GameMode.MULTIPLAYER, networkColor = null)
        gameVM.onLocalMove = { record -> MultiplayerService.sendMove(record.from, record.to, record.promotion) }
        chatMessages = emptyList()
        opponentOnline = false
    }

    fun createRoom(gameVM: GameViewModel, onReady: () -> Unit) {
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
                gameVM.networkColor = MultiplayerService.myColor
                waitingForOpponent = true
            } catch (e: Exception) {
                errorMessage = message(e)
            }
        }
    }

    fun joinRoom(code: String, gameVM: GameViewModel, onReady: () -> Unit) {
        begin(gameVM)
        errorMessage = null
        viewModelScope.launch {
            try {
                MultiplayerService.joinRoom(code)
                gameVM.networkColor = MultiplayerService.myColor
                onReady()
            } catch (e: Exception) {
                errorMessage = message(e)
            }
        }
    }

    fun quickPlay(gameVM: GameViewModel, onReady: () -> Unit) {
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
                gameVM.networkColor = MultiplayerService.myColor
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

    fun leave() {
        MultiplayerService.leaveRoom()
        waitingForOpponent = false
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
