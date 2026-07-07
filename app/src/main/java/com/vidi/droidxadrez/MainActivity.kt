package com.vidi.droidxadrez

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.engine.BotDifficulty
import com.vidi.droidxadrez.ui.GameMode
import com.vidi.droidxadrez.ui.GameScreen
import com.vidi.droidxadrez.ui.GameViewModel
import com.vidi.droidxadrez.ui.HelpScreen
import com.vidi.droidxadrez.ui.MainMenuScreen
import com.vidi.droidxadrez.ui.SplashScreen
import com.vidi.droidxadrez.ui.TutorialScreen

private enum class AppScreen { MENU, GAME, TUTORIAL, HELP }

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Loc.init(applicationContext)
        SoundEngine.init(applicationContext)

        setContent {
            XadrezTheme {
                var screen by remember { mutableStateOf(AppScreen.MENU) }
                var showSplash by remember { mutableStateOf(true) }

                Box(modifier = Modifier.fillMaxSize().background(Theme.bg)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AppHeader()
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (screen) {
                                AppScreen.MENU -> MainMenuScreen(
                                    onStart1v1 = { vm.newGame(GameMode.ONE_V_ONE); screen = AppScreen.GAME },
                                    onStartBot = { level -> vm.newGame(GameMode.BOT, level); screen = AppScreen.GAME },
                                    onOpenTutorial = { screen = AppScreen.TUTORIAL },
                                    onOpenHelp = { screen = AppScreen.HELP },
                                )
                                AppScreen.GAME -> GameScreen(vm = vm, onBackToMenu = { screen = AppScreen.MENU })
                                AppScreen.TUTORIAL -> TutorialScreen(onBackToMenu = { screen = AppScreen.MENU })
                                AppScreen.HELP -> HelpScreen(onBackToMenu = { screen = AppScreen.MENU })
                            }
                        }
                        AppFooter()
                    }

                    if (showSplash) {
                        SplashScreen(onDismiss = { showSplash = false })
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppHeader() {
    var soundOn by remember { mutableStateOf(SoundEngine.isOn) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.bg)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text("♚", fontSize = 22.sp, color = Theme.gold)
        Spacer(Modifier.width(8.dp))
        Text("Xadrez", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
        Spacer(Modifier.weight(1f))

        Text(
            text = if (soundOn) "🔊" else "🔇",
            modifier = Modifier
                .background(Theme.panel, CircleShape)
                .border(1.dp, Theme.panelBorder, CircleShape)
                .clickable {
                    soundOn = SoundEngine.toggleSound()
                    if (soundOn) SoundEngine.playClick()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp),
        )
        Spacer(Modifier.width(10.dp))

        var lang by remember { mutableStateOf(Loc.language) }
        Text(
            text = (if (lang == AppLanguage.PT) "🇵🇹 " else "🇬🇧 ") + lang.code.uppercase(),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Theme.ink,
            modifier = Modifier
                .background(Theme.panel, CircleShape)
                .border(1.dp, Theme.panelBorder, CircleShape)
                .clickable {
                    Loc.toggle()
                    lang = Loc.language
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@androidx.compose.runtime.Composable
private fun AppFooter() {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
    ) {
        Row {
            Text(Loc.t("footerBy") + " ", fontSize = 12.sp, color = Theme.inkDim)
            Text("David Arsénio Martins", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Theme.goldSoft)
        }
        Spacer(Modifier.height(2.dp))
        Row {
            Text(
                "ividi.dev",
                fontSize = 12.sp,
                color = Theme.inkDim,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ividi.dev/")))
                },
            )
            Text("  ·  ", fontSize = 12.sp, color = Theme.inkDim)
            Text(
                "GitHub",
                fontSize = 12.sp,
                color = Theme.inkDim,
                modifier = Modifier.clickable {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VidiPT89/DroidXadrez")))
                },
            )
        }
    }
}
