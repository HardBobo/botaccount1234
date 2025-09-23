class TTEntry {
    int depth;
    int value;
    int flag; // 0 = EXACT, 1 = LOWERBOUND, 2 = UPPERBOUND
    boolean isValid;
    Zug bestMove; // Best move found for this position (may be null)

    // Backward-compatible constructor (no bestMove)
    TTEntry(int value, int depth, int flag) {
        this.value = value;
        this.depth = depth;
        this.flag = flag;
        this.isValid = true;
        this.bestMove = null;
    }

    // Constructor with bestMove
    TTEntry(int value, int depth, int flag, Zug bestMove) {
        this.value = value;
        this.depth = depth;
        this.flag = flag;
        this.isValid = true;
        this.bestMove = bestMove;
    }
}
