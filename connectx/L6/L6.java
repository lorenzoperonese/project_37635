package connectx.L6;

// MiniMax + AlphaBeta Pruning + Transposition Table
// + Move Explorator Order + Iterative Deepening + Heuristic

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.CXBoard;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;
import java.util.Random;

public class L6 implements CXPlayer {
    private int TIMEOUT;
    private long START;
    private CXGameState myWin;
    private CXGameState yourWin;
    int rows;
    int columns;
    int symbols;
    Random random;
    CXCellState player;
    CXCellState opponent;
    Integer[] moveOrder;
    private long[][][] zobristKeys; // Matrice per memorizzare i valori Zobrist delle celle
    private Map<Long, Score_Depth> transpositionTable;
    boolean amIfirst;
    boolean timeIsRunningOut;
    int[][] scoreMove;
    int depth;
    int transpositionTableHits;
    int transpositionTableMisses;
    boolean firstMove;
    int[] table;

    public L6() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rows = M;
        this.columns = N;
        this.symbols = K;
        if (first) {
            this.myWin = CXGameState.WINP1;
            this.yourWin = CXGameState.WINP2;
            this.player = CXCellState.P1;
            this.opponent = CXCellState.P2;
        } else {
            this.myWin = CXGameState.WINP2;
            this.yourWin = CXGameState.WINP1;
            this.player = CXCellState.P2;
            this.opponent = CXCellState.P1;
        }
        this.TIMEOUT = timeout_in_secs;
        this.moveOrder = new Integer[100];
        for (int i = 0; i < this.columns; i++)
            this.moveOrder[i] = this.columns / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2; // 0 -> N/2, 1 -> N/2 -1, 2 -> N/2 +1
        random = new Random();
        this.zobristKeys = new long[this.rows][this.columns][3];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                for (int k = 0; k < 3; k++) {
                    this.zobristKeys[i][j][k] = random.nextLong();
                }
            }
        }
        this.transpositionTable = new HashMap<>();
        this.amIfirst = first;
        this.scoreMove = new int[this.rows][this.columns];
        // per la simmetria, riempio prima 1/4, poi copio nel resto della matrice
        // il valore delle mosse è dato da -d^3/(M*N), dove d è la distanza dal centro + 1
        for (int i = 0; i <= this.rows / 2; i++) {
            for (int j = 0; j <= this.columns / 2; j++) {
                int distance = Math.abs(this.rows / 2 - i) + Math.abs(this.columns / 2 - j);
                int value = 0; //-(int) Math.pow(distance + 1, 3) / (this.rows * this.columns);
                this.scoreMove[i][j] = value;
                this.scoreMove[this.rows - 1 - i][j] = value;
                this.scoreMove[i][this.columns - 1 - j] = value;
                this.scoreMove[this.rows - 1 - i][this.columns - 1 - j] = value;
            }
        }
        // stampa dello score delle celle
        if (false) {
            for (int row = 0; row < this.rows; row++) {
                for (int col = 0; col < this.columns; col++) {
                    System.err.print(this.scoreMove[row][col] + "\t");
                }
                System.err.println();
            }
        }
        this.depth = 0;
        this.transpositionTableHits = 0;
        this.transpositionTableMisses = 0;
        this.firstMove = true;
    }

    private void checktime() throws TimeoutException {

        if ((System.currentTimeMillis() - this.START) / 1000.0 >= this.TIMEOUT * (95.0 / 100.0)) {
            // System.err.println("time");
            timeIsRunningOut = true;
            throw new TimeoutException();
        }

    }

    public int selectColumn(CXBoard B) {
        this.timeIsRunningOut = false;
        this.START = System.currentTimeMillis();
        int iterativeDepth;
        // se non è la prima mossa, inizio alla profondità a cui mi sono fermato prima (-2 per sicurezza)
        iterativeDepth = firstMove ? (amIfirst ? 0 : 1) : this.depth -2;
        int bestMove = -1;
        int currentBestMove = -1;
        try {
            while(iterativeDepth < this.columns * this.rows - B.numOfMarkedCells() +1) {
                iterativeDepth += 2;
                currentBestMove = alphaBetaSearch(B, iterativeDepth);
                if(currentBestMove != -1)
                    bestMove = currentBestMove;
            }
        } catch (TimeoutException e) {
            // System.err.println("Depth reached: " + (iterativeDepth -2));
            // System.err.println("change");
            // System.err.println("Transposition table hits: " + this.transpositionTableHits);
            // System.err.println("Transposition table misses: " + this.transpositionTableMisses);
            this.transpositionTableHits = 0;
            this.transpositionTableMisses = 0;
            if(firstMove) this.depth = iterativeDepth - 2;
            return bestMove;
        }
        //System.err.println("change");
        // System.err.println("Transposition table hits: " + this.transpositionTableHits);
        // System.err.println("Transposition table misses: " + this.transpositionTableMisses);
        this.transpositionTableHits = 0;
        this.transpositionTableMisses = 0;
        this.depth = iterativeDepth;
        return bestMove;
    }

    private int alphaBetaSearch(CXBoard B, int depth) throws TimeoutException {
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        CXBoard newBoard = B.copy();
        for (int i = 0; i < this.columns; i++) {
            if (!B.fullColumn(this.moveOrder[i])) {
                newBoard.markColumn(this.moveOrder[i]);
                int score = alphaBetaMin(newBoard, alpha, beta, depth);
                // System.err.println(this.moveOrder[i] + " " + score);
                newBoard.unmarkColumn();
                if (score > alpha) {
                    alpha = score;
                    bestMove = moveOrder[i];
                }
            }
        }
        // System.err.println("Best: " + bestMove + " " + alpha);
        return bestMove;
    }

    int alphaBetaMax(CXBoard B, int alpha, int beta, int depthleft) throws TimeoutException {
        checktime();
        int transpositionScore = lookupTranspositionTable(B, depthleft);
        if (transpositionScore != Integer.MIN_VALUE) {
            this.transpositionTableHits++;
            return transpositionScore;
        }
        this.transpositionTableMisses++;
        if (depthleft == 0 || B.gameState() != CXGameState.OPEN) return evaluate(B);
        for (int i = 0; i < this.columns; i++) {
            if (!B.fullColumn(this.moveOrder[i])) {
                B.markColumn(this.moveOrder[i]);
                int score = alphaBetaMin(B, alpha, beta, depthleft - 1);
                B.unmarkColumn();
                if (score >= beta)
                    return beta;   // cutoff
                if (score > alpha)
                    alpha = score;
            }
        }
        storeTranspositionTable(B, alpha, depthleft);
        return alpha;
    }

    int alphaBetaMin(CXBoard B, int alpha, int beta, int depthleft) throws TimeoutException {
        checktime();
        if (depthleft == 0 || B.gameState() != CXGameState.OPEN) return evaluate(B);
        for (int i = 0; i < this.columns; i++) {
            if(!B.fullColumn(this.moveOrder[i])) {
                B.markColumn(this.moveOrder[i]);
                int score = alphaBetaMax(B, alpha, beta, depthleft - 1);
                B.unmarkColumn();
                if (score <= alpha)
                    return alpha; // cutoff
                if (score < beta)
                    beta = score;
            }
        }
        return beta;
    }

    private int evaluate(CXBoard board) {
        int score = 0;
        if (board.gameState() == this.myWin)
            score = Integer.MAX_VALUE;
        else if (board.gameState() == this.yourWin)
            score = Integer.MIN_VALUE;
        else {
            // score mosse
            for (int i = 0; i < this.rows; i++)
                for (int j = 0; j < this.columns; j++) {
                    if (board.cellState(i, j) == this.player)
                        score += this.scoreMove[i][j];
                    else if (board.cellState(i, j) == this.opponent)
                        score -= this.scoreMove[i][j];
                }

            // score righe
            int emptyCellsW = 0;
            int streakW = 0;
            int emptyCellsL = 0;
            int streakL = 0;
            CXCellState cs;
            int j=0;
            int contPl = 0;
            int contOpp = 0;
            for(int i=0;i<this.rows;i++) {
                while(j+this.symbols <= this.columns) {
                    for(int k=0;k<this.symbols;k++) {
                        if(board.cellState(i, j+k) == this.player)
                            contPl++;
                        else if(board.cellState(i, j+k) == this.opponent)
                            contOpp++;
                    }
                    if(contPl == 0 && contOpp > 0)
                        score -= contOpp * contOpp;
                    else if(contPl > 0 && contOpp == 0)
                        score += contPl * contPl;
                    contPl = 0;
                    contOpp = 0;
                    j++;
                }
                j=0;
            }

            // score colonne
            for (j = 0; j < this.columns; j++) {
                int i = 0;
                while (board.cellState(i, j) == CXCellState.FREE && i < this.rows -1) {
                    emptyCellsW++;
                    i++;
                }
                if (i < this.rows) {
                    if (board.cellState(i, j) == this.player) {
                        while (board.cellState(i, j) == this.player && i < this.rows -1) {
                            streakW++;
                            i++;
                        }
                        if(board.cellState(i, j) == this.player)
                            streakW++;
                        if (streakW + emptyCellsW >= this.symbols)
                            score += streakW * streakW;
                    } else {
                        while (board.cellState(i, j) == this.opponent && i < this.rows -1) {
                            streakL++;
                            i++;
                        }
                        if(board.cellState(i, j) == this.opponent)
                            streakL++;
                        if (streakL + emptyCellsW >= this.symbols)
                            score -= streakL * streakL;
                    }
                }
                streakL = 0;
                streakW = 0;
                emptyCellsW = 0;
            }

            // score diagonali
            /*
            ----X
            ---X-
            --X--
            -X---
            X----
            [ left ]
             */
            for (int i = 0; i < this.rows; i++) {
                int r = i, c = 0;
                while (r >= this.symbols -1 && c < this.columns - this.symbols +1) {
                    for(int k=0;k<this.symbols;k++) {
                        if(board.cellState(r,c) == this.player)
                            contPl ++;
                        else if(board.cellState(r,c) == this.opponent)
                            contOpp ++;
                        r--;
                        c++;
                    }
                    if(contPl == 0 && contOpp > 0)
                        score -= contOpp * contOpp;
                    else if(contPl > 0 && contOpp == 0)
                        score += contPl * contPl;
                    contPl = 0;
                    contOpp = 0;
                    r = r + this.symbols - 1;
                    c = c - this.symbols + 1;
                }
            }
            // System.err.println("diagonal left score: " + score);
            /*
            ----X
            ---X-
            --X--
            -X---
            X----
            [ right ]
             */
            for (j = 1; j < this.columns; j++) {
                int r = this.rows - 1, c = j;
                while (r >= this.symbols -1 && c < this.columns - this.symbols +1) {
                    for(int k=0;k<this.symbols;k++) {
                        if(board.cellState(r,c) == this.player)
                            contPl ++;
                        else if(board.cellState(r,c) == this.opponent)
                            contOpp ++;
                        r--;
                        c++;
                    }
                    if(contPl == 0 && contOpp > 0)
                        score -= contOpp * contOpp;
                    else if(contPl > 0 && contOpp == 0)
                        score += contPl * contPl;
                    contPl = 0;
                    contOpp = 0;
                    r = r + this.symbols - 1;
                    c = c - this.symbols + 1;
                }
            }
            // System.err.println("diagonal right score: " + score);

            // score anti-diagonali
            /*
            X----
            -X---
            --X--
            ---X-
            ----X
            [ right ]
             */
            for (int i = 0; i < this.rows; i++) {
                int r = i, c = this.columns - 1;
                while (r >= this.symbols -1 && c >= this.symbols -1) {
                    for(int k=0;k<this.symbols;k++) {
                        if(board.cellState(r,c) == this.player)
                            contPl ++;
                        else if(board.cellState(r,c) == this.opponent)
                            contOpp ++;
                        r--;
                        c--;
                    }
                    if(contPl == 0 && contOpp > 0)
                        score -= contOpp * contOpp;
                    else if(contPl > 0 && contOpp == 0)
                        score += contPl * contPl;
                    contPl = 0;
                    contOpp = 0;
                    r = r + this.symbols - 1;
                    c = c + this.symbols - 1;
                }
            }
            // System.err.println("anti-diagonal right score: " + score);
            /*
            X----
            -X---
            --X--
            ---X-
            ----X
            [ left ]
            */
            for (j = 0; j <this.columns -1; j++) {
                int r = this.rows - 1, c = j;
                while (r >= this.symbols -1 && c >= this.symbols -1) {
                    for(int k=0;k<this.symbols;k++) {
                        if(board.cellState(r,c) == this.player)
                            contPl ++;
                        else if(board.cellState(r,c) == this.opponent)
                            contOpp ++;
                        r--;
                        c--;
                    }
                    if(contPl == 0 && contOpp > 0)
                        score -= contOpp * contOpp;
                    else if(contPl > 0 && contOpp == 0)
                        score += contPl * contPl;
                    contPl = 0;
                    contOpp = 0;
                    r = r + this.symbols - 1;
                    c = c + this.symbols - 1;
                }
            }
        }
        // System.err.println("anti-diagonal left score: " + score);
        return score;
    }

    private long computeHash(CXBoard board) {
        long hash = 0;
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                CXCellState cellState = board.cellState(i, j);
                int cellStateIndex = cellState.ordinal(); // ordinal ritorna l'indice dell'enum (P1 -> 0, P2 -> 1, FREE -> 2)
                hash ^= zobristKeys[i][j][cellStateIndex]; // Zobrist hash (^= -> Xor)
            }
        }
        return hash;
    }

    private int lookupTranspositionTable(CXBoard board, int depth) {
        long hash = computeHash(board);
        // se l'hash della board attuale è nella table e è calcolato a una profondità
        // maggiore o uguale a quella attuale oppure so che la mossa è vincente, lo ritorno
        if (transpositionTable.containsKey(hash) && (transpositionTable.get(hash).getDepth() >= depth || transpositionTable.get(hash).getScore() == Integer.MAX_VALUE))
            return transpositionTable.get(hash).getScore();
        // valore che indica che la posizione non è presente nella tabella
        return Integer.MIN_VALUE;
    }

    private void storeTranspositionTable(CXBoard board, int score, int depth) {
        // se il tempo sta per scadere non aggiorno la transposition table
        // perchè il valore che è stato calcolato potrebbe essere inesatto
        if (timeIsRunningOut)
            return;
        long hash = computeHash(board);
        Score_Depth sd = new Score_Depth(score, depth);
        transpositionTable.put(hash, sd);
    }

    public String playerName() {
        return "L6";
    }
}
