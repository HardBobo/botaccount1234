class TTEntry {
    int depth;
    int value;
    int flag; // 0 = EXACT, 1 = LOWERBOUND, 2 = UPPERBOUND
    boolean isValid;

    TTEntry(int value, int depth, int flag) {
        this.value = value;
        this.depth = depth;
        this.flag = flag;
        this.isValid = true;
    }
}