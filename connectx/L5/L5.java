package connectx.L5;

// MiniMax + AlphaBeta Pruning + Transposition Table
// + Move Explorator Order + Iterative Deepening

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.CXBoard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.Random;

public class L5 implements CXPlayer {
    private int TIMEOUT;
    private long START;
    private CXGameState myWin;
    private CXGameState yourWin;
    int rows;
    int columns;
    int symbols;
    CXCellState player;
    CXCellState opponent;
    Integer[] moveOrder;
    private long[][][] zobristKeys; // Matrice per memorizzare i valori Zobrist delle celle
    private Map<Long, Score_Depth> transpositionTable;
    boolean timeisrunningout;
    int transpositionTableHits;
    int transpositionTableMisses;

    boolean am_i_first;

    public L5() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rows = M;
        this.columns = N;
        this.symbols = K;
        am_i_first = first;
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
        this.moveOrder = new Integer[50];
        // invece di controllare le mosse possibili da 1 a n, controllo prima
        // le colonne centrali per ottimizzare i cutoff di alpha-beta
        // (solitamente le mosse centrali sono migliori, quindi quelle laterali vengono
        // tagliate)
        // moveOrder[0] -> N/2, moveOrder[1] -> N/2 -1, moveOrder[2] -> N/2 +1, ...
        // PS ottimizzabile per il futuro modificando l'array in base alla situazione
        for (int i = 0; i < this.columns; i++)
            this.moveOrder[i] = this.columns / 2 + (1 - 2 * (i % 2)) * (i + 1) / 2;
        Random random = new Random(System.currentTimeMillis());
        this.zobristKeys = new long[this.rows][this.columns][3];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                for (int k = 0; k < 3; k++) {
                    this.zobristKeys[i][j][k] = random.nextLong();
                }
            }
        }
        this.transpositionTable = new HashMap<>();
        this.timeisrunningout = false;
        this.transpositionTableHits = 0;
        this.transpositionTableMisses = 0;
    }

    private void checktime() throws TimeoutException {

        if ((System.currentTimeMillis() - this.START) / 1000.0 >= this.TIMEOUT * (99.0 / 100.0)) {
            timeisrunningout = true;
            throw new TimeoutException();
        }
    }

    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();
        Random random = new Random(System.currentTimeMillis()); // emergency move
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int depth = am_i_first ? 0 : 1;
        // iterative deepening: controllo prima a profondità 1,
        // poi a profondità 2, ... finchè non finisce il tempo
        int currentBestMove = -1;
        timeisrunningout = false;
        // temporaneo per testing
        if (B.numOfMarkedCells() < 7)
            return this.columns / 2;

        try {
            while (!timeisrunningout) {
                bestMove = currentBestMove;
                // System.err.println("bestMove: " + bestMove);
                depth += 2;
                // System.err.println("depth: " + depth);
                if (B.numOfMarkedCells() == 0) {
                    // se sono il primo a muovere muovo al centro,
                    // ma uso comunque il tempo per esplorare
                    // e riempire la transposition table,
                    // esplorando solo il ramo della mossa centrale che farò
                    B.markColumn(this.columns / 2);
                    alphaBetaMin(B, alpha, beta, depth);
                    B.unmarkColumn();
                    currentBestMove = this.columns / 2;
                } else {
                    for (int i = 0; i < this.columns; i++) {
                        if (!B.fullColumn(this.moveOrder[i])) {
                            B.markColumn(this.moveOrder[i]);
                            int score = alphaBetaMin(B, alpha, beta, depth);
                            B.unmarkColumn();
                            // System.err.println("Move: " + this.moveOrder[i]);
                            System.err.println("Score: " + score);
                            // System.err.println("Alpha: " + alpha);
                            if (score > alpha) {
                                alpha = score;
                                currentBestMove = this.moveOrder[i];
                                // System.err.println("Move changed: ");
                            }
                        }
                    }
                }
            }
            return bestMove;
        } catch (TimeoutException e) {
            System.err.println("Depth reached: " + (depth));
            System.err.println("Transposition table hits: " + transpositionTableHits);
            System.err.println("Transposition table misses: " + transpositionTableMisses);
            // se non ho trovato una mossa valida, gioco random
            while (bestMove == -1 || B.fullColumn(bestMove))
                bestMove = random.nextInt(this.columns);

            // System.err.println("bestMove: " + bestMove);
            return bestMove;
        }
    }

    int alphaBetaMax(CXBoard B, int alpha, int beta, int depthleft) throws TimeoutException {
        checktime();
        // se lo score della mossa è nella transposition table lo uso,
        // altrimenti lo valuto e lo salvo nella transposition table
        int transpositionScore = lookupTranspositionTable(B, depthleft);
        if (transpositionScore != Integer.MIN_VALUE) {
            transpositionTableHits++;
            return transpositionScore;
        }
        transpositionTableMisses++;
        if (depthleft == 0 || B.gameState() != CXGameState.OPEN)
            return evaluate(B);
        for (int i = 0; i < this.columns; i++) {
            if (!B.fullColumn(this.moveOrder[i])) {
                B.markColumn(this.moveOrder[i]);
                int score = alphaBetaMin(B, alpha, beta, depthleft - 1);
                B.unmarkColumn();
                if (score >= beta) {
                    storeTranspositionTable(B, beta, depthleft);
                    return beta; // cutoff
                }
                if (score > alpha)
                    alpha = score;
            }
        }
        storeTranspositionTable(B, alpha, depthleft);
        return alpha;
    }

    int alphaBetaMin(CXBoard B, int alpha, int beta, int depthleft) throws TimeoutException {
        checktime();
        if (depthleft == 0 || B.gameState() != CXGameState.OPEN)
            return evaluate(B);
        for (int i = 0; i < this.columns; i++) {
            if (!B.fullColumn(this.moveOrder[i])) {
                B.markColumn(this.moveOrder[i]);
                int score = alphaBetaMax(B, alpha, beta, depthleft - 1);
                B.unmarkColumn();
                if (score <= alpha) {
                    return alpha; // cutoff
                }
                if (score < beta)
                    beta = score;
            }
        }
        return beta;
    }

    private int evaluate(CXBoard board) {
        if (board.gameState() == this.myWin)
            return Integer.MAX_VALUE;
        else if (board.gameState() == this.yourWin)
            return Integer.MIN_VALUE + 1;
        else {
            // something
            return 0;
        }
    }

    private long computeHash(CXBoard board) {
        long hash = 0;
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                CXCellState cellState = board.cellState(i, j);
                // ordinal ritorna l'indice dell'enum (P1 -> 0, P2 -> 1, FREE -> 2)
                int cellStateIndex = cellState.ordinal();
                // Zobrist hashing (a ^= b -> a = a XOR b)
                hash ^= zobristKeys[i][j][cellStateIndex];
            }
        }
        return hash;
    }

    private int lookupTranspositionTable(CXBoard board, int depth) {
        long hash = computeHash(board);
        // se l'hash della board attuale è nella table e è calcolato
        // a una profondità maggiore o uguale a quella attuale, lo ritorno
        if (transpositionTable.containsKey(hash) && transpositionTable.get(hash).getDepth() >= depth) {
            return transpositionTable.get(hash).getScore();
        }
        // valore che indica che la posizione non è presente nella tabella
        return Integer.MIN_VALUE;
    }

    private void storeTranspositionTable(CXBoard board, int score, int depth) {
        // se il tempo sta per scadere non aggiorno la transposition table
        // perchè il valore che è stato calcolato potrebbe essere inesatto
        if (timeisrunningout)
            return;
        long hash = computeHash(board);
        Score_Depth sd = new Score_Depth(score, depth);
        transpositionTable.put(hash, sd);
    }

    public String playerName() {
        return "L5";
    }
}
