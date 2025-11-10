package com.client.Gestio;

import org.json.JSONObject;
import com.client.Main;
import javafx.application.Platform;

public class GestioMissatgesPong {

    /**
     * Procesa un mensaje recibido del servidor
     * @param response Mensaje JSON como String
     */
    public static void processMessage(String response) {
        JSONObject msgObj;
        try {
            msgObj = new JSONObject(response);
        } catch (Exception ex) {
            System.err.println("JSON inválido: " + response);
            return;
        }

        switch (msgObj.optString("type", "")) {
            case "gameState":
                actualizarEstadoJuego(msgObj);
                break;

            case "scoreUpdate":
                actualizarPuntaje(msgObj);
                break;

            case "gameOver":
                gestionarFinPartida(msgObj);
                break;

            default:
                System.out.println("Mensaje desconocido: " + response);
                break;
        }
    }

    /** Actualiza posiciones de palas y bola */
    private static void actualizarEstadoJuego(JSONObject msgObj) {
        Platform.runLater(() -> {
            if (Main.ctrlPlay != null) {
                // Actualizar posición bola
                JSONObject ball = msgObj.optJSONObject("ball");
                if (ball != null) {
                    Main.ctrlPlay.bolaX = ball.optDouble("x", Main.ctrlPlay.bolaX);
                    Main.ctrlPlay.bolaY = ball.optDouble("y", Main.ctrlPlay.bolaY);
                }

                // Actualizar palas
                JSONObject paddles = msgObj.optJSONObject("paddles");
                if (paddles != null) {
                    Main.ctrlPlay.palaJugador1Y = paddles.optDouble("player1Y", Main.ctrlPlay.palaJugador1Y);
                    Main.ctrlPlay.palaJugador2Y = paddles.optDouble("player2Y", Main.ctrlPlay.palaJugador2Y);
                }

                // Redibujar canvas
                Main.ctrlPlay.draw();
            }
        });
    }

    /** Actualiza puntaje de los jugadores */
    private static void actualizarPuntaje(JSONObject msgObj) {
        Platform.runLater(() -> {
            if (Main.ctrlPlay != null) {
                Main.ctrlPlay.scoreJugador1 = msgObj.optInt("player1Score", Main.ctrlPlay.scoreJugador1);
                Main.ctrlPlay.scoreJugador2 = msgObj.optInt("player2Score", Main.ctrlPlay.scoreJugador2);
                Main.ctrlPlay.updateScoreDisplay();
            }
        });
    }

    /** Maneja el fin de la partida */
    private static void gestionarFinPartida(JSONObject msgObj) {
        String ganador = msgObj.optString("winner", ""); // nombre del jugador ganador
        Platform.runLater(() -> {
            if (Main.ctrlPlay != null) {
                Main.ctrlPlay.gameOver(ganador);
            }
        });
    }
}
