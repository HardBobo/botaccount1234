# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

This is a Java chess engine and Lichess bot named "botaccount1234" that:
- Connects to Lichess via streaming API to play automated chess games
- Uses negamax algorithm with alpha-beta pruning for move search
- Implements opening book from grandmaster games
- Supports all chess rules including castling, en passant, and promotion

## Development Commands

### Building and Running
```bash
# First-time setup (Windows)
.\setup.ps1

# Compile the project (from IntelliJ or command line)
javac -cp "path/to/json-20240303.jar" src/*.java -d out/

# Configure your bot token (choose one method):
# Method 1: Edit bot.properties file with your token
# Method 2: Set environment variable
$env:LICHESS_BOT_TOKEN = "your_actual_token_here"

# Run the Lichess bot (main entry point)
java -cp "out;path/to/json-20240303.jar" LichessBotStream

# Run individual components for testing
java -cp "out;path/to/json-20240303.jar" BotEngine
java -cp "out;path/to/json-20240303.jar" PerftMoveGenTest
```

### Dependencies
- **JSON library**: Uses `json-20240303.jar` (located in `~/Downloads/` per .iml file)
- **Java 21**: Project configured for JDK 21
- **IntelliJ IDEA**: Primary development environment

### Testing
```bash
# Run move generation performance tests
java -cp "out:path/to/json-20240303.jar" PerftMoveGenTest

# Test opening book functionality
java -cp "out:path/to/json-20240303.jar" OpeningDictionary
```

## Code Architecture

### Core Components

**Game Engine**
- `MoveFinder.java`: Core chess engine with negamax search, alpha-beta pruning, quiescence search, and iterative deepening
- `Board.java`: Board representation, piece placement, FEN parsing, and game state management
- `Evaluation.java`: Position evaluation function for the chess engine
- `Spiel.java`: Game logic utilities including move validation, check detection, and attack calculations

**Lichess Integration**  
- `LichessBotStream.java`: **Main entry point** - handles Lichess API streaming, challenge acceptance, and game state management

**Piece System**
- `Piece.java`: Abstract base class for all chess pieces
- Individual piece classes: `Bauer.java` (Pawn), `Turm.java` (Rook), `Springer.java` (Knight), `Laeufer.java` (Bishop), `Dame.java` (Queen), `Koenig.java` (King), `Empty.java`
- Each piece implements `moeglicheZuege()` method for move generation

**Move Representation**
- `Zug.java`: Move class handling algebraic notation conversion and promotion
- `MoveInfo.java`: Stores move metadata for undo functionality
- `Koordinaten.java`: Coordinate system wrapper

**Opening Book**
- `OpeningDictionary.java`: Loads opening moves from grandmaster games
- Data file: `openingdatabank/gm_games5moves.txt` (155KB of opening variations)

**Configuration & Utilities**
- `Config.java`: Configuration management for API tokens and file paths
- `MoveOrdering.java`: Move ordering for alpha-beta optimization  

### Key Architecture Patterns

**Board Representation**: 8x8 `Piece[][]` array with coordinate system where (0,0) is top-left (a8) and (7,7) is bottom-right (h1)

**Move Generation**: Each piece type implements its own move generation logic. Pseudo-legal moves are generated first, then filtered for legality by testing if the king would be in check.

**Search Algorithm**: 
- Negamax with alpha-beta pruning
- Quiescence search for tactical positions  
- Iterative deepening (currently depth 1-4)
- Move ordering for better pruning
- Transposition table structure exists but is commented out

**Lichess Integration**: Event-driven architecture using HTTP streaming API with automatic challenge acceptance and real-time game state processing.

## Important File Locations

- **Main executable**: `src/LichessBotStream.java`
- **Engine core**: `src/MoveFinder.java` 
- **Opening book**: `openingdatabank/gm_games5moves.txt`
- **Dependencies**: JSON jar referenced in `Schach2.iml`

## Development Notes

**Configuration**: The bot uses a flexible configuration system:
- API tokens and file paths are loaded from `bot.properties` or environment variables
- No hardcoded secrets in the codebase
- `bot.properties` is gitignored for security

**German Language**: Code contains German comments and variable names (e.g., `brett` = board, `Koenig` = King, `Laeufer` = Bishop).

**Threading**: Uses `CompletableFuture` for async HTTP requests to Lichess API.

**Move Format**: Uses algebraic notation (e.g., "e2e4", "e7e8q" for promotion).

## Getting Started

1. **Prerequisites**: Ensure Java 21 is installed and JSON dependency is available
2. **Setup**: Run `./setup.ps1` to create configuration files
3. **Token**: Get your Lichess bot token from [Lichess OAuth](https://lichess.org/account/oauth/token/create?scopes[]=bot:play&description=My+Bot)
4. **Configure**: Either:
   - Edit `bot.properties` and add your token
   - Or set environment variable: `$env:LICHESS_BOT_TOKEN = "your_token"`
5. **Run**: Execute `java -cp "out;path/to/json-20240303.jar" LichessBotStream`
6. **Test**: Challenge the bot on Lichess to test functionality

The bot will automatically accept challenges and play using the opening book initially, then switch to engine moves when out of book.
