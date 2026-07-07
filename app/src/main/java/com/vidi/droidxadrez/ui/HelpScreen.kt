package com.vidi.droidxadrez.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.AppLanguage
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.Theme

private data class HelpBlock(val titlePt: String, val titleEn: String, val bodyPt: String, val bodyEn: String) {
    fun title(lang: AppLanguage) = if (lang == AppLanguage.PT) titlePt else titleEn
    fun body(lang: AppLanguage) = if (lang == AppLanguage.PT) bodyPt else bodyEn
}

private val HELP_BLOCKS = listOf(
    HelpBlock(
        titlePt = "Objetivo", titleEn = "Objective",
        bodyPt = "Dar xeque-mate ao rei adversário — colocá-lo sob ataque sem qualquer forma de escapar.",
        bodyEn = "Checkmate the opponent's king — put it under attack with no way to escape.",
    ),
    HelpBlock(
        titlePt = "Como jogar", titleEn = "How to play",
        bodyPt = "Toca numa peça tua para a selecionar — os lances possíveis ficam marcados com um ponto (ou um anel, se for uma captura). Toca numa das casas marcadas para jogar. Toca noutra peça tua para trocar a seleção.",
        bodyEn = "Tap one of your pieces to select it — legal moves are marked with a dot (or a ring, for a capture). Tap a marked square to play the move. Tap another of your pieces to change the selection.",
    ),
    HelpBlock(
        titlePt = "Modos de jogo", titleEn = "Game modes",
        bodyPt = "• 1 vs 1 — dois jogadores alternam turnos no mesmo dispositivo.\n• Contra o Bot — escolhe entre 4 níveis de dificuldade (Iniciante a Difícil); jogas sempre com as Brancas e o bot joga com as Pretas.\n• Tutorial — lições passo-a-passo sobre movimentação, regras especiais, aberturas, táticas e finais.",
        bodyEn = "• 1 vs 1 — two players take turns on the same device.\n• Vs Bot — choose between 4 difficulty levels (Beginner to Hard); you always play White and the bot plays Black.\n• Tutorial — step-by-step lessons on piece movement, special rules, openings, tactics and endgames.",
    ),
    HelpBlock(
        titlePt = "Controlos", titleEn = "Controls",
        bodyPt = "Inverter — roda o tabuleiro 180°.\nNovo Jogo — reinicia a partida atual.\nMenu — volta ao menu principal.\n🔊 — liga/desliga o som.\n🇵🇹/🇬🇧 — muda o idioma entre Português e Inglês.",
        bodyEn = "Flip — rotates the board 180°.\nNew Game — restarts the current match.\nMenu — returns to the main menu.\n🔊 — toggles sound on/off.\n🇵🇹/🇬🇧 — switches the language between Portuguese and English.",
    ),
)

@Composable
fun HelpScreen(onBackToMenu: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(Loc.t("helpTitle"), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Theme.goldSoft)
        Spacer(Modifier.height(16.dp))

        for (block in HELP_BLOCKS) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme.panel, RoundedCornerShape(16.dp))
                    .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text(block.title(Loc.language), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Theme.goldSoft)
                Spacer(Modifier.height(6.dp))
                Text(block.body(Loc.language), fontSize = 14.sp, color = Theme.inkDim)
            }
            Spacer(Modifier.height(14.dp))
        }

        GhostButton(Loc.t("backToMenu")) { onBackToMenu() }
    }
}
