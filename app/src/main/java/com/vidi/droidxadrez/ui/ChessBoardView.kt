package com.vidi.droidxadrez.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.Theme
import com.vidi.droidxadrez.engine.Board
import com.vidi.droidxadrez.engine.Move
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.engine.PieceType
import com.vidi.droidxadrez.engine.Square

private val PIECE_GLYPH: Map<PieceType, String> = mapOf(
    PieceType.KING to "♚", PieceType.QUEEN to "♛", PieceType.ROOK to "♜",
    PieceType.BISHOP to "♝", PieceType.KNIGHT to "♞", PieceType.PAWN to "♟",
)

/** Reusable 8x8 board renderer, shared by the main game screen and the tutorial lessons. */
@Composable
fun ChessBoardView(
    board: Board,
    modifier: Modifier = Modifier,
    selected: Square? = null,
    legalTargets: List<Move> = emptyList(),
    lastMove: Pair<Square, Square>? = null,
    checkSquare: Square? = null,
    flipped: Boolean = false,
    onTap: (Square) -> Unit = {},
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(3.dp, Theme.gold, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    for (displayCol in 0..7) {
                        val r = if (flipped) 7 - displayRow else displayRow
                        val c = if (flipped) 7 - displayCol else displayCol
                        SquareView(
                            r = r, c = c, board = board,
                            selected = selected, legalTargets = legalTargets,
                            lastMove = lastMove, checkSquare = checkSquare,
                            onTap = onTap,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareView(
    r: Int,
    c: Int,
    board: Board,
    selected: Square?,
    legalTargets: List<Move>,
    lastMove: Pair<Square, Square>?,
    checkSquare: Square?,
    onTap: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sq = Square(r, c)
    val isLight = (r + c) % 2 == 0
    val piece = board[r][c]
    val isSelected = selected == sq
    val isLast = lastMove?.let { it.first == sq || it.second == sq } ?: false
    val isCheck = checkSquare == sq
    val target = legalTargets.firstOrNull { it.to == sq }

    val bg = if (isLight) Theme.squareLight else Theme.squareDark
    val borderColor = when {
        isSelected -> Theme.gold
        isCheck -> Theme.danger
        isLast -> Theme.gold.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .background(bg)
            .then(if (borderColor != Color.Transparent) Modifier.border(3.dp, borderColor) else Modifier)
            .clickable { onTap(sq) },
        contentAlignment = Alignment.Center,
    ) {
        if (piece != null) {
            Text(
                text = PIECE_GLYPH[piece.type] ?: "",
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = if (piece.color == PieceColor.WHITE) Color(0xFFFAF6E9) else Color(0xFF141008),
            )
        }
        if (target != null) {
            if (target.capture) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .fillMaxSize()
                        .border(3.dp, Theme.danger, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Theme.gold.copy(alpha = 0.55f), CircleShape)
                )
            }
        }
    }
}
