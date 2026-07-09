package com.vidi.droidxadrez.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vidi.droidxadrez.R
import com.vidi.droidxadrez.Theme
import com.vidi.droidxadrez.engine.Board
import com.vidi.droidxadrez.engine.Move
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.engine.PieceType
import com.vidi.droidxadrez.engine.Square
import java.util.UUID

val pieceDrawable: Map<PieceType, Int> = mapOf(
    PieceType.KING to R.drawable.piece_k, PieceType.QUEEN to R.drawable.piece_q,
    PieceType.ROOK to R.drawable.piece_r, PieceType.BISHOP to R.drawable.piece_b,
    PieceType.KNIGHT to R.drawable.piece_n, PieceType.PAWN to R.drawable.piece_p,
)

/** A single on-board piece with a stable identity, so Compose can animate it sliding
 * from its old square to its new one instead of popping in and out. */
data class PieceInstance(val id: String, val type: PieceType, val color: PieceColor, val square: Square) {
    companion object {
        fun fresh(board: Board): List<PieceInstance> {
            val result = mutableListOf<PieceInstance>()
            for (r in 0..7) for (c in 0..7) {
                board[r][c]?.let { p -> result.add(PieceInstance(UUID.randomUUID().toString(), p.type, p.color, Square(r, c))) }
            }
            return result
        }
    }
}

/** Reusable 8x8 board renderer, shared by the main game screen and the tutorial lessons. */
@Composable
fun ChessBoardView(
    pieces: List<PieceInstance>,
    modifier: Modifier = Modifier,
    selected: Square? = null,
    legalTargets: List<Move> = emptyList(),
    lastMove: Pair<Square, Square>? = null,
    checkSquare: Square? = null,
    flipped: Boolean = false,
    onTap: (Square) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .border(3.dp, Theme.gold, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
    ) {
        val cellSize = maxWidth / 8

        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    for (displayCol in 0..7) {
                        val r = if (flipped) 7 - displayRow else displayRow
                        val c = if (flipped) 7 - displayCol else displayCol
                        SquareBackground(
                            r = r, c = c,
                            selected = selected, legalTargets = legalTargets,
                            lastMove = lastMove, checkSquare = checkSquare,
                            onTap = onTap,
                            modifier = Modifier.weight(1f).fillMaxSize(),
                        )
                    }
                }
            }
        }

        pieces.forEach { piece ->
            key(piece.id) {
                AnimatedPieceView(piece = piece, cellSize = cellSize, flipped = flipped)
            }
        }
    }
}

@Composable
private fun AnimatedPieceView(piece: PieceInstance, cellSize: Dp, flipped: Boolean) {
    val displayR = if (flipped) 7 - piece.square.r else piece.square.r
    val displayC = if (flipped) 7 - piece.square.c else piece.square.c
    val offsetX by animateDpAsState(targetValue = cellSize * displayC, animationSpec = tween(380), label = "pieceX")
    val offsetY by animateDpAsState(targetValue = cellSize * displayR, animationSpec = tween(380), label = "pieceY")

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(cellSize),
        contentAlignment = Alignment.Center,
    ) {
        GradientPieceIcon(
            drawableId = pieceDrawable[piece.type] ?: R.drawable.piece_p,
            brush = if (piece.color == PieceColor.WHITE) Theme.whitePieceBrush else Theme.blackPieceBrush,
            outlineColor = if (piece.color == PieceColor.WHITE) Theme.whitePieceOutline else Theme.blackPieceOutline,
            modifier = Modifier.fillMaxSize().padding(6.dp),
        )
    }
}

@Composable
private fun SquareBackground(
    r: Int,
    c: Int,
    selected: Square?,
    legalTargets: List<Move>,
    lastMove: Pair<Square, Square>?,
    checkSquare: Square?,
    onTap: (Square) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sq = Square(r, c)
    val isLight = (r + c) % 2 == 0
    val isSelected = selected == sq
    val isLast = lastMove?.let { it.first == sq || it.second == sq } ?: false
    val isCheck = checkSquare == sq
    val target = legalTargets.firstOrNull { it.to == sq }

    val bgBrush = if (isLight) Theme.lightSquareBrush else Theme.darkSquareBrush
    val borderColor = when {
        isSelected -> Theme.gold
        isCheck -> Theme.danger
        isLast -> Theme.gold.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .background(bgBrush)
            .then(if (borderColor != Color.Transparent) Modifier.border(3.dp, borderColor) else Modifier)
            .clickable { onTap(sq) },
        contentAlignment = Alignment.Center,
    ) {
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

/** Renders a template (single-color) vector drawable filled with a gradient brush.
 * An optional [outlineColor] draws a slightly oversized silhouette behind the fill, since a
 * cream/gold piece otherwise loses all definition against the light squares' near-identical tone. */
@Composable
fun GradientPieceIcon(drawableId: Int, brush: Brush, modifier: Modifier = Modifier, outlineColor: Color? = null) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (outlineColor != null) {
            PieceLayer(drawableId, SolidColor(outlineColor), Modifier.fillMaxSize().scale(1.09f))
        }
        PieceLayer(drawableId, brush, Modifier.fillMaxSize())
    }
}

@Composable
private fun PieceLayer(drawableId: Int, brush: Brush, modifier: Modifier) {
    val painter = painterResource(id = drawableId)
    Box(
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithCache {
                onDrawWithContent {
                    with(painter) { draw(size) }
                    drawRect(brush, blendMode = BlendMode.SrcIn)
                }
            }
    )
}
