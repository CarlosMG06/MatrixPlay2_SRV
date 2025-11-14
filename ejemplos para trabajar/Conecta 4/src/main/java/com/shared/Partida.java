package com.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.java_websocket.WebSocket;

public class Partida {
    public final String id = UUID.randomUUID().toString(); // ID único
    public WebSocket jugador1;
    public WebSocket jugador2;
    public String[][] board = new String[6][7];
    public int currentPlayer = 1; // 1=rojo, 2=amarillo
    public int lastRow = -1, lastCol = -1;
    public String winnerName = "";
    public ClientPerfil perfil1, perfil2;

    public Partida(WebSocket j1, ClientPerfil p1, WebSocket j2, ClientPerfil p2) {
        this.jugador1 = j1;
        this.jugador2 = j2;
        this.perfil1 = p1;
        this.perfil2 = p2;
        resetBoard();
    }

    public void resetBoard() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 7; c++) board[r][c] = "";
        }
        lastRow = -1;
        lastCol = -1;
        winnerName = "";
        currentPlayer = 1;
    }
}
