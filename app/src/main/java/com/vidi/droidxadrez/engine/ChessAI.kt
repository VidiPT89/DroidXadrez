package com.vidi.droidxadrez.engine

import kotlin.random.Random

enum class BotDifficulty { BEGINNER, EASY, MEDIUM, HARD }

private data class LevelConfig(val depth: Int, val margin: Int, val blunderChance: Double, val quiescence: Boolean, val timeBudgetMs: Long)

private val LEVELS = mapOf(
    BotDifficulty.BEGINNER to LevelConfig(depth = 1, margin = 150, blunderChance = 0.35, quiescence = false, timeBudgetMs = 400),
    BotDifficulty.EASY to LevelConfig(depth = 2, margin = 60, blunderChance = 0.12, quiescence = false, timeBudgetMs = 700),
    BotDifficulty.MEDIUM to LevelConfig(depth = 3, margin = 20, blunderChance = 0.0, quiescence = true, timeBudgetMs = 1500),
    BotDifficulty.HARD to LevelConfig(depth = 4, margin = 0, blunderChance = 0.0, quiescence = true, timeBudgetMs = 3000),
)

private const val MATE_SCORE = 1_000_000

private val PST: Map<PieceType, Array<IntArray>> = mapOf(
    PieceType.PAWN to arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5, 5, 10, 25, 25, 10, 5, 5),
        intArrayOf(0, 0, 0, 20, 20, 0, 0, 0),
        intArrayOf(5, -5, -10, 0, 0, -10, -5, 5),
        intArrayOf(5, 10, 10, -20, -20, 10, 10, 5),
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
    ),
    PieceType.KNIGHT to arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20, 0, 0, 0, 0, -20, -40),
        intArrayOf(-30, 0, 10, 15, 15, 10, 0, -30),
        intArrayOf(-30, 5, 15, 20, 20, 15, 5, -30),
        intArrayOf(-30, 0, 15, 20, 20, 15, 0, -30),
        intArrayOf(-30, 5, 10, 15, 15, 10, 5, -30),
        intArrayOf(-40, -20, 0, 5, 5, 0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
    ),
    PieceType.BISHOP to arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 10, 10, 5, 0, -10),
        intArrayOf(-10, 5, 5, 10, 10, 5, 5, -10),
        intArrayOf(-10, 0, 10, 10, 10, 10, 0, -10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10, -10),
        intArrayOf(-10, 5, 0, 0, 0, 0, 5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
    ),
    PieceType.ROOK to arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 0),
        intArrayOf(5, 10, 10, 10, 10, 10, 10, 5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(-5, 0, 0, 0, 0, 0, 0, -5),
        intArrayOf(0, 0, 0, 5, 5, 0, 0, 0),
    ),
    PieceType.QUEEN to arrayOf(
        intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20),
        intArrayOf(-10, 0, 0, 0, 0, 0, 0, -10),
        intArrayOf(-10, 0, 5, 5, 5, 5, 0, -10),
        intArrayOf(-5, 0, 5, 5, 5, 5, 0, -5),
        intArrayOf(0, 0, 5, 5, 5, 5, 0, -5),
        intArrayOf(-10, 5, 5, 5, 5, 5, 0, -10),
        intArrayOf(-10, 0, 5, 0, 0, 0, 0, -10),
        intArrayOf(-20, -10, -10, -5, -5, -10, -10, -20),
    ),
    PieceType.KING to arrayOf(
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
        intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
        intArrayOf(20, 20, 0, 0, 0, 0, 20, 20),
        intArrayOf(20, 30, 10, 0, 0, 10, 30, 20),
    ),
)

object ChessAI {
    private fun evaluate(game: ChessGame): Int {
        var score = 0
        for (r in 0..7) for (c in 0..7) {
            val p = game.board[r][c] ?: continue
            val pstRow = if (p.color == PieceColor.WHITE) r else 7 - r
            val pstVal = PST.getValue(p.type)[pstRow][c]
            val sign = if (p.color == PieceColor.WHITE) 1 else -1
            score += sign * (p.type.value + pstVal)
        }
        return score
    }

    private fun orderMoves(moves: List<Move>): List<Move> {
        fun score(m: Move): Int {
            var s = 0
            if (m.capture) s += 1000
            if (m.promotion != null) s += 900
            return s
        }
        return moves.sortedByDescending { score(it) }
    }

    private fun quiescence(game: ChessGame, alphaIn: Int, beta: Int, colorSign: Int, qdepth: Int): Int {
        var alpha = alphaIn
        val standPat = colorSign * evaluate(game)
        if (qdepth <= 0) return standPat
        if (standPat >= beta) return beta
        if (standPat > alpha) alpha = standPat

        val moves = orderMoves(game.allLegalMoves(game.turn).filter { it.capture || it.promotion != null })
        for (move in moves) {
            val child = game.clone()
            child.makeMove(move.from, move.to, move.promotion)
            val score = -quiescence(child, -beta, -alpha, -colorSign, qdepth - 1)
            if (score >= beta) return beta
            if (score > alpha) alpha = score
        }
        return alpha
    }

    private fun negamax(game: ChessGame, depth: Int, alphaIn: Int, beta: Int, colorSign: Int, useQuiescence: Boolean, deadline: Long): Int {
        var alpha = alphaIn
        if (game.isGameOver) {
            val status = game.gameStatusText()
            return if (status.key == "checkmate") -MATE_SCORE - depth else 0
        }
        if (depth == 0) {
            return if (useQuiescence) quiescence(game, alpha, beta, colorSign, 4) else colorSign * evaluate(game)
        }
        if (System.currentTimeMillis() > deadline) return colorSign * evaluate(game)

        val moves = orderMoves(game.allLegalMoves(game.turn))
        if (moves.isEmpty()) {
            return if (game.isInCheck(game.turn)) -MATE_SCORE - depth else 0
        }

        var best = Int.MIN_VALUE / 2
        for (move in moves) {
            val child = game.clone()
            child.makeMove(move.from, move.to, move.promotion)
            val score = -negamax(child, depth - 1, -beta, -alpha, -colorSign, useQuiescence, deadline)
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        return best
    }

    /** Picks a move for the side to move. Safe to call from a background thread. */
    fun pickMove(game: ChessGame, difficulty: BotDifficulty): Move? {
        val cfg = LEVELS.getValue(difficulty)
        val rootMoves = game.allLegalMoves(game.turn)
        if (rootMoves.isEmpty()) return null

        if (Random.nextDouble() < cfg.blunderChance) {
            return rootMoves.random()
        }

        val colorSign = if (game.turn == PieceColor.WHITE) 1 else -1
        val deadline = System.currentTimeMillis() + cfg.timeBudgetMs
        var scored: List<Pair<Move, Int>> = emptyList()

        if (cfg.timeBudgetMs >= 1500) {
            var currentOrder = orderMoves(rootMoves)
            for (d in 1..cfg.depth) {
                val results = mutableListOf<Pair<Move, Int>>()
                for (move in currentOrder) {
                    val child = game.clone()
                    child.makeMove(move.from, move.to, move.promotion)
                    val score = -negamax(child, d - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, -colorSign, cfg.quiescence, deadline)
                    results.add(move to score)
                    if (System.currentTimeMillis() > deadline) break
                }
                val sorted = results.sortedByDescending { it.second }
                currentOrder = sorted.map { it.first }
                scored = sorted
                if (System.currentTimeMillis() > deadline) break
            }
        } else {
            scored = orderMoves(rootMoves).map { move ->
                val child = game.clone()
                child.makeMove(move.from, move.to, move.promotion)
                val score = -negamax(child, cfg.depth - 1, Int.MIN_VALUE / 2, Int.MAX_VALUE / 2, -colorSign, cfg.quiescence, deadline)
                move to score
            }.sortedByDescending { it.second }
        }

        if (scored.isEmpty()) return rootMoves.random()
        val best = scored[0].second
        val within = scored.filter { best - it.second <= cfg.margin }
        return within.random().first
    }
}
