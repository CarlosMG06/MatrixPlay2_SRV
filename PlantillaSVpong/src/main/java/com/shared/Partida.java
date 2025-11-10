package com.shared; //server

import java.util.UUID;
import org.java_websocket.WebSocket;

public class Partida {
    public final String id = UUID.randomUUID().toString();
    public WebSocket playerLeft;
    public WebSocket playerRight;
    public int scoreLeft = 0;
    public int scoreRight = 0;
    public float ballX, ballY;
    public float paddleLeftY, paddleRightY;
    public String winnerName = "";

    public Partida(WebSocket pLeft, WebSocket pRight) {
        this.playerLeft = pLeft;
        this.playerRight = pRight;
        resetGame();
    }

    public void resetGame() {
        scoreLeft = 0;
        scoreRight = 0;
        ballX = 32;      // centro inicial del display 64x64
        ballY = 32;
        paddleLeftY = 28; // posición inicial
        paddleRightY = 28;
        winnerName = "";
    }
}
