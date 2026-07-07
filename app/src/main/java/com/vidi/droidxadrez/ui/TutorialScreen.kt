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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidi.droidxadrez.AppLanguage
import com.vidi.droidxadrez.Loc
import com.vidi.droidxadrez.Theme
import com.vidi.droidxadrez.engine.ChessGame
import com.vidi.droidxadrez.engine.CastlingRights
import com.vidi.droidxadrez.engine.Move
import com.vidi.droidxadrez.engine.Piece
import com.vidi.droidxadrez.engine.PieceColor
import com.vidi.droidxadrez.engine.PieceType
import com.vidi.droidxadrez.engine.Square

private data class Lesson(
    val titlePt: String, val titleEn: String,
    val textPt: String, val textEn: String,
    val setup: () -> ChessGame,
) {
    fun title(lang: AppLanguage) = if (lang == AppLanguage.PT) titlePt else titleEn
    fun text(lang: AppLanguage) = if (lang == AppLanguage.PT) textPt else textEn
}

private fun customGame(pieces: List<Triple<String, PieceType, PieceColor>>, turn: PieceColor = PieceColor.WHITE): ChessGame {
    val board = Array(8) { arrayOfNulls<Piece?>(8) }
    for ((square, type, color) in pieces) {
        val file = "abcdefgh".indexOf(square[0])
        val rank = 8 - (square[1] - '0')
        board[rank][file] = Piece(type, color)
    }
    val g = ChessGame()
    g.board = board
    g.turn = turn
    g.castling = CastlingRights(wK = false, wQ = false, bK = false, bQ = false)
    g.enPassant = null
    g.history = mutableListOf()
    return g
}

private val LESSONS: List<Lesson> = listOf(
    Lesson(
        titlePt = "1. Como as peças se movem", titleEn = "1. How the pieces move",
        textPt = "Cada peça move-se de forma diferente. O peão avança uma casa (duas no primeiro lance) e captura na diagonal. O cavalo salta em 'L'. O bispo move-se na diagonal. A torre move-se em linha reta. A dama combina torre e bispo. O rei move-se uma casa em qualquer direção.\n\nToca numa peça do tabuleiro para veres exatamente para onde ela pode ir.",
        textEn = "Every piece moves differently. The pawn advances one square (two on its first move) and captures diagonally. The knight jumps in an 'L' shape. The bishop moves diagonally. The rook moves in straight lines. The queen combines rook and bishop. The king moves one square in any direction.\n\nTap a piece on the board to see exactly where it can go.",
        setup = { customGame(listOf(Triple("d4", PieceType.KNIGHT, PieceColor.WHITE), Triple("b6", PieceType.BISHOP, PieceColor.WHITE), Triple("g2", PieceType.ROOK, PieceColor.WHITE), Triple("a2", PieceType.PAWN, PieceColor.WHITE), Triple("f6", PieceType.QUEEN, PieceColor.BLACK), Triple("e8", PieceType.KING, PieceColor.BLACK))) },
    ),
    Lesson(
        titlePt = "2. Regras especiais", titleEn = "2. Special rules",
        textPt = "O roque move o rei duas casas em direção à torre (e a torre salta para o outro lado do rei), desde que nenhum dos dois se tenha mexido e as casas entre eles estejam livres e fora de ataque.\n\nO en passant permite a um peão capturar um peão adversário que acabou de avançar duas casas, como se tivesse avançado só uma.\n\nUm peão que chega à última fileira é promovido — normalmente a dama.\n\nXeque é quando o rei está sob ataque; xeque-mate é quando não há forma de escapar; afogamento é quando o jogador não tem lances legais mas não está em xeque — resulta em empate.",
        textEn = "Castling moves the king two squares toward a rook (and the rook jumps to the other side of the king), as long as neither has moved and the squares between them are empty and not under attack.\n\nEn passant lets a pawn capture an enemy pawn that just advanced two squares, as if it had only moved one.\n\nA pawn reaching the last rank is promoted — usually to a queen.\n\nCheck is when the king is under attack; checkmate is when there is no way to escape; stalemate is when a player has no legal move but isn't in check — the game is a draw.",
        setup = { customGame(listOf(Triple("e1", PieceType.KING, PieceColor.WHITE), Triple("h1", PieceType.ROOK, PieceColor.WHITE), Triple("a1", PieceType.ROOK, PieceColor.WHITE), Triple("e8", PieceType.KING, PieceColor.BLACK))) },
    ),
    Lesson(
        titlePt = "3. Princípios de abertura", titleEn = "3. Opening principles",
        textPt = "Controla o centro (casas d4, d5, e4, e5) com peões e peças. Desenvolve cavalos e bispos cedo, antes da dama. Roca cedo para pores o rei em segurança. Evita mover a mesma peça várias vezes na abertura e não saias com a dama demasiado cedo — ela pode ser atacada e perder tempo.",
        textEn = "Control the center (the d4, d5, e4, e5 squares) with pawns and pieces. Develop knights and bishops early, before the queen. Castle early to keep your king safe. Avoid moving the same piece multiple times in the opening, and don't bring your queen out too soon — it can be attacked and lose you tempo.",
        setup = { customGame(listOf(Triple("e4", PieceType.PAWN, PieceColor.WHITE), Triple("c3", PieceType.KNIGHT, PieceColor.WHITE), Triple("f3", PieceType.KNIGHT, PieceColor.WHITE), Triple("e1", PieceType.KING, PieceColor.WHITE), Triple("e5", PieceType.PAWN, PieceColor.BLACK), Triple("c6", PieceType.KNIGHT, PieceColor.BLACK), Triple("f6", PieceType.KNIGHT, PieceColor.BLACK), Triple("e8", PieceType.KING, PieceColor.BLACK))) },
    ),
    Lesson(
        titlePt = "4. Táticas básicas", titleEn = "4. Basic tactics",
        textPt = "Garfo: uma peça ataca duas peças adversárias ao mesmo tempo (o cavalo é excelente nisto). Cravo: uma peça não se pode mover porque exporia uma peça mais valiosa atrás dela. Espeto: como o cravo, mas a peça mais valiosa está à frente e é forçada a mover-se, expondo a de trás. Ataque descoberto: mover uma peça revela o ataque de outra peça escondida atrás.\n\nToca no cavalo para veres um exemplo de garfo neste tabuleiro.",
        textEn = "Fork: one piece attacks two enemy pieces at once (the knight is excellent at this). Pin: a piece can't move because it would expose a more valuable piece behind it. Skewer: like a pin, but the more valuable piece is in front and forced to move, exposing the one behind it. Discovered attack: moving one piece reveals an attack from another piece hidden behind it.\n\nTap the knight to see a fork example on this board.",
        setup = { customGame(listOf(Triple("e5", PieceType.KNIGHT, PieceColor.WHITE), Triple("d7", PieceType.KING, PieceColor.BLACK), Triple("f7", PieceType.ROOK, PieceColor.BLACK))) },
    ),
    Lesson(
        titlePt = "5. Finais básicos", titleEn = "5. Basic endgames",
        textPt = "Com rei e dama contra rei sozinho, encurrala o rei adversário para a margem do tabuleiro usando a dama a uma 'distância de cavalo', trazendo o teu rei para ajudar a dar o mate.\n\nOposição: em finais de rei e peão, ter o teu rei diretamente à frente do rei adversário (com uma casa de intervalo) força-o a recuar.\n\nUm peão passado (sem peões adversários a travá-lo nas colunas vizinhas) é um trunfo enorme — protege-o e empurra-o para promoção.",
        textEn = "With king and queen versus a lone king, herd the enemy king to the edge of the board using the queen at a 'knight's distance', and bring your own king up to help deliver mate.\n\nOpposition: in king-and-pawn endgames, having your king directly facing the enemy king (with one square between them) forces it to give way.\n\nA passed pawn (with no enemy pawns able to stop it on neighboring files) is a huge asset — protect it and push it toward promotion.",
        setup = { customGame(listOf(Triple("e1", PieceType.KING, PieceColor.WHITE), Triple("d5", PieceType.QUEEN, PieceColor.WHITE), Triple("e8", PieceType.KING, PieceColor.BLACK))) },
    ),
)

@Composable
fun TutorialScreen(onBackToMenu: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var game by remember { mutableStateOf(LESSONS[0].setup(), neverEqualPolicy()) }
    var selected by remember { mutableStateOf<Square?>(null) }
    var legalTargets by remember { mutableStateOf<List<Move>>(emptyList()) }

    fun select(newIndex: Int) {
        if (newIndex !in LESSONS.indices) return
        index = newIndex
        game = LESSONS[index].setup()
        selected = null
        legalTargets = emptyList()
    }

    fun tap(square: Square) {
        val sel = selected
        if (sel != null && legalTargets.any { it.to == square }) {
            game.makeMove(sel, square)
            game = game
            selected = null
            legalTargets = emptyList()
            return
        }
        val piece = game.pieceAt(square.r, square.c)
        if (piece != null && piece.color == game.turn) {
            selected = square
            legalTargets = game.legalMoves(square)
        } else {
            selected = null
            legalTargets = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LESSONS.forEachIndexed { i, _ ->
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (i == index) Theme.gold else Theme.panel, CircleShape)
                        .border(1.dp, Theme.panelBorder, CircleShape)
                        .clickable { select(i) },
                ) {
                    Text(
                        text = "${i + 1}",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (i == index) Theme.bg else Theme.ink,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.panel, RoundedCornerShape(16.dp))
                .border(1.dp, Theme.panelBorder, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(LESSONS[index].title(Loc.language), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Theme.goldSoft)
            Spacer(Modifier.height(8.dp))
            Text(LESSONS[index].text(Loc.language), fontSize = 14.sp, color = Theme.inkDim)
        }
        Spacer(Modifier.height(16.dp))

        ChessBoardView(
            pieces = PieceInstance.fresh(game.board),
            selected = selected,
            legalTargets = legalTargets,
            onTap = ::tap,
            modifier = Modifier.width(320.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(Loc.t("lessonHintClick"), fontSize = 13.sp, color = Theme.goldSoft)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GhostButton(Loc.t("prevLesson")) { select(index - 1) }
            GhostButton(Loc.t("nextLesson")) { select(index + 1) }
            GhostButton(Loc.t("backToMenu")) { onBackToMenu() }
        }
    }
}
