package connectx.L2;

// MiniMax + AlphaBeta Pruning

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.CXBoard;

import java.util.concurrent.TimeoutException;

public class L2 implements CXPlayer {
    private int TIMEOUT;
    private long START;
    private CXGameState myWin;
    private CXGameState yourWin;
    int rows;
    int columns;
    int symbols;
    CXCellState player;
    CXCellState opponent;

    public L2() {
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
    }

    private void checktime() throws TimeoutException {

        if ((System.currentTimeMillis() - this.START) / 1000.0 >= this.TIMEOUT * (99.0 / 100.0)) {
            throw new TimeoutException();
        }

    }

    public int selectColumn(CXBoard B) {
        this.START = System.currentTimeMillis();
        // idea: all'inizio prendere il centro
        if(B.numOfMarkedCells() == 0)
            return B.N/2;
        int bestMove = -1;
        int alpha = Integer.MIN_VALUE +1;
        int beta = Integer.MAX_VALUE;
        Integer[] L = B.getAvailableColumns();
        int depth = 1000000;
        for (int i = 0; i < L.length; i++) {
            B.markColumn(L[i]);
            int score = alphaBetaMin(B, alpha, beta, depth);
            B.unmarkColumn();
            if (score > alpha) {
                alpha = score;
                bestMove = L[i];
            }
        }
        return bestMove;
    }

    int alphaBetaMax(CXBoard B, int alpha, int beta, int depthleft ) {
        try {
            checktime();
            if (depthleft == 0 || B.gameState() != CXGameState.OPEN) return evaluate(B);
            Integer[] L = B.getAvailableColumns();
            for (int i = 0; i < L.length; i++) {
                B.markColumn(L[i]);
                int score = alphaBetaMin(B, alpha, beta, depthleft - 1);
                B.unmarkColumn();
                if (score >= beta)
                    return beta;   // cutoff
                if (score > alpha)
                    alpha = Math.max(alpha, score);
            }
            return alpha;
        } catch (TimeoutException e) {
            return evaluate(B);
        }
    }

    int alphaBetaMin(CXBoard B, int alpha, int beta, int depthleft ) {
        try {
            checktime();
            if (depthleft == 0 || B.gameState() != CXGameState.OPEN) return evaluate(B);
            Integer[] L = B.getAvailableColumns();
            for (int i = 0; i < L.length; i++) {
                B.markColumn(L[i]);
                int score = alphaBetaMax(B, alpha, beta, depthleft - 1);
                B.unmarkColumn();
                if (score <= alpha)
                    return alpha; // cutoff
                if (score < beta)
                    beta = Math.min(beta, score);
            }
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




    public String playerName() {
        return "L2";
    }
}
