package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.shared.ClientData;
import com.shared.ClientPerfil;
import com.shared.GameObject;
import com.shared.Partida;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GameHandler { // Plantilla

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

    /** Crea una partida entre dos jugadores */
    public void crearPartida(WebSocket jugador1, WebSocket jugador2) {
        String nom1 = clients.nameBySocket(jugador1);
        String nom2 = clients.nameBySocket(jugador2);

        // Elimina partidas existentes de esos jugadores
        partidas.removeIf(p -> Objects.equals(p.jugador1, jugador1) ||
                                Objects.equals(p.jugador2, jugador1) ||
                                Objects.equals(p.jugador1, jugador2) ||
                                Objects.equals(p.jugador2, jugador2));

        ClientPerfil perf1 = perfils.stream().filter(p -> p.name.equals(nom1)).findFirst().orElse(null);
        ClientPerfil perf2 = perfils.stream().filter(p -> p.name.equals(nom2)).findFirst().orElse(null);
        if (perf1 == null || perf2 == null) return;

        ClientData data1 = clientsData.get(nom1);
        ClientData data2 = clientsData.get(nom2);

        // Asignar colores a los jugadores
        data1.color = Missatges.RED;
        data2.color = Missatges.YELLOW;

        // Crear partida
        Partida nueva = new Partida(jugador1, perf1, jugador2, perf2);
        partidas.add(nueva);

        // Inicializar palas y bola
        nueva.gameObjects.put("paddleLeft", new GameObject("paddleLeft", 50, 200, 10, 100, 0));
        nueva.gameObjects.put("paddleRight", new GameObject("paddleRight", 1140, 200, 10, 100, 0));
        nueva.gameObjects.put("ball", new GameObject("ball", 600, 300, 20, 20, 0));

        perf1.estat = Missatges.ESTAT_EN_PARTIDA;
        perf2.estat = Missatges.ESTAT_EN_PARTIDA;

        Main.sendCountdown(jugador1, jugador2);
    }

    /** Actualiza la posición de la pala de un jugador */
    public void handlePaddleMove(WebSocket conn, JSONObject obj, Partida partida) {
        if (partida == null) return;

        String playerName = clients.nameBySocket(conn);
        ClientData playerData = clientsData.get(playerName);

        // Nueva posición enviada por el cliente
        float newY = obj.getFloat("paddleY");
        playerData.paddleY = newY;

        // Actualiza el objeto GameObject correspondiente
        GameObject paddle = playerData.color.equals(Missatges.RED) ? partida.gameObjects.get("paddleLeft")
                                                                    : partida.gameObjects.get("paddleRight");
        if (paddle != null) paddle.y = newY;

        // Broadcast del estado actualizado
        serverUtils.broadcastStatus(partida);
    }

    /** Actualiza la posición de la bola y envía el estado a ambos jugadores */
    public void updateBallPosition(Partida partida, float x, float y) {
        if (partida == null) return;

        GameObject ball = partida.gameObjects.get("ball");
        if (ball != null) {
            ball.x = x;
            ball.y = y;
        }

        serverUtils.broadcastStatus(partida);
    }

    /** Actualiza el marcador de la partida y finaliza si hay ganador */
    public void handleScore(Partida partida, String ganadorColor) {
        if (partida == null) return;

        if (ganadorColor.equalsIgnoreCase(Missatges.RED)) {
            partida.score1++;
        } else if (ganadorColor.equalsIgnoreCase(Missatges.YELLOW)) {
            partida.score2++;
        }

        // Comprobar si hay ganador según configuración
        int maxGoles = 5; // puedes sacarlo de config
        String winner = "";
        if (partida.score1 >= maxGoles) winner = partida.jugador1Name();
        else if (partida.score2 >= maxGoles) winner = partida.jugador2Name();

        if (!winner.isEmpty()) {
            partida.winnerName = winner;
            serverUtils.resetearEstatJugadors(partida);

            JSONObject fiPartida = new JSONObject();
            fiPartida.put(Missatges.K_TYPE, Missatges.PARTIDA_FINALITZADA);
            fiPartida.put("guanyador", winner);

            serverUtils.sendSafe(partida.jugador1, fiPartida.toString());
            serverUtils.sendSafe(partida.jugador2, fiPartida.toString());
        }

        serverUtils.broadcastStatus(partida);
    }
}
