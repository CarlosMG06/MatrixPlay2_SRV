package com.client.Controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.client.Main;
import com.client.PlayTimer;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class CtrlPlay implements Initializable {

    @FXML
    public javafx.scene.control.Label title;

    @FXML
    public Canvas canvas;
    public GraphicsContext gc;

    public PlayTimer animationTimer;

    // Estado del juego
    public double playerPaddleY = 200;
    public double rivalPaddleY = 200;
    public double ballX = 400, ballY = 300;
    public double ballVX = 200, ballVY = 200; // px/s

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.gc = canvas.getGraphicsContext2D();

        // Iniciar timer
        animationTimer = new PlayTimer(fps -> run(fps), this::draw, 60);
        animationTimer.start();
    }

    // Actualizar posiciones
    private void run(double fps) {
        double dt = 1.0 / fps;

        // Actualizar posición de la pelota
        ballX += ballVX * dt;
        ballY += ballVY * dt;

        // Rebotes superior/inferior
        if (ballY <= 0 || ballY >= canvas.getHeight() - 15) {
            ballVY = -ballVY;
        }

        // Aquí podrías enviar/recibir posiciones al servidor
        // Main.wsClient.safeSend(...);
    }

    // Dibujar todo
    public void draw() {
        if (gc == null) return;

        // Fondo
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Palas
        gc.setFill(Color.BLUE);
        gc.fillRect(10, playerPaddleY, 10, 80); // jugador
        gc.setFill(Color.RED);
        gc.fillRect(canvas.getWidth() - 20, rivalPaddleY, 10, 80); // rival

        // Pelota
        gc.setFill(Color.BLACK);
        gc.fillOval(ballX, ballY, 15, 15);
    }

    // Métodos para actualizar palas desde servidor o input local
    public void setPlayerPaddleY(double y) {
        playerPaddleY = y;
    }

    public void setRivalPaddleY(double y) {
        rivalPaddleY = y;
    }

    public void setBallPosition(double x, double y) {
        ballX = x;
        ballY = y;
    }
}
