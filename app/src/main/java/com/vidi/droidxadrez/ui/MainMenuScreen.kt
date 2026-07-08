package com.vidi.droidxadrez.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.Theme
import com.vidi.droidxadrez.engine.BotDifficulty
import kotlinx.coroutines.delay

private data class ModeCardData(val icon: String, val titleKey: String, val descKey: String, val action: () -> Unit)

@Composable
fun MainMenuScreen(
    onStart1v1: () -> Unit,
    onStartBot: (BotDifficulty) -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenMultiplayer: () -> Unit,
) {
    var showDifficulty by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(showDifficulty) {
        if (showDifficulty) {
            delay(50) // let the panel's height enter layout before we scroll to it
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text(Loc.t("menuTitle"), fontSize = 38.sp, fontWeight = FontWeight.Bold, color = Theme.goldSoft)
        Spacer(Modifier.height(6.dp))
        Text(Loc.t("menuSubtitle"), fontSize = 16.sp, color = Theme.inkDim)
        Spacer(Modifier.height(24.dp))

        val cards = listOf(
            ModeCardData("🧑‍🤝‍🧑", "mode1v1", "mode1v1Desc") { showDifficulty = false; onStart1v1() },
            ModeCardData("🤖", "modeBot", "modeBotDesc") { showDifficulty = true },
            ModeCardData("🎓", "modeTutorial", "modeTutorialDesc") { onOpenTutorial() },
            ModeCardData("❓", "modeHelp", "modeHelpDesc") { onOpenHelp() },
            ModeCardData("🌐", "modeMultiplayer", "modeMultiplayerDesc") { showDifficulty = false; onOpenMultiplayer() },
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(max = 480.dp).widthIn(max = 480.dp),
        ) {
            items(cards.size) { i ->
                val card = cards[i]
                ModeCard(card.icon, Loc.t(card.titleKey), Loc.t(card.descKey), card.action)
            }
        }

        if (showDifficulty) {
            Spacer(Modifier.height(20.dp))
            DifficultyPanel(onCancel = { showDifficulty = false }, onPick = onStartBot)
        }
    }
}

@Composable
private fun ModeCard(icon: String, title: String, desc: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Theme.panel, RoundedCornerShape(16.dp))
            .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Text(icon, fontSize = 32.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Theme.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(desc, fontSize = 12.sp, color = Theme.inkDim, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DifficultyPanel(onCancel: () -> Unit, onPick: (BotDifficulty) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(max = 480.dp)
            .background(Theme.panel, RoundedCornerShape(16.dp))
            .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Text(Loc.t("chooseDifficulty"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
        Spacer(Modifier.height(14.dp))

        val levels = listOf(
            Triple(BotDifficulty.BEGINNER, "★☆☆☆", "levelBeginner"),
            Triple(BotDifficulty.EASY, "★★☆☆", "levelEasy"),
            Triple(BotDifficulty.MEDIUM, "★★★☆", "levelMedium"),
            Triple(BotDifficulty.HARD, "★★★★", "levelHard"),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 140.dp).fillMaxWidth(),
        ) {
            items(levels.size) { i ->
                val (level, stars, labelKey) = levels[i]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Theme.bgSoft, RoundedCornerShape(12.dp))
                        .border(1.dp, Theme.panelBorder, RoundedCornerShape(12.dp))
                        .clickable { onPick(level) }
                        .padding(vertical = 14.dp),
                ) {
                    Text(stars, color = Theme.goldSoft, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(Loc.t(labelKey), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Theme.ink)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GhostButton(Loc.t("cancelBtn")) { onCancel() }
    }
}
