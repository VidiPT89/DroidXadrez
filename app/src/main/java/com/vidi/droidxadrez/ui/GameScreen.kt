package com.vidi.droidxadrez.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.R
import com.vidi.droidxadrez.Theme
import com.vidi.droidxadrez.engine.GameResult
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.engine.PieceType

@Composable
fun GameScreen(vm: GameViewModel, onBackToMenu: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlayerTag(color = PieceColor.BLACK, vm = vm)
        Spacer(Modifier.height(10.dp))

        ChessBoardView(
            pieces = vm.pieces,
            selected = vm.selected,
            legalTargets = vm.legalTargets,
            lastMove = vm.lastMove,
            checkSquare = vm.kingInCheckSquare(),
            flipped = vm.flipped,
            onTap = { vm.tapSquare(it) },
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        )

        Spacer(Modifier.height(10.dp))
        PlayerTag(color = PieceColor.WHITE, vm = vm)
        Spacer(Modifier.height(18.dp))

        StatusCard(vm)
        Spacer(Modifier.height(14.dp))
        HistoryCard(vm)
        Spacer(Modifier.height(14.dp))

        if (vm.mode == GameMode.BOT) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(Loc.t("undoMove"), enabled = !vm.thinking && vm.game.history.isNotEmpty()) { vm.undoLastTurn() }
                GhostButton(Loc.t("redoMove"), enabled = !vm.thinking && vm.canRedo) { vm.redoLastTurn() }
            }
            Spacer(Modifier.height(10.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(Loc.t("flipBoard")) { vm.flipBoard() }
            GhostButton(Loc.t("newGame")) { vm.newGame(vm.mode, vm.botLevel) }
            GhostButton(Loc.t("backToMenu")) { onBackToMenu() }
        }
    }

    val promo = vm.promotionCandidates
    if (promo != null) {
        PromotionDialog(vm)
    }
    if (vm.showResult) {
        ResultDialog(vm, onRematch = { vm.showResult = false; vm.newGame(vm.mode, vm.botLevel) }, onMenu = { vm.showResult = false; onBackToMenu() })
    }
}

@Composable
private fun PlayerTag(color: PieceColor, vm: GameViewModel) {
    val active = vm.game.turn == color && !vm.statusText().over
    val label = if (color == PieceColor.WHITE) Loc.t("whitePlayer") else Loc.t("blackPlayer")
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (active) Theme.goldSoft else Theme.ink,
        modifier = Modifier
            .background(Theme.panel, CircleShape)
            .border(1.dp, if (active) Theme.gold else Theme.panelBorder, CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun StatusCard(vm: GameViewModel) {
    val status = vm.statusText()
    val note = when {
        vm.thinking -> Loc.t("thinking")
        status.key == "check" -> Loc.t("inCheck")
        else -> ""
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.panel, RoundedCornerShape(16.dp))
            .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (vm.game.turn == PieceColor.WHITE) Loc.t("turnWhite") else Loc.t("turnBlack"),
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Theme.ink,
        )
        if (note.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(text = note, fontSize = 14.sp, color = Theme.goldSoft)
        }
    }
}

@Composable
private fun HistoryCard(vm: GameViewModel) {
    val history = vm.game.history
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Theme.panel, RoundedCornerShape(16.dp))
            .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = Loc.t("moveHistory"),
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Theme.inkDim,
        )
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            val pairsCount = (history.size + 1) / 2
            for (i in 0 until pairsCount) {
                val white = history[i * 2]
                val black = history.getOrNull(i * 2 + 1)
                Text(
                    text = "${i + 1}. ${white.san}${if (black != null) "   ${black.san}" else ""}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Theme.ink,
                )
            }
        }
    }
}

@Composable
private fun PromotionDialog(vm: GameViewModel) {
    val color = vm.game.turn
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(Loc.t("promoTitle"), color = Theme.ink) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (type in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    GradientPieceIcon(
                        drawableId = pieceDrawable[type] ?: R.drawable.piece_q,
                        brush = if (color == PieceColor.WHITE) Theme.whitePieceBrush else Theme.blackPieceBrush,
                        modifier = Modifier
                            .size(58.dp)
                            .background(Theme.bgSoft, RoundedCornerShape(12.dp))
                            .border(1.dp, Theme.panelBorder, RoundedCornerShape(12.dp))
                            .clickable { vm.choosePromotion(type) }
                            .padding(10.dp),
                    )
                }
            }
        },
        containerColor = Theme.panel,
    )
}

@Composable
private fun ResultDialog(vm: GameViewModel, onRematch: () -> Unit, onMenu: () -> Unit) {
    val status = vm.statusText()
    var icon = "🤝"
    var title: String
    var text: String
    when {
        status.key == "checkmate" -> {
            icon = "🏆"
            title = Loc.t("resultCheckmateTitle")
            text = if (status.winner == PieceColor.WHITE) Loc.t("resultCheckmateWhite") else Loc.t("resultCheckmateBlack")
        }
        status.key == "stalemate" -> { title = Loc.t("resultStalemateTitle"); text = Loc.t("resultStalemateText") }
        vm.game.result == GameResult.DRAW_50 -> { title = Loc.t("resultDraw50Title"); text = Loc.t("resultDraw50Text") }
        vm.game.result == GameResult.DRAW_REPETITION -> { title = Loc.t("resultDrawRepTitle"); text = Loc.t("resultDrawRepText") }
        else -> { title = Loc.t("resultDrawMatTitle"); text = Loc.t("resultDrawMatText") }
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text("$icon  $title", color = Theme.goldSoft, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(text, color = Theme.inkDim)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GhostButton(Loc.t("newGame")) { onRematch() }
                    GhostButton(Loc.t("backToMenu")) { onMenu() }
                }
            }
        },
        containerColor = Theme.panel,
    )
}

@Composable
fun GhostButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (enabled) Theme.ink else Theme.inkDim,
        modifier = Modifier
            .border(1.dp, Theme.panelBorder, CircleShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
