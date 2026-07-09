package com.vidi.droidxadrez.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.Theme
import com.vidi.droidxadrez.multiplayer.MultiplayerService

private enum class LobbyStep { CHOICE, JOIN }

@Composable
fun MultiplayerLobbyScreen(
    mpVM: MultiplayerViewModel,
    gameVM: GameViewModel,
    onReady: () -> Unit,
    onBack: () -> Unit,
) {
    var step by remember { mutableStateOf(LobbyStep.CHOICE) }
    var joinCode by remember { mutableStateOf("") }
    var quickPlayWaiting by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 480.dp)
            .background(Theme.panel, RoundedCornerShape(16.dp))
            .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
            .padding(24.dp),
    ) {
        Text(Loc.t("mpTitle"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
        Spacer(Modifier.height(18.dp))

        when {
            mpVM.waitingForOpponent -> {
                Text(Loc.t("mpWaitingTitle"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
                Spacer(Modifier.height(10.dp))
                // Only a deliberately-created room is worth showing/sharing a code for — Quick
                // Play's whole point is that no code is needed, so keep that wait screen plain.
                if (!quickPlayWaiting) {
                    MultiplayerService.roomCode?.let { code ->
                        Text(code, fontSize = 34.sp, fontFamily = FontFamily.Monospace, letterSpacing = 6.sp, color = Theme.goldSoft)
                        Spacer(Modifier.height(14.dp))
                        GhostButton(Loc.t("mpShareLink")) {
                            val url = "https://vidipt89.github.io/Xadrez/?room=$code"
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${Loc.t("mpShareText")} $url")
                            }
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                GhostButton(Loc.t("cancelBtn")) { mpVM.leave(); step = LobbyStep.CHOICE }
            }
            step == LobbyStep.JOIN -> {
                Text(Loc.t("mpEnterCode"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
                Spacer(Modifier.height(10.dp))
                TextField(
                    value = joinCode,
                    onValueChange = { joinCode = it.uppercase().take(6) },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace, fontSize = 20.sp, textAlign = TextAlign.Center,
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Theme.bgSoft,
                        unfocusedContainerColor = Theme.bgSoft,
                        focusedTextColor = Theme.ink,
                        unfocusedTextColor = Theme.ink,
                    ),
                    placeholder = { Text("ABC123", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    modifier = Modifier.widthIn(max = 220.dp),
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    GhostButton(Loc.t("mpJoin"), enabled = joinCode.length == 6, modifier = Modifier.weight(1f)) {
                        mpVM.joinRoom(joinCode, gameVM, onReady)
                    }
                    GhostButton(Loc.t("cancelBtn"), modifier = Modifier.weight(1f)) { step = LobbyStep.CHOICE }
                }
            }
            else -> {
                if (!MultiplayerService.configured) {
                    Text(
                        Loc.t("mpNotConfigured"),
                        fontSize = 13.sp,
                        color = Theme.danger,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                GhostButton(Loc.t("mpQuickPlay"), enabled = MultiplayerService.configured, modifier = Modifier.fillMaxWidth()) {
                    quickPlayWaiting = true
                    mpVM.quickPlay(gameVM, onReady)
                }
                Text(
                    Loc.t("mpQuickPlayDesc"),
                    fontSize = 12.sp,
                    color = Theme.inkDim,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    GhostButton(Loc.t("mpCreateRoom"), enabled = MultiplayerService.configured, modifier = Modifier.weight(1f)) {
                        quickPlayWaiting = false
                        mpVM.createRoom(gameVM, onReady)
                    }
                    GhostButton(Loc.t("mpJoinRoom"), enabled = MultiplayerService.configured, modifier = Modifier.weight(1f)) {
                        joinCode = ""
                        step = LobbyStep.JOIN
                    }
                }
                Spacer(Modifier.height(10.dp))
                GhostButton(Loc.t("cancelBtn")) { onBack() }
            }
        }

        mpVM.errorMessage?.let { error ->
            Spacer(Modifier.height(10.dp))
            Text(error, fontSize = 13.sp, color = Theme.danger, textAlign = TextAlign.Center)
        }
    }
    }
}
