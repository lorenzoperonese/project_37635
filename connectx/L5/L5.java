package connectx.L5;

// MiniMax + AlphaBeta Pruning + Transposition Table
// + Move Explorator Order + Iterative Deepening

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.L5.Score_Depth;

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
    boolean amIfirst;
    boolean timeIsRunningOut;

    public L5() {
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
            this.moveOrder[i]= this.columns/2 + (1-2*(i%2))*(i+1)/2; // 0 -> N/2, 1 -> N/2 -1, 2 -> N/2 +1
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
        this.amIfirst = first;
    }

    private void checktime() throws TimeoutException {

        if ((System.currentTimeMillis() - this.START) / 1000.0 >= this.TIMEOUT * (99.0 / 100.0)) {
            // System.err.println("time");
            timeIsRunningOut = true;
            throw new TimeoutException();
        }

    }

    public int selectColumn(CXBoard B) {
        this.timeIsRunningOut = false;
        this.START = System.currentTimeMillis();
        int depth = amIfirst ? 0 : 1;
        int bestMove = -1;
        int currentBestMove = -1;
        try {
            while(depth < this.columns * this.rows) {
                depth +=2;
                currentBestMove = alphaBetaSearch(B, depth);
                if(currentBestMove != -1)
                    bestMove = currentBestMove;
            }
        } catch (TimeoutException e) {
            // System.out.println("Depth reached: " + depth);
            // System.err.println("change");
            return bestMove;
        }
        //System.err.println("change");
        return bestMove;
    }

    private int alphaBetaSearch(CXBoard B, int depth) throws TimeoutException {
        Random random = new Random();
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        CXBoard newBoard = B.copy();
        for (int i = 0; i < this.columns; i++) {
            if (!B.fullColumn(this.moveOrder[i])) {
                newBoard.markColumn(this.moveOrder[i]);
                int score = alphaBetaMin(newBoard, alpha, beta, depth);
                //System.err.println(this.moveOrder[i] + " " + score);
                newBoard.unmarkColumn();
                if (score > alpha) {
                    alpha = score;
                    bestMove = moveOrder[i];
                }
            }
        }
        //System.err.println("Best: " + bestMove + " " + alpha);
        return bestMove;
    }

    int alphaBetaMax(CXBoard B, int alpha, int beta, int depthleft) throws TimeoutException {
        checktime();
        int transpositionScore = lookupTranspositionTable(B, depthleft);
        if (transpositionScore != Integer.MIN_VALUE)
            return transpositionScore;
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
        connectx.L5.Score_Depth sd = new Score_Depth(score, depth);
        transpositionTable.put(hash, sd);
    }

    public String playerName() {
        return "L5";
    }
}
