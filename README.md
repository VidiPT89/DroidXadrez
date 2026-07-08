# ♚ Xadrez — Joga, Aprende, Domina o Tabuleiro (Android)

> Uma app de xadrez nativa para Android, construída com Kotlin e Jetpack Compose — joga 1 vs 1, desafia um bot com 4 níveis de dificuldade, e aprende com um tutorial passo-a-passo.

A versão Android nativa de [Xadrez para web](https://github.com/VidiPT89/Xadrez) e [iXadrez para iOS](https://github.com/VidiPT89/iXadrez). Xadrez é uma app de jogo de tabuleiro completa construída com **Kotlin** e **Jetpack Compose** — sem bibliotecas de xadrez externas, sem dependências de terceiros. O motor de regras, a inteligência artificial e toda a interface são feitos de raiz. Joga uma partida local a dois, desafia o bot num dos quatro níveis de dificuldade, ou aprende a jogar do zero com lições interativas sobre movimentação, regras especiais, aberturas, táticas e finais.

## 📦 What's Inside

- ♟️ Motor de regras completo: todos os movimentos das peças, roque (ambos os lados), en passant, promoção, xeque, xeque-mate, afogamento, regra dos 50 lances, material insuficiente e repetição tripla
- 📜 Histórico de lances em notação algébrica simplificada, com desambiguação automática
- 🧑‍🤝‍🧑 Modo 1 vs 1 — dois jogadores alternam no mesmo dispositivo
- 🤖 Modo Contra o Bot com 4 níveis de dificuldade (Iniciante, Fácil, Médio, Difícil), motor minimax com poda alfa-beta, tabelas de posição por peça e pesquisa por tempo limitado no nível Difícil
- 🧠 O bot pesquisa numa coroutine em `Dispatchers.Default` — nunca bloqueia a interface, mesmo a pensar em profundidade
- 🎓 Modo Tutorial com 5 lições interativas: movimentação das peças, regras especiais, princípios de abertura, táticas básicas (garfos, cravos, espetos) e finais básicos
- ❓ Modo Ajuda com referência rápida de regras, modos de jogo e controlos
- 🇵🇹 🇬🇧 Alternância de idioma entre Português Europeu e Inglês, guardada entre sessões
- 🔊 Efeitos sonoros sintetizados em tempo real via `AudioTrack` para lances, capturas, xeque e fim de jogo
- 🎬 Splash de abertura animado com apresentação da app, que desaparece automaticamente
- 🖼️ Tabuleiro totalmente adaptável, com destaque de lances legais, última jogada e xeque
- 🌐 Modo Multijogador — joga online com um amigo através de uma sala com código de 6 caracteres ou link de convite, com chat em tempo real, indicador de presença do adversário e desistência

## 🛠️ Tech Stack

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
![Material Design 3](https://img.shields.io/badge/Material%20Design%203-757575?style=flat&logo=material-design&logoColor=white)
![Min SDK](https://img.shields.io/badge/Min%20SDK-26%20(Android%208)-green?style=flat&logo=android&logoColor=white)

## 🏗️ Architecture

```
com.vidi.droidxadrez/
├── engine
│   ├── ChessEngine.kt       →  Motor de regras puro (geração e validação de lances, notação)
│   └── ChessAI.kt            →  Bot de IA (minimax + poda alfa-beta), corre numa coroutine
│
├── ui (um pacote de ecrãs Compose)
│   ├── GameViewModel.kt       →  Estado do jogo, liga o motor à interface
│   ├── ChessBoardView.kt       →  Renderização do tabuleiro, partilhada entre jogo e tutorial
│   ├── GameScreen.kt            →  Ecrã de jogo: tabuleiro, painel, promoção, resultado
│   ├── MainMenuScreen.kt         →  Menu principal e seletor de dificuldade
│   ├── TutorialScreen.kt          →  5 lições interativas
│   ├── HelpScreen.kt               →  Referência de regras e controlos
│   ├── SplashScreen.kt              →  Apresentação inicial
│   ├── MultiplayerViewModel.kt       →  Liga o MultiplayerService ao GameViewModel
│   └── MultiplayerLobbyScreen.kt      →  Criar/entrar numa sala, partilhar convite
│
├── multiplayer
│   └── MultiplayerService.kt   →  Salas, lances, chat e presença via Firestore
│
├── Loc.kt          →  Strings PT/EN, preferência guardada em SharedPreferences
├── SoundEngine.kt   →  Sintetizador de efeitos sonoros em tempo real via AudioTrack
├── Theme.kt          →  Cores e tema Material3 partilhados
└── MainActivity.kt    →  Hospeda a árvore Compose, roteamento entre ecrãs, header e footer
```

## ⚙️ Game Mechanics

```
Cada lance:
  1. O jogador ativo toca numa peça sua — os lances legais dessa peça ficam destacados
  2. Toca numa casa destacada para jogar (um anel indica captura)
  3. Se o lance for uma promoção, um diálogo pede a peça de destino (Dama, Torre, Bispo ou Cavalo)
  4. O motor atualiza o estado: roque, en passant e direitos de roque são geridos automaticamente
  5. Após o lance, verifica-se xeque, xeque-mate, afogamento e as três regras de empate
  6. No modo Contra o Bot, quando é a vez das Pretas, uma coroutine em background calcula o
     melhor lance para o nível escolhido e devolve-o à thread principal assim que termina
```

## 🤖 Níveis do Bot

```
Iniciante — profundidade 1, comete erros propositadamente com frequência
Fácil     — profundidade 2, pequena margem de aleatoriedade
Médio     — profundidade 3, quase sempre joga o melhor lance encontrado
Difícil   — profundidade 4+ com quiescence search e aprofundamento iterativo,
            sempre o melhor lance dentro do tempo disponível
```

## 🚀 How to Run

```bash
# 1. Clone the repository
git clone https://github.com/VidiPT89/DroidXadrez.git
cd DroidXadrez

# 2. Open in Android Studio and run on an emulator or device
#    (or from the command line, with the Android SDK configured):
./gradlew installDebug
```

Requires Android Studio (Koala or newer) and a device/emulator running Android 8.0 (API 26) or later.

## 🌐 Multijogador

O modo Multijogador usa [Firebase](https://firebase.google.com/) (Firestore + autenticação anónima) para sincronizar as jogadas e o chat entre dispositivos em tempo real — sem servidor próprio. Para ativar:

1. Cria um projeto gratuito na [Firebase Console](https://console.firebase.google.com/), ativa **Authentication → Anonymous** e cria uma **Firestore Database** em modo produção.
2. Publica as regras de segurança do Firestore.
3. Regista uma app Android no projeto com o package `com.vidi.droidxadrez`, descarrega o `google-services.json` gerado e substitui o ficheiro placeholder em `app/`.

Sem esta configuração, a app funciona normalmente em todos os outros modos — o cartão "Multijogador" fica apenas indisponível.

## 📝 Notes

- Todo o motor de xadrez e a IA foram escritos de raiz, sem bibliotecas externas
- O mesmo motor de regras existe em três implementações independentes — JavaScript ([Xadrez](https://github.com/VidiPT89/Xadrez)), Swift ([iXadrez](https://github.com/VidiPT89/iXadrez)) e Kotlin (este repositório) — mantendo a mesma lógica e o mesmo comportamento em cada plataforma
- As preferências de idioma e som são guardadas com `SharedPreferences`, persistindo entre sessões

---

Developed by **David Arsénio Martins**
