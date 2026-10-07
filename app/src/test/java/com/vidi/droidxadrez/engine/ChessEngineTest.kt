package com.vidi.droidxadrez.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Perft counts every legal move tree to a fixed depth from well-known positions and compares
 * against published totals — it catches castling, en passant, promotion and pin bugs at once.
 * The same positions are checked by the web (tests/engine.test.js) and iOS ports.
 */
class ChessEngineTest {
    private fun sq(s: String) = Square(8 - (s[1] - '0'), "abcdefgh".indexOf(s[0]))

    private fun fromFen(fen: String): ChessGame {
        val parts = fen.split(" ")
        val board: Board = Array(8) { arrayOfNulls<Piece?>(8) }
        parts[0].split("/").forEachIndexed { r, row ->
            var c = 0
            for (ch in row) {
                if (ch.isDigit()) {
                    c += ch - '0'
                } else {
                    val type = PieceType.entries.first { it.code == ch.lowercase() }
                    board[r][c] = Piece(type, if (ch.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK)
                    c++
                }
            }
        }
        val g = ChessGame()
        g.board = board
        g.turn = if (parts[1] == "w") PieceColor.WHITE else PieceColor.BLACK
        g.castling = CastlingRights(parts[2].contains('K'), parts[2].contains('Q'), parts[2].contains('k'), parts[2].contains('q'))
        g.enPassant = if (parts[3] == "-") null else sq(parts[3])
        g.history = mutableListOf()
        return g
    }

    private fun perft(game: ChessGame, depth: Int): Int {
        val moves = game.allLegalMoves(game.turn)
        if (depth == 1) return moves.size
        var nodes = 0
        for (m in moves) {
            val child = game.clone()
            child.makeMove(m.from, m.to, m.promotion)
            child.result = null // draws end a real game, but perft must keep counting
            nodes += perft(child, depth - 1)
        }
        return nodes
    }

    private fun assertPerft(fen: String, vararg counts: Int) {
        counts.forEachIndexed { i, expected -> assertEquals("perft ${i + 1} $fen", expected, perft(fromFen(fen), i + 1)) }
    }

    @Test fun perftStartPosition() = assertPerft("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 20, 400, 8902)
    @Test fun perftKiwipete() = assertPerft("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1", 48, 2039)
    @Test fun perftEndgame() = assertPerft("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1", 14, 191, 2812)
    @Test fun perftPromotions() = assertPerft("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1", 6, 264, 9467)
    @Test fun perftPosition5() = assertPerft("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8", 44, 1486)

    /** Each position becomes K+minors vs K after the white king captures the rook on d2. */
    private fun resultAfterKxd2(fen: String): GameResult? {
        val g = fromFen(fen)
        g.makeMove(sq("e1"), sq("d2"))
        return g.result
    }

    @Test fun insufficientMaterial() {
        assertEquals(null, resultAfterKxd2("4k3/8/8/8/8/8/3r4/2B1KB2 w - - 0 1")) // bishops on both colours mate
        assertEquals(GameResult.DRAW_MATERIAL, resultAfterKxd2("4kb2/8/8/8/8/8/3r4/2B1K3 w - - 0 1")) // same colour
        assertEquals(null, resultAfterKxd2("2b1k3/8/8/8/8/8/3r4/2B1K3 w - - 0 1")) // opposite colours
        assertEquals(GameResult.DRAW_MATERIAL, resultAfterKxd2("4k3/8/8/8/8/8/3r4/1N2K3 w - - 0 1")) // lone knight
    }

    @Test fun threefoldRepetitionIgnoresUnusableEnPassantSquare() {
        val g = ChessGame()
        listOf("e2" to "e4", "g8" to "f6", "g1" to "f3", "f6" to "g8", "f3" to "g1",
            "g8" to "f6", "g1" to "f3", "f6" to "g8", "f3" to "g1").forEach { (a, b) -> g.makeMove(sq(a), sq(b)) }
        assertEquals(GameResult.DRAW_REPETITION, g.result)
    }

    @Test fun foolsMate() {
        val g = ChessGame()
        listOf("f2" to "f3", "e7" to "e5", "g2" to "g4", "d8" to "h4").forEach { (a, b) -> g.makeMove(sq(a), sq(b)) }
        assertEquals(GameResult.CHECKMATE, g.result)
        assertEquals(PieceColor.BLACK, g.winner)
        assertEquals("Qh4#", g.history.last().san)
    }

    @Test fun botFindsMateInOne() {
        val g = fromFen("r3k3/8/8/8/8/8/5PPP/6K1 b - - 0 1")
        for (level in listOf(BotDifficulty.MEDIUM, BotDifficulty.HARD)) {
            assertEquals("bot $level", sq("a1"), ChessAI.pickMove(g, level)?.to)
        }
    }
}
