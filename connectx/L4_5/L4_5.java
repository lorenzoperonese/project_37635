package connectx.L4_5;

// MiniMax + AlphaBeta Pruning + Transposition Table + Move Explorator Order

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.CXBoard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.Random;

public class L4_5 implements CXPlayer {
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
    private Map<Long, Integer> transpositionTable;


    public L4_5() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rows = M;
        this.columns = N;
        this.symbols = K;
        if(first) {
            this.myWin = CXGameState.WINP1;
            this.yourWin = CXGameState.WINP2;
            this.player = CXCellState.P1;
            this.opponent = CXCellState.P2;
        }
        else {
            this.myWin = CXGameState.WINP2;
            this.yourWin = CXGameState.WINP1;
            this.player = CXCellState.P2;
            this.opponent = CXCellState.P1;
        }
        this.TIMEOUT = timeout_in_secs;
        this.moveOrder = new Integer[100];
        for(int i=0;i<this.columns;i++)
            this.moveOrder[i]= this.columns/2 + (1-2*(i%2))*(i+1)/2 -1; // 0 -> N/2, 1 -> N/2 -1, 2 -> N/2 +1
        Random random = new Random();
        this.zobristKeys = new long[this.rows][this.columns][3];
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < this.columns; j++) {
                for (int k = 0; k < 3; k++) {
                    this.zobristKeys[i][j][k] = random.nextLong();
                }
            }
        }
        this.transpositionTable = new HashMap<>();
    }

    private void checktime() throws TimeoutException {

        if ((System.currentTimeMillis() - this.START) / 1000.0 >= this.TIMEOUT * (99.0 / 100.0)) {
            System.err.println("Time");
            throw new TimeoutException();
        }

    }

    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();
        Random random = new Random();
        // idea: all'inizio prendere il centro, ma riempire comunque la transposition table
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE +1;
        int beta = Integer.MAX_VALUE;
        int depth = 100000;
        for (int i = 0; i < this.columns; i++) {
            if(!B.fullColumn(this.moveOrder[i])) {
                B.markColumn(this.moveOrder[i]);
                int score = alphaBetaMin(B, alpha, beta, depth);
                B.unmarkColumn();
                if (score > alpha) {
                    alpha = score;
                    bestMove = moveOrder[i];
                }
            }
        }
        if(B.numOfMarkedCells() == 0)
            bestMove = this.columns/2;
        while(bestMove == -1 || B.fullColumn(bestMove))
            bestMove = random.nextInt() % this.columns;
        return bestMove;
    }

    int alphaBetaMax(CXBoard B, int alpha, int beta, int depthleft) {
        try {
            checktime();
            int transpositionScore = lookupTranspositionTable(B);
            if (transpositionScore != Integer.MIN_VALUE)
                return transpositionScore;
            if (depthleft == 0 || B.gameState() != CXGameState.OPEN) return evaluate(B);
            for (int i = 0; i < this.columns; i++) {
                if(!B.fullColumn(this.moveOrder[i])) {
                    B.markColumn(this.moveOrder[i]);
                    int score = alphaBetaMin(B, alpha, beta, depthleft - 1);
                    B.unmarkColumn();
                    if (score >= beta)
                        return beta;   // cutoff
                    if (score > alpha)
                        alpha = score;
                }
            }
            storeTranspositionTable(B, alpha);
            return alpha;
        } catch (TimeoutException e) {
            return evaluate(B);
        }
    }

    int alphaBetaMin(CXBoard B, int alpha, int beta, int depthleft) {
        try {
            checktime();
            int transpositionScore = lookupTranspositionTable(B);
            if (transpositionScore != Integer.MIN_VALUE)
                return transpositionScore;
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
            storeTranspositionTable(B, beta);
            return beta;
        } catch (TimeoutException e) {
            return evaluate(B);
        }
    }

    private int evaluate(CXBoard board) {
        if (board.gameState() == this.myWin)
            return Integer.MAX_VALUE;
        else if (board.gameState() == this.yourWin)
            return Integer.MIN_VALUE;
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
                int cellStateIndex = cellState.ordinal(); // ordinal ritorna l'indice dell'enum (P1 -> 0, P2 -> 1, FREE -> 2)
                hash ^= zobristKeys[i][j][cellStateIndex]; // Zobrist hash (^= -> Xor)
            }
        }
        return hash;
    }

    private int lookupTranspositionTable(CXBoard board) {
        long hash = computeHash(board);
        if (transpositionTable.containsKey(hash)) {
            return transpositionTable.get(hash);
        }
        return Integer.MIN_VALUE; // Valore che indica che la posizione non è presente nella tabella
    }

    private void storeTranspositionTable(CXBoard board, int score) {
        long hash = computeHash(board);
        transpositionTable.put(hash, score);
    }

    public String playerName() {
        return "L4_5";
    }
}
