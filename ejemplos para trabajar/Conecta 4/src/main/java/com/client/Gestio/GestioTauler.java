package com.client.Gestio;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import com.client.PlayGrid;
import com.shared.ClientData;
import com.shared.GameObject;

import java.util.List;

public class GestioTauler {

    public static void drawBoard(GraphicsContext gc, PlayGrid grid, int ROWS, int COLS, String[][] board) {
        double cellSize = grid.getCellSize();
        double startX = grid.getStartX();
        double startY = grid.getStartY();

        // Fondo azul
        gc.setFill(Color.rgb(30, 96, 199));
        gc.fillRect(startX, startY, COLS * cellSize, ROWS * cellSize);

        // Huecos blancos
        double r = cellSize * 0.42;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                double cx = startX + col * cellSize + cellSize / 2;
                double cy = startY + row * cellSize + cellSize / 2;
                gc.setFill(Color.rgb(235, 235, 235));
                gc.fillOval(cx - r, cy - r, r * 2, r * 2);

                // Dibujar ficha colocada
                String v = board[row][col];
                if (v.equals("R")) {
                    gc.setFill(Color.RED);
                    gc.fillOval(cx - r, cy - r, r * 2, r * 2);
                } else if (v.equals("Y")) {
                    gc.setFill(Color.YELLOW);
                    gc.fillOval(cx - r, cy - r, r * 2, r * 2);
                }
            }
        }
    }

    public static void drawClients(GraphicsContext gc, PlayGrid grid, List<ClientData> clients) {
        for (ClientData cd : clients) {
            if (cd.row >= 0 && cd.col >= 0) {
                Color base = Color.web(cd.color);
                gc.setFill(new Color(base.getRed(), base.getGreen(), base.getBlue(), 0.3));
                gc.fillRect(grid.getCellX(cd.col), grid.getCellY(cd.row), grid.getCellSize(), grid.getCellSize());
            }
            gc.setFill(Color.web(cd.color));
            gc.fillOval(cd.mouseX - 5, cd.mouseY - 5, 10, 10);
        }
    }

    public static void drawObjects(GraphicsContext gc, List<GameObject> objects) {
        for (GameObject go : objects) {
            if (!go.isPiece) continue;

            Color color;
            try { 
                color = Color.web(go.color); 
            } catch (Exception e) { 
                color = Color.GRAY; 
            }

            gc.setFill(color);
            gc.fillOval(go.x - go.radius, go.y - go.radius, go.radius * 2, go.radius * 2);
            gc.setStroke(Color.BLACK);
            gc.strokeOval(go.x - go.radius, go.y - go.radius, go.radius * 2, go.radius * 2);
            gc.setFill(Color.BLACK);
            gc.fillText(go.id, go.x - (go.id.length() * 3), go.y + 4);
        }
    }



}
