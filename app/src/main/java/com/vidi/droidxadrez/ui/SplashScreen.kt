package com.vidi.droidxadrez.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.Theme
import kotlinx.coroutines.delay
import android.content.Intent
import android.net.Uri

private const val INTRO_DURATION_MS = 3200

@Composable
fun SplashScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(INTRO_DURATION_MS, easing = LinearEasing))
    }
    LaunchedEffect(Unit) {
        delay(INTRO_DURATION_MS.toLong())
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f))
            .clickable(indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 380.dp)
                .padding(24.dp)
                .background(Theme.panel, RoundedCornerShape(20.dp))
                .border(1.dp, Theme.panelBorder, RoundedCornerShape(20.dp))
                .padding(32.dp),
        ) {
            Text("♚", fontSize = 44.sp, color = Theme.gold)
            Spacer(Modifier.height(10.dp))
            Text(Loc.t("introTitle"), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Theme.goldSoft)
            Spacer(Modifier.height(10.dp))
            Text(Loc.t("introText"), fontSize = 14.sp, color = Theme.inkDim, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            Row {
                Text(Loc.t("footerBy") + " ", fontSize = 14.sp, color = Theme.ink)
                Text("David Arsénio Martins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Theme.ink)
            }
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    "ividi.dev",
                    fontSize = 13.sp,
                    color = Theme.goldSoft,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ividi.dev/")))
                    },
                )
                Text("  ·  ", fontSize = 13.sp, color = Theme.inkDim)
                Text(
                    "GitHub",
                    fontSize = 13.sp,
                    color = Theme.goldSoft,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VidiPT89/DroidXadrez")))
                    },
                )
            }
            Spacer(Modifier.height(14.dp))

            GhostButton(Loc.t("introSkip")) { onDismiss() }
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Theme.panelBorder, RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .height(3.dp)
                        .background(Theme.gold, RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}
