package com.server;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

import com.shared.ClientData;
import com.shared.ClientPerfil;
import com.shared.GameObject;
import com.shared.Partida;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public class ServerUtils {
    

    private final Main server;
    private final ClientRegistry clients;
    private final Map<String, ClientData> clientsData;
    private final List<ClientPerfil> perfils;
    private final List<Partida> partidas;
    public ServerUtils(Main server,ClientRegistry clients, Map<String,ClientData> clientsData,List<ClientPerfil> perfils,List<Partida> partidas) {
        this.server = server;
        this.clients = clients;
        this.clientsData = clientsData;
        this.perfils = perfils;
        this.partidas = partidas;
    }

    
    /** Envia de forma segura un payload i, si el socket no està connectat, el neteja del registre. */
    public void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            clientsData.remove(name);
            System.out.println("Client desconnectat durant send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadcastStatus(Partida p) {
            if (p == null) return;

            JSONArray arrClients = new JSONArray();
            String name1 = clients.nameBySocket(p.jugador1);
            String name2 = clients.nameBySocket(p.jugador2);
            if (name1 != null) {
                ClientData cd = clientsData.get(name1);
                if (cd != null) arrClients.put(cd.toJSON());
            }
            if (name2 != null) {
                ClientData cd = clientsData.get(name2);
                if (cd != null) arrClients.put(cd.toJSON());
            }

            JSONArray arrObjects = new JSONArray();
            for (GameObject obj : p.gameObjects.values()) {
                arrObjects.put(obj.toJSON());
            }

            JSONObject gameState = new JSONObject();
            gameState.put("status", p.winnerName.isEmpty() ? "playing" : "finished");
            gameState.put("board", p.board);
            String turnoNombre = (p.currentPlayer == 1) ? clients.nameBySocket(p.jugador1) : clients.nameBySocket(p.jugador2);
            gameState.put("turn", turnoNombre == null ? "" : turnoNombre);
            gameState.put("currentPlayer", p.currentPlayer);
            gameState.put("lastMove", new JSONObject().put("row", p.lastRow).put("col", p.lastCol));
            gameState.put("winner", p.winnerName);

            for (WebSocket conn : List.of(p.jugador1, p.jugador2)) {
                if (conn == null) continue;
                String name = clients.nameBySocket(conn);
                JSONObject serverData = new JSONObject();
                serverData.put("type", "serverData");
                serverData.put("partidaId", p.id);
                serverData.put("clientName", name);
                serverData.put("clientsList", arrClients);
                serverData.put("objectsList", arrObjects);
                serverData.put("game", gameState);
                sendSafe(conn, serverData.toString());
                
                System.out.println(serverData.toString());

            }
        }

        public Partida getPartidaByJugador(WebSocket jugador) {
            for (Partida p : partidas) {
                if (p.jugador1 == jugador || p.jugador2 == jugador) return p;
            }
            return null;
        }

            
        public void resetearEstatJugadors(Partida p) {
            if (p == null) return;
            if (p.jugador1 != null) {
                ClientPerfil perf1 = perfils.stream().filter(perf -> perf.name.equals(clients.nameBySocket(p.jugador1))).findFirst().orElse(null);
                if (perf1 != null) perf1.estat = "Disponible";
            }
            if (p.jugador2 != null) {
                ClientPerfil perf2 = perfils.stream().filter(perf -> perf.name.equals(clients.nameBySocket(p.jugador2))).findFirst().orElse(null);
                if (perf2 != null) perf2.estat = "Disponible";
            }

            JSONArray arrPerfils = new JSONArray();
            for (ClientPerfil perf : perfils) arrPerfils.put(perf.toJSON());

            JSONObject msg = new JSONObject();
            msg.put("type", "llistaPerfils");
            msg.put("value", arrPerfils);
            server.broadcast(msg.toString());
        }

        public int findLowestEmptyRow(String[][] board,int col) {
            if (col < 0 || col >= board[0].length) return -1;
            for (int row = board.length - 1; row >= 0; row--) {
                if (board[row][col].isEmpty()) {
                    return row;
                }
            }
            return -1;
        }


    private List<int[]> checkHorizontal(String[][] board, int row, int col, String value) {
        List<int[]> coords = new ArrayList<>();
        coords.add(new int[]{row, col});

        // derecha
        for (int c = col + 1; c < board[0].length && value.equals(board[row][c]); c++) coords.add(new int[]{row, c});
        // izquierda
        for (int c = col - 1; c >= 0 && value.equals(board[row][c]); c--) coords.add(new int[]{row, c});

        return coords.size() >= 4 ? coords : null;
    }

    private List<int[]> checkVertical(String[][] board, int row, int col, String value) {
        List<int[]> coords = new ArrayList<>();
        coords.add(new int[]{row, col});

        // abajo
        for (int r = row + 1; r < board.length && value.equals(board[r][col]); r++) coords.add(new int[]{r, col});
        // arriba
        for (int r = row - 1; r >= 0 && value.equals(board[r][col]); r--) coords.add(new int[]{r, col});

        return coords.size() >= 4 ? coords : null;
    }

     private List<int[]> checkDiagonal(String[][] board, int row, int col, String value) {
        List<int[]> coords;

         // diagonal principal (\)
        coords = new ArrayList<>();
        coords.add(new int[]{row, col});
        for (int r = row + 1, c = col + 1; r < board.length && c < board[0].length && value.equals(board[r][c]); r++, c++) coords.add(new int[]{r, c});
        for (int r = row - 1, c = col - 1; r >= 0 && c >= 0 && value.equals(board[r][c]); r--, c--) coords.add(new int[]{r, c});
        if (coords.size() >= 4) return coords;

            // diagonal secundaria (/)
            coords = new ArrayList<>();
            coords.add(new int[]{row, col});
            for (int r = row + 1, c = col - 1; r < board.length && c >= 0 && value.equals(board[r][c]); r++, c--) coords.add(new int[]{r, c});
            for (int r = row - 1, c = col + 1; r >= 0 && c < board[0].length && value.equals(board[r][c]); r--, c++) coords.add(new int[]{r, c});
            return coords.size() >= 4 ? coords : null;
        }


        public List<int[]> comprobarGuanyador(String[][] tauler, int fila, int columna, String valor) {
        List<int[]> celdasGanadoras;

        celdasGanadoras = checkHorizontal(tauler, fila, columna, valor);
        if (celdasGanadoras != null) return celdasGanadoras;

        celdasGanadoras = checkVertical(tauler, fila, columna, valor);
        if (celdasGanadoras != null) return celdasGanadoras;

        celdasGanadoras = checkDiagonal(tauler, fila, columna, valor);
        if (celdasGanadoras != null) return celdasGanadoras;

        // comprobar empate
        boolean empat = true;
        for (int col = 0; col < tauler[0].length; col++) {
            if (tauler[0][col].isEmpty()) {
                empat = false;
                break;
            }
        }

        if (empat) return List.of(new int[]{-1, -1}); // señal de empate

        return null; // nadie gana todavía
    }


    
}
