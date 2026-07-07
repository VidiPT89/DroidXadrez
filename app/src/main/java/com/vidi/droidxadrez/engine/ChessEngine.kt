package com.vidi.droidxadrez.engine

enum class PieceColor(val code: String) {
    WHITE("w"), BLACK("b");

    val opponent: PieceColor get() = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val code: String, val value: Int) {
    PAWN("p", 100), KNIGHT("n", 320), BISHOP("b", 330), ROOK("r", 500), QUEEN("q", 900), KING("k", 20000)
}

data class Piece(val type: PieceType, val color: PieceColor)

data class Square(val r: Int, val c: Int) {
    fun inBounds(): Boolean = r in 0..7 && c in 0..7
    val name: String get() = "${"abcdefgh"[c]}${8 - r}"
}

data class Move(
    val from: Square,
    val to: Square,
    val piece: Piece,
    val capture: Boolean = false,
    val promotion: PieceType? = null,
    val enPassant: Boolean = false,
    val doubleStep: Boolean = false,
    val castle: String? = null, // "K" or "Q"
)

data class CastlingRights(var wK: Boolean = true, var wQ: Boolean = true, var bK: Boolean = true, var bQ: Boolean = true)

data class MoveRecord(
    val san: String,
    val from: Square,
    val to: Square,
    val piece: PieceType,
    val color: PieceColor,
    val capture: Boolean,
    val promotion: PieceType?,
    val castle: String?,
    val status: String, // "ok" | "check" | "checkmate" | "stalemate" | "draw"
)

enum class GameResult { CHECKMATE, STALEMATE, DRAW_50, DRAW_REPETITION, DRAW_MATERIAL }

data class GameStatus(val over: Boolean, val key: String, val winner: PieceColor?)

typealias Board = Array<Array<Piece?>>

private val KNIGHT_OFFSETS = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
private val KING_OFFSETS = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
private val BISHOP_DIRS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
private val ROOK_DIRS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun initialBoard(): Board {
    val back = arrayOf(PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN, PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK)
    val board: Board = Array(8) { arrayOfNulls(8) }
    for (c in 0..7) board[0][c] = Piece(back[c], PieceColor.BLACK)
    for (c in 0..7) board[1][c] = Piece(PieceType.PAWN, PieceColor.BLACK)
    for (c in 0..7) board[6][c] = Piece(PieceType.PAWN, PieceColor.WHITE)
    for (c in 0..7) board[7][c] = Piece(back[c], PieceColor.WHITE)
    return board
}

fun cloneBoard(board: Board): Board = Array(8) { r -> Array(8) { c -> board[r][c] } }

class ChessGame {
    var board: Board = initialBoard()
    var turn: PieceColor = PieceColor.WHITE
    var castling: CastlingRights = CastlingRights()
    var enPassant: Square? = null
    var halfmoveClock: Int = 0
    var fullmoveNumber: Int = 1
    var history: MutableList<MoveRecord> = mutableListOf()
    var positionCounts: MutableMap<String, Int> = mutableMapOf()
    var result: GameResult? = null
    var winner: PieceColor? = null

    init {
        recordPosition()
    }

    fun clone(): ChessGame {
        val g = ChessGame()
        g.board = cloneBoard(board)
        g.turn = turn
        g.castling = castling.copy()
        g.enPassant = enPassant
        g.halfmoveClock = halfmoveClock
        g.fullmoveNumber = fullmoveNumber
        g.history = history.toMutableList()
        g.positionCounts = positionCounts.toMutableMap()
        g.result = result
        g.winner = winner
        return g
    }

    fun pieceAt(r: Int, c: Int): Piece? = board[r][c]

    val isGameOver: Boolean get() = result != null

    private fun positionKey(): String {
        val sb = StringBuilder(turn.code)
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            sb.append(if (p != null) p.color.code + p.type.code else ".")
        }
        sb.append("|").append(if (castling.wK) 1 else 0).append(if (castling.wQ) 1 else 0)
            .append(if (castling.bK) 1 else 0).append(if (castling.bQ) 1 else 0)
        sb.append("|").append(enPassant?.name ?: "-")
        return sb.toString()
    }

    private fun recordPosition() {
        val key = positionKey()
        positionCounts[key] = (positionCounts[key] ?: 0) + 1
    }

    fun findKing(color: PieceColor): Square? {
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            if (p != null && p.type == PieceType.KING && p.color == color) return Square(r, c)
        }
        return null
    }

    fun isSquareAttacked(r: Int, c: Int, byColor: PieceColor): Boolean {
        for ((dr, dc) in KNIGHT_OFFSETS) {
            val rr = r + dr; val cc = c + dc
            if (Square(rr, cc).inBounds()) {
                val p = board[rr][cc]
                if (p != null && p.color == byColor && p.type == PieceType.KNIGHT) return true
            }
        }
        for ((dr, dc) in KING_OFFSETS) {
            val rr = r + dr; val cc = c + dc
            if (Square(rr, cc).inBounds()) {
                val p = board[rr][cc]
                if (p != null && p.color == byColor && p.type == PieceType.KING) return true
            }
        }
        val pawnDir = if (byColor == PieceColor.WHITE) 1 else -1
        for (dc in listOf(-1, 1)) {
            val rr = r + pawnDir; val cc = c + dc
            if (Square(rr, cc).inBounds()) {
                val p = board[rr][cc]
                if (p != null && p.color == byColor && p.type == PieceType.PAWN) return true
            }
        }
        for ((dr, dc) in BISHOP_DIRS) {
            var rr = r + dr; var cc = c + dc
            while (Square(rr, cc).inBounds()) {
                val p = board[rr][cc]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) return true
                    break
                }
                rr += dr; cc += dc
            }
        }
        for ((dr, dc) in ROOK_DIRS) {
            var rr = r + dr; var cc = c + dc
            while (Square(rr, cc).inBounds()) {
                val p = board[rr][cc]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) return true
                    break
                }
                rr += dr; cc += dc
            }
        }
        return false
    }

    fun isInCheck(color: PieceColor): Boolean {
        val king = findKing(color) ?: return false
        return isSquareAttacked(king.r, king.c, color.opponent)
    }

    private fun pseudoMoves(r: Int, c: Int): List<Move> {
        val p = board[r][c] ?: return emptyList()
        val moves = mutableListOf<Move>()
        val from = Square(r, c)

        when (p.type) {
            PieceType.PAWN -> {
                val dir = if (p.color == PieceColor.WHITE) -1 else 1
                val startRow = if (p.color == PieceColor.WHITE) 6 else 1
                val promoRow = if (p.color == PieceColor.WHITE) 0 else 7
                val oneR = r + dir
                if (Square(oneR, c).inBounds() && board[oneR][c] == null) {
                    if (oneR == promoRow) {
                        for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                            moves.add(Move(from, Square(oneR, c), p, promotion = promo))
                        }
                    } else {
                        moves.add(Move(from, Square(oneR, c), p))
                        val twoR = r + dir * 2
                        if (r == startRow && board[twoR][c] == null) {
                            moves.add(Move(from, Square(twoR, c), p, doubleStep = true))
                        }
                    }
                }
                for (dc in listOf(-1, 1)) {
                    val cc = c + dc
                    if (!Square(oneR, cc).inBounds()) continue
                    val target = board[oneR][cc]
                    if (target != null && target.color != p.color) {
                        if (oneR == promoRow) {
                            for (promo in listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                moves.add(Move(from, Square(oneR, cc), p, capture = true, promotion = promo))
                            }
                        } else {
                            moves.add(Move(from, Square(oneR, cc), p, capture = true))
                        }
                    } else if (target == null) {
                        val ep = enPassant
                        if (ep != null && ep.r == oneR && ep.c == cc) {
                            moves.add(Move(from, Square(oneR, cc), p, capture = true, enPassant = true))
                        }
                    }
                }
            }
            PieceType.KNIGHT -> {
                for ((dr, dc) in KNIGHT_OFFSETS) {
                    val rr = r + dr; val cc = c + dc
                    if (!Square(rr, cc).inBounds()) continue
                    val target = board[rr][cc]
                    if (target != null) {
                        if (target.color != p.color) moves.add(Move(from, Square(rr, cc), p, capture = true))
                    } else {
                        moves.add(Move(from, Square(rr, cc), p))
                    }
                }
            }
            PieceType.KING -> {
                for ((dr, dc) in KING_OFFSETS) {
                    val rr = r + dr; val cc = c + dc
                    if (!Square(rr, cc).inBounds()) continue
                    val target = board[rr][cc]
                    if (target != null) {
                        if (target.color != p.color) moves.add(Move(from, Square(rr, cc), p, capture = true))
                    } else {
                        moves.add(Move(from, Square(rr, cc), p))
                    }
                }
                moves.addAll(castlingMoves(r, c, p.color, p))
            }
            else -> {
                val dirs = when (p.type) {
                    PieceType.BISHOP -> BISHOP_DIRS
                    PieceType.ROOK -> ROOK_DIRS
                    else -> BISHOP_DIRS + ROOK_DIRS
                }
                for ((dr, dc) in dirs) {
                    var rr = r + dr; var cc = c + dc
                    while (Square(rr, cc).inBounds()) {
                        val target = board[rr][cc]
                        if (target != null) {
                            if (target.color != p.color) moves.add(Move(from, Square(rr, cc), p, capture = true))
                            break
                        } else {
                            moves.add(Move(from, Square(rr, cc), p))
                        }
                        rr += dr; cc += dc
                    }
                }
            }
        }
        return moves
    }

    private fun castlingMoves(r: Int, c: Int, color: PieceColor, piece: Piece): List<Move> {
        if (isInCheck(color)) return emptyList()
        val rank = if (color == PieceColor.WHITE) 7 else 0
        if (r != rank || c != 4) return emptyList()
        val opp = color.opponent
        val moves = mutableListOf<Move>()

        val kingSide = if (color == PieceColor.WHITE) castling.wK else castling.bK
        if (kingSide && board[rank][5] == null && board[rank][6] == null) {
            val rook = board[rank][7]
            if (rook != null && rook.type == PieceType.ROOK && rook.color == color &&
                !isSquareAttacked(rank, 5, opp) && !isSquareAttacked(rank, 6, opp)
            ) {
                moves.add(Move(Square(r, c), Square(rank, 6), piece, castle = "K"))
            }
        }
        val queenSide = if (color == PieceColor.WHITE) castling.wQ else castling.bQ
        if (queenSide && board[rank][3] == null && board[rank][2] == null && board[rank][1] == null) {
            val rook = board[rank][0]
            if (rook != null && rook.type == PieceType.ROOK && rook.color == color &&
                !isSquareAttacked(rank, 3, opp) && !isSquareAttacked(rank, 2, opp)
            ) {
                moves.add(Move(Square(r, c), Square(rank, 2), piece, castle = "Q"))
            }
        }
        return moves
    }

    private fun applyMoveRaw(move: Move): Piece? {
        val piece = board[move.from.r][move.from.c]!!
        val captured = board[move.to.r][move.to.c]

        board[move.to.r][move.to.c] = Piece(move.promotion ?: piece.type, piece.color)
        board[move.from.r][move.from.c] = null

        if (move.enPassant) {
            val capR = if (piece.color == PieceColor.WHITE) move.to.r + 1 else move.to.r - 1
            board[capR][move.to.c] = null
        }
        if (move.castle == "K") {
            val rank = move.from.r
            board[rank][5] = board[rank][7]
            board[rank][7] = null
        } else if (move.castle == "Q") {
            val rank = move.from.r
            board[rank][3] = board[rank][0]
            board[rank][0] = null
        }
        return captured
    }

    private fun legalMoves(r: Int, c: Int): List<Move> {
        val p = board[r][c] ?: return emptyList()
        val pseudo = pseudoMoves(r, c)
        val legal = mutableListOf<Move>()
        for (move in pseudo) {
            val savedBoard = cloneBoard(board)
            val savedCastling = castling.copy()
            val savedEnPassant = enPassant
            applyMoveRaw(move)
            val stillInCheck = isInCheck(p.color)
            board = savedBoard
            castling = savedCastling
            enPassant = savedEnPassant
            if (!stillInCheck) legal.add(move)
        }
        return legal
    }

    fun legalMoves(square: Square): List<Move> {
        val p = board[square.r][square.c]
        if (p == null || p.color != turn || isGameOver) return emptyList()
        return legalMoves(square.r, square.c)
    }

    fun allLegalMoves(color: PieceColor = turn): List<Move> {
        if (isGameOver) return emptyList()
        val moves = mutableListOf<Move>()
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            if (p != null && p.color == color) moves.addAll(legalMoves(r, c))
        }
        return moves
    }

    private fun sanFor(move: Move, legalMovesThisTurn: List<Move>): String {
        if (move.castle == "K") return "O-O"
        if (move.castle == "Q") return "O-O-O"
        val p = move.piece
        val dest = move.to.name

        if (p.type == PieceType.PAWN) {
            val sb = StringBuilder()
            if (move.capture) sb.append("abcdefgh"[move.from.c]).append("x")
            sb.append(dest)
            if (move.promotion != null) sb.append("=").append(move.promotion.code.uppercase())
            return sb.toString()
        }

        val letter = p.type.code.uppercase()
        val ambiguous = legalMovesThisTurn.filter { it != move && it.piece.type == p.type && it.to == move.to }
        var disambig = ""
        if (ambiguous.isNotEmpty()) {
            val sameFile = ambiguous.any { it.from.c == move.from.c }
            val sameRank = ambiguous.any { it.from.r == move.from.r }
            disambig = when {
                !sameFile -> "abcdefgh"[move.from.c].toString()
                !sameRank -> "${8 - move.from.r}"
                else -> move.from.name
            }
        }
        return "$letter$disambig${if (move.capture) "x" else ""}$dest"
    }

    fun makeMove(from: Square, to: Square, promotion: PieceType? = null): MoveRecord? {
        if (isGameOver) return null
        val legalNow = allLegalMoves(turn)
        val match = legalNow.firstOrNull { it.from == from && it.to == to && it.promotion == promotion } ?: return null

        val sanBase = sanFor(match, legalNow)
        val piece = board[match.from.r][match.from.c]!!
        val isPawnMove = piece.type == PieceType.PAWN
        val captured = applyMoveRaw(match)

        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) { castling.wK = false; castling.wQ = false }
            else { castling.bK = false; castling.bQ = false }
        }
        if (piece.type == PieceType.ROOK) {
            if (match.from.r == 7 && match.from.c == 0) castling.wQ = false
            if (match.from.r == 7 && match.from.c == 7) castling.wK = false
            if (match.from.r == 0 && match.from.c == 0) castling.bQ = false
            if (match.from.r == 0 && match.from.c == 7) castling.bK = false
        }
        if (captured != null && captured.type == PieceType.ROOK) {
            if (match.to.r == 7 && match.to.c == 0) castling.wQ = false
            if (match.to.r == 7 && match.to.c == 7) castling.wK = false
            if (match.to.r == 0 && match.to.c == 0) castling.bQ = false
            if (match.to.r == 0 && match.to.c == 7) castling.bK = false
        }

        enPassant = if (match.doubleStep) Square((match.from.r + match.to.r) / 2, match.from.c) else null
        halfmoveClock = if (isPawnMove || captured != null || match.enPassant) 0 else halfmoveClock + 1
        if (turn == PieceColor.BLACK) fullmoveNumber += 1

        val movedColor = turn
        turn = turn.opponent
        recordPosition()

        val nextMoves = allLegalMoves(turn)
        val inCheck = isInCheck(turn)
        var sanFinal = sanBase
        var status = "ok"
        if (nextMoves.isEmpty()) {
            if (inCheck) {
                sanFinal += "#"
                result = GameResult.CHECKMATE
                winner = movedColor
                status = "checkmate"
            } else {
                result = GameResult.STALEMATE
                status = "stalemate"
            }
        } else if (inCheck) {
            sanFinal += "+"
            status = "check"
        }

        if (!isGameOver) {
            if (halfmoveClock >= 100) { result = GameResult.DRAW_50; status = "draw" }
            else if ((positionCounts[positionKey()] ?: 0) >= 3) { result = GameResult.DRAW_REPETITION; status = "draw" }
            else if (isInsufficientMaterial()) { result = GameResult.DRAW_MATERIAL; status = "draw" }
        }

        val record = MoveRecord(sanFinal, match.from, match.to, piece.type, movedColor,
            captured != null || match.enPassant, match.promotion, match.castle, status)
        history.add(record)
        return record
    }

    private fun isInsufficientMaterial(): Boolean {
        val pieces = mutableListOf<Piece>()
        for (r in 0..7) for (c in 0..7) board[r][c]?.let { pieces.add(it) }
        if (pieces.size > 4) return false
        val nonKings = pieces.filter { it.type != PieceType.KING }
        if (nonKings.isEmpty()) return true
        if (nonKings.size == 1 && (nonKings[0].type == PieceType.BISHOP || nonKings[0].type == PieceType.KNIGHT)) return true
        if (nonKings.size == 2 && nonKings.all { it.type == PieceType.BISHOP }) return true
        return false
    }

    fun gameStatusText(): GameStatus = when (result) {
        GameResult.CHECKMATE -> GameStatus(true, "checkmate", winner)
        GameResult.STALEMATE -> GameStatus(true, "stalemate", null)
        GameResult.DRAW_50 -> GameStatus(true, "draw50", null)
        GameResult.DRAW_REPETITION -> GameStatus(true, "drawRepetition", null)
        GameResult.DRAW_MATERIAL -> GameStatus(true, "drawMaterial", null)
        null -> if (isInCheck(turn)) GameStatus(false, "check", null) else GameStatus(false, "playing", null)
    }

    /** Rebuilds state by replaying the history minus the last [n] plies. */
    fun undoPlies(n: Int) {
        val keep = history.subList(0, maxOf(0, history.size - n))
        val fresh = ChessGame()
        for (record in keep) {
            fresh.makeMove(record.from, record.to, record.promotion)
        }
        board = fresh.board
        turn = fresh.turn
        castling = fresh.castling
        enPassant = fresh.enPassant
        halfmoveClock = fresh.halfmoveClock
        fullmoveNumber = fresh.fullmoveNumber
        history = fresh.history
        positionCounts = fresh.positionCounts
        result = fresh.result
        winner = fresh.winner
    }
}
