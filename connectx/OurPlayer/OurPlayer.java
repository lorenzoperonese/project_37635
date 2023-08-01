package connectx.OurPlayer;

import connectx.CXCellState;
import connectx.CXGameState;
import connectx.CXPlayer;
import connectx.CXBoard;

import javax.naming.LinkLoopException;
import java.sql.Time;
import java.util.concurrent.TimeoutException;

public class OurPlayer implements CXPlayer {
    private int TIMEOUT;
    private long START;
    private CXGameState myWin;
    private CXGameState yourWin;
    int rows;
    int columns;
    int symbols;
    CXCellState player;
    CXCellState opponent;

    public OurPlayer() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        this.rows = M;
        this.columns = N;
        this.symbols = K;
        if(first) {
            this.myWin = CXGameState.WINP1;
            this.yourWin = CXGameState.WINP2;
            this.player = CXCellState.P1;
        }
        else {
            this.myWin = CXGameState.WINP2;
            this.yourWin = CXGameState.WINP1;
            this.player = CXCellState.P2;
        }
        this.TIMEOUT = timeout_in_secs;
    }

    private void checktime() throws TimeoutException {

        //System.out.println((System.currentTimeMillis() - this.START) / 1000.0);
        if ((System.currentTimeMillis() - this.START) / 1000.0 >= this.TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    public int selectColumn(CXBoard B) {

        this.START = System.currentTimeMillis();
        int bestMove = -1;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        Integer[] L = B.getAvailableColumns();
        int depth = 10;
        for (int move : L) {
            CXBoard newBoard = B.copy();
            newBoard.markColumn(move);
            int score = minimax(newBoard, depth -1, alpha, beta);
            newBoard.unmarkColumn();

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int minimax(CXBoard B, int depth, int alpha, int beta) {
        try {
            checktime();
            if (depth == 0 || B.gameState() != CXGameState.OPEN) {
                return evaluate(B);
            }
            int currentPlayer = B.currentPlayer();
            Integer[] availableColumns = B.getAvailableColumns();

            if (currentPlayer == 0) {
                // Turno del giocatore MAX
                int bestScore = Integer.MIN_VALUE;
                for (int move : availableColumns) {
                    CXBoard newBoard = B.copy();
                    newBoard.markColumn(move);
                    int score = minimax(newBoard, depth - 1, alpha, beta);
                    bestScore = Math.max(bestScore, score);
                    alpha = Math.max(alpha, bestScore);

                    if (beta <= alpha) {
                        // Potatura Alpha-Beta
                        break;
                    }
                }
                return bestScore;
            } else {
                // Turno del giocatore MIN
                int bestScore = Integer.MAX_VALUE;
                for (int move : availableColumns) {
                    CXBoard newBoard = B.copy();
                    newBoard.markColumn(move);
                    int score = minimax(newBoard, depth - 1, alpha, beta);
                    bestScore = Math.min(bestScore, score);
                    beta = Math.min(beta, bestScore);

                    if (beta <= alpha) {
                        // Potatura Alpha-Beta
                        break;
                    }
                }
                return bestScore;
            }
        } catch (TimeoutException e){
            return evaluate(B);
        }
    }

    private int evaluate(CXBoard board) {
        int X = board.X; // Numero di simboli da allineare

        // Calcolo il numero di connessioni per il giocatore corrente
        int currentPlayerConnections = countConnections(board, this.player, X);

        // Calcolo il numero di connessioni per l'avversario
        int opponentConnections = countConnections(board, this.opponent, X);

        // Assegno un punteggio basato sulle connessioni del giocatore corrente meno le connessioni dell'avversario
        int score = currentPlayerConnections - opponentConnections;

        // Se il giocatore corrente ha vinto, gli assegno un punteggio molto alto
        if (board.gameState() == this.myWin)
            score = Integer.MAX_VALUE;
        else if (board.gameState() == this.yourWin)
            score = 0;

        return score;
    }

    // Funzione di utilità per calcolare il numero di connessioni per un giocatore dato
    private int countConnections(CXBoard board, CXCellState player, int X) {
        int M = board.M;
        int N = board.N;

        int connections = 0;

        // Controllo le righe
        for (int i = 0; i < M; i++) {
            for (int j = 0; j <= N - X; j++) {
                boolean isConnected = true;
                for (int k = 0; k < X; k++) {
                    if (board.cellState(i, j + k) != player) {
                        isConnected = false;
                        break;
                    }
                }
                if (isConnected) {
                    connections++;
                }
            }
        }

        // Controllo le colonne
        for (int i = 0; i < N; i++) {
            for (int j = 0; j <= M - X; j++) {
                boolean isConnected = true;
                for (int k = 0; k < X; k++) {
                    if (board.cellState(j + k, i) != player) {
                        isConnected = false;
                        break;
                    }
                }
                if (isConnected) {
                    connections++;
                }
            }
        }

        // Controllo le diagonali (verso l'alto)
        for (int i = 0; i <= M - X; i++) {
            for (int j = 0; j <= N - X; j++) {
                boolean isConnected = true;
                for (int k = 0; k < X; k++) {
                    if (board.cellState(i + k, j + k) != player) {
                        isConnected = false;
                        break;
                    }
                }
                if (isConnected) {
                    connections++;
                }
            }
        }

        // Controllo le diagonali (verso il basso)
        for (int i = X - 1; i < M; i++) {
            for (int j = 0; j <= N - X; j++) {
                boolean isConnected = true;
                for (int k = 0; k < X; k++) {
                    if (board.cellState(i - k, j + k) != player) {
                        isConnected = false;
                        break;
                    }
                }
                if (isConnected) {
                    connections++;
                }
            }
        }

        return connections;
    }


    public String playerName() {
        return "OurPlayer";
    }
}
