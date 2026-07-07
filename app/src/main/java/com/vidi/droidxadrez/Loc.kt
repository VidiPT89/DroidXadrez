package com.vidi.droidxadrez

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String) { PT("pt"), EN("en") }

private val STRINGS: Map<AppLanguage, Map<String, String>> = mapOf(
    AppLanguage.PT to mapOf(
        "menuTitle" to "Xadrez",
        "menuSubtitle" to "Escolhe um modo para começar",
        "mode1v1" to "1 vs 1",
        "mode1v1Desc" to "Dois jogadores, mesmo ecrã",
        "modeBot" to "Contra o Bot",
        "modeBotDesc" to "Escolhe o nível da IA",
        "modeTutorial" to "Tutorial",
        "modeTutorialDesc" to "Aprende a jogar por níveis",
        "modeHelp" to "Ajuda",
        "modeHelpDesc" to "Regras e controlos",
        "chooseDifficulty" to "Escolhe o nível do bot",
        "levelBeginner" to "Iniciante",
        "levelEasy" to "Fácil",
        "levelMedium" to "Médio",
        "levelHard" to "Difícil",
        "cancelBtn" to "Cancelar",
        "blackPlayer" to "Pretas",
        "whitePlayer" to "Brancas",
        "moveHistory" to "Histórico de Lances",
        "undoMove" to "↩️ Voltar Atrás",
        "flipBoard" to "Inverter",
        "newGame" to "Novo Jogo",
        "backToMenu" to "Menu",
        "prevLesson" to "Anterior",
        "nextLesson" to "Seguinte",
        "helpTitle" to "Ajuda",
        "footerBy" to "Desenvolvido por",
        "introTitle" to "Xadrez",
        "introText" to "Joga 1 vs 1, desafia o bot, aprende as regras e as estratégias — tudo num só tabuleiro.",
        "introSkip" to "Saltar",
        "turnWhite" to "Vez das Brancas",
        "turnBlack" to "Vez das Pretas",
        "thinking" to "O bot está a pensar…",
        "inCheck" to "Xeque!",
        "resultCheckmateTitle" to "Xeque-mate!",
        "resultCheckmateWhite" to "As Brancas vencem.",
        "resultCheckmateBlack" to "As Pretas vencem.",
        "resultStalemateTitle" to "Tabuada por Afogamento",
        "resultStalemateText" to "Nenhum jogador tem lances legais. O jogo termina empatado.",
        "resultDraw50Title" to "Empate",
        "resultDraw50Text" to "50 lances sem capturas nem movimento de peão.",
        "resultDrawRepTitle" to "Empate",
        "resultDrawRepText" to "A mesma posição repetiu-se três vezes.",
        "resultDrawMatTitle" to "Empate",
        "resultDrawMatText" to "Nenhum dos lados tem material suficiente para dar mate.",
        "promoTitle" to "Promover peão a:",
        "lessonHintClick" to "Toca numa peça para veres os seus movimentos.",
    ),
    AppLanguage.EN to mapOf(
        "menuTitle" to "Chess",
        "menuSubtitle" to "Choose a mode to begin",
        "mode1v1" to "1 vs 1",
        "mode1v1Desc" to "Two players, same screen",
        "modeBot" to "Vs Bot",
        "modeBotDesc" to "Choose the AI level",
        "modeTutorial" to "Tutorial",
        "modeTutorialDesc" to "Learn to play, level by level",
        "modeHelp" to "Help",
        "modeHelpDesc" to "Rules and controls",
        "chooseDifficulty" to "Choose the bot's level",
        "levelBeginner" to "Beginner",
        "levelEasy" to "Easy",
        "levelMedium" to "Medium",
        "levelHard" to "Hard",
        "cancelBtn" to "Cancel",
        "blackPlayer" to "Black",
        "whitePlayer" to "White",
        "moveHistory" to "Move History",
        "undoMove" to "↩️ Undo",
        "flipBoard" to "Flip",
        "newGame" to "New Game",
        "backToMenu" to "Menu",
        "prevLesson" to "Previous",
        "nextLesson" to "Next",
        "helpTitle" to "Help",
        "footerBy" to "Developed by",
        "introTitle" to "Chess",
        "introText" to "Play 1 vs 1, challenge the bot, learn the rules and strategy — all on one board.",
        "introSkip" to "Skip",
        "turnWhite" to "White to move",
        "turnBlack" to "Black to move",
        "thinking" to "The bot is thinking…",
        "inCheck" to "Check!",
        "resultCheckmateTitle" to "Checkmate!",
        "resultCheckmateWhite" to "White wins.",
        "resultCheckmateBlack" to "Black wins.",
        "resultStalemateTitle" to "Stalemate",
        "resultStalemateText" to "Neither player has a legal move. The game is a draw.",
        "resultDraw50Title" to "Draw",
        "resultDraw50Text" to "50 moves without a capture or pawn move.",
        "resultDrawRepTitle" to "Draw",
        "resultDrawRepText" to "The same position occurred three times.",
        "resultDrawMatTitle" to "Draw",
        "resultDrawMatText" to "Neither side has enough material to checkmate.",
        "promoTitle" to "Promote pawn to:",
        "lessonHintClick" to "Tap a piece to see how it moves.",
    ),
)

object Loc {
    private const val PREFS = "xadrez_prefs"
    private const val KEY_LANG = "lang"

    private lateinit var prefs: SharedPreferences
    var language by mutableStateOf(AppLanguage.PT)
        private set

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        language = AppLanguage.entries.firstOrNull { it.code == prefs.getString(KEY_LANG, "pt") } ?: AppLanguage.PT
    }

    fun toggle() {
        language = if (language == AppLanguage.PT) AppLanguage.EN else AppLanguage.PT
        prefs.edit().putString(KEY_LANG, language.code).apply()
    }

    fun t(key: String): String = STRINGS[language]?.get(key) ?: STRINGS.getValue(AppLanguage.PT).getValue(key)
}
