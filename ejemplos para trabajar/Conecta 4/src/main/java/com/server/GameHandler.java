package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import com.shared.ClientData;
import com.shared.ClientPerfil;
import com.shared.Partida;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameHandler {

    private final Map<String, ClientData> clientsData;
    private final ClientRegistry clients;
    private final Main Main;
    private final List<Partida> partidas;
    private final List<ClientPerfil> perfils;
    private final ServerUtils serverUtils;

    public GameHandler(Map<String, ClientData> clientsData, ClientRegistry clients, List<Partida> partidas,
                       List<ClientPerfil> perfils, Main Main, ServerUtils serverUtils) {
        this.clientsData = clientsData;
        this.clients = clients;
        this.partidas = partidas;
        this.perfils = perfils;
        this.Main = Main;
        this.serverUtils = serverUtils;
    }

    public void crearPartida(WebSocket jugador1, WebSocket jugador2) {
        String nom1 = clients.nameBySocket(jugador1);
        String nom2 = clients.nameBySocket(jugador2);

        partidas.removeIf(p -> 
            Objects.equals(p.jugador1, jugador1) || 
            Objects.equals(p.jugador2, jugador1) ||
            Objects.equals(p.jugador1, jugador2) || 
            Objects.equals(p.jugador2, jugador2)
        );

        ClientPerfil perf1 = perfils.stream().filter(p -> p.name.equals(nom1)).findFirst().orElse(null);
        ClientPerfil perf2 = perfils.stream().filter(p -> p.name.equals(nom2)).findFirst().orElse(null);
        if (perf1 == null || perf2 == null) return;

        ClientData data1 = clientsData.get(nom1);
        ClientData data2 = clientsData.get(nom2);

        data1.color = Missatges.RED;
        data2.color = Missatges.YELLOW;

        Partida nueva = new Partida(jugador1, perf1, jugador2, perf2);
        partidas.add(nueva);
        Main.initializegameObjects(nueva);

        perf1.estat = Missatges.ESTAT_EN_PARTIDA;
        perf2.estat = Missatges.ESTAT_EN_PARTIDA;

        JSONArray arrPerfils = new JSONArray();
        for (ClientPerfil perf : perfils) arrPerfils.put(perf.toJSON());

        JSONObject msg = new JSONObject();
        msg.put(Missatges.K_TYPE, Missatges.LLISTA_PERFILS);
        msg.put(Missatges.K_VALUE, arrPerfils);
        Main.broadcast(msg.toString());

        nueva.currentPlayer = 1;

        JSONObject emparellar = new JSONObject();
        emparellar.put(Missatges.K_TYPE, Missatges.EMPARELLAT);
        emparellar.put(Missatges.JUGADOR_1, nom1);
        emparellar.put(Missatges.JUGADOR_2, nom2);

        serverUtils.sendSafe(jugador1, emparellar.toString());
        serverUtils.sendSafe(jugador2, emparellar.toString());

        Main.sendCountdown(jugador1, jugador2);
    }

    public void handleClientMouseMoving(WebSocket conn, JSONObject obj) {
        String clientName = clients.nameBySocket(conn);
        clientsData.put(clientName, ClientData.fromJSON(obj.getJSONObject(Missatges.K_VALUE)));
    }


    

    // public void handleColocarFitxa(WebSocket conn, JSONObject obj, Partida partida) {
    //     if (partida == null) return;

    //     String nom = obj.getString(Missatges.NAME);
    //     String color = obj.getString(Missatges.COLOR);
    //     int col = obj.getInt(Missatges.COL);
    //     String id = obj.getString(Missatges.ID);

    //     ClientData jugador = clientsData.get(nom);
    //     if (jugador == null) return;

    //     String turnoColor = (partida.currentPlayer == 1) ? Missatges.RED : Missatges.YELLOW;
    //     if (!jugador.color.equalsIgnoreCase(turnoColor)) return;

    //     GameObject piece = partida.gameObjects.get(id);
    //     if (piece == null || !piece.isPiece || !piece.color.equalsIgnoreCase(jugador.color)) return;

    //     int row = serverUtils.findLowestEmptyRow(partida.board, col);
    //     if (row < 0) return;

    //     String valor = color.equalsIgnoreCase(Missatges.RED) ? "R" : "Y";
    //     partida.board[row][col] = valor;
    //     piece.isPiece = false;

    //     JSONObject fitxaColocada = new JSONObject();
    //     fitxaColocada.put(Missatges.K_TYPE, Missatges.FITXA_COLOCADA);
    //     fitxaColocada.put(Missatges.ROW, row);
    //     fitxaColocada.put(Missatges.COL, col);
    //     fitxaColocada.put(Missatges.K_VALUE, valor);
    //     fitxaColocada.put(Missatges.ID, id);

    //     serverUtils.sendSafe(partida.jugador1, fitxaColocada.toString());
    //     serverUtils.sendSafe(partida.jugador2, fitxaColocada.toString());

    //     partida.lastRow = row;
    //     partida.lastCol = col;

    //     List<int[]> guanyador = serverUtils.comprobarGuanyador(partida.board, row, col, valor);
    //     if (guanyador != null) {
    //         if (guanyador.size() == 1 && guanyador.get(0)[0] == -1) {
    //             partida.winnerName = "EMPAT";
    //         } else {
    //             partida.winnerName = nom;
    //         }

    //         JSONObject fiPartida = new JSONObject();
    //         fiPartida.put(Missatges.K_TYPE, Missatges.PARTIDA_FINALITZADA);
    //         fiPartida.put("guanyador", partida.winnerName);
    //         fiPartida.put(Missatges.COLOR, color);

    //         JSONArray coordsArray = new JSONArray();
    //         if (!partida.winnerName.equals("EMPAT")) {
    //             for (int[] pos : guanyador) {
    //                 coordsArray.put(new JSONArray(pos));
    //             }
    //         }
    //         fiPartida.put(Missatges.WINNING_CELLS, coordsArray);

    //         serverUtils.sendSafe(partida.jugador1, fiPartida.toString());
    //         serverUtils.sendSafe(partida.jugador2, fiPartida.toString());
    //         serverUtils.resetearEstatJugadors(partida);
    //     }

    //     partida.currentPlayer = (partida.currentPlayer == 1) ? 2 : 1;
    //     serverUtils.broadcastStatus(partida);
    // }
}
