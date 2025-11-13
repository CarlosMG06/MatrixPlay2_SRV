package com.client.Gestio;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class GestorAnimacio {

    private boolean animating = false;
    private String animColor;
    private int animCol = -1, animRow = -1;
    private double animY, targetY;
    private double fallSpeed = 1200; // px/s

    private final int cellSize;
    private final double startX;
    private final double startY;

    // Fichas ganadoras
    private final List<int[]> fitxesGuanyadores = new ArrayList<>();
    private double tiempoOutline = 0;
    private Color colorGanador = Color.GOLD;

    public GestorAnimacio(int cellSize, double startX, double startY) {
        this.cellSize = cellSize;
        this.startX = startX;
        this.startY = startY;
    }

    public void startAnimation(String color, int row, int col) {
        this.animColor = color;
        this.animRow = row;
        this.animCol = col;
        this.animY = startY; // Comienza desde arriba
        this.targetY = startY + row * cellSize + cellSize / 2;
        this.animating = true;
    }

    // Marcar fichas ganadoras y su color
    public void marcarfitxesGuanyadores(List<int[]> posicionesGanadoras, String colorWinner) {
        fitxesGuanyadores.clear();
        fitxesGuanyadores.addAll(posicionesGanadoras);
        tiempoOutline = 0;

        if (colorWinner.equalsIgnoreCase("red")) {
            colorGanador = Color.RED;
        } else if (colorWinner.equalsIgnoreCase("yellow")) {
            colorGanador = Color.GOLDENROD; // más cálido que amarillo puro
        } else {
            colorGanador = Color.GOLD;
        }
    }

    // Compatibilidad con versiones antiguas
    public void marcarfitxesGuanyadores(List<int[]> posicionesGanadoras) {
        marcarfitxesGuanyadores(posicionesGanadoras, "gold");
    }

    public void run(double fps) {
        if (!animating && fitxesGuanyadores.isEmpty()) return;

        double dt = 1.0 / fps;

        // Animación de caída
        if (animating) {
            animY += fallSpeed * dt;
            if (animY >= targetY) {
                animY = targetY;
                animating = false;
            }
        }

        // Efecto pulsante
        if (!fitxesGuanyadores.isEmpty()) {
            tiempoOutline += dt;
        }
    }

    public void draw(GraphicsContext gc) {
        // --- Animación de caída ---
        if (animating && animColor != null) {
            double r = cellSize * 0.42;
            double cx = startX + animCol * cellSize + cellSize / 2;
            gc.setFill(animColor.equalsIgnoreCase("red") ? Color.RED : Color.YELLOW);
            gc.fillOval(cx - r, animY - r, r * 2, r * 2);
        }

        // --- Efecto de parpadeo (solo fichas ganadoras) ---
        if (!fitxesGuanyadores.isEmpty()) {
            double brillo = 0.5 + 0.5 * Math.sin(tiempoOutline * 4);
            double alpha = 0.4 + 0.4 * Math.sin(tiempoOutline * 4);

            // Color brillante (mismo tono del ganador)
            Color colorBrillo = Color.color(
                colorGanador.getRed(),
                colorGanador.getGreen(),
                colorGanador.getBlue(),
                alpha
            );

            gc.save(); // guardar estado gráfico
            gc.setLineWidth(4 + 2 * brillo);
            gc.setStroke(colorBrillo);

            for (int[] pos : fitxesGuanyadores) {
                int fila = pos[0];
                int columna = pos[1];

                double offset = 5;
                double x = startX + columna * cellSize + offset;
                double y = startY + fila * cellSize + offset;
                double diameter = cellSize - offset * 2;

                // dibujar contorno limpio sin afectar las demás fichas
                gc.strokeOval(x, y, diameter, diameter);
            }

            gc.restore(); // restaurar estado gráfico
        }
    }

    public void reset() {
        animating = false;
        animColor = null;
        animCol = -1;
        animRow = -1;
        animY = 0;
        targetY = 0;
        fitxesGuanyadores.clear();
        tiempoOutline = 0;
        colorGanador = Color.GOLD;
    }

    public boolean isFitxesGuanyadores() {
        return !fitxesGuanyadores.isEmpty();
    }

    public boolean isAnimating() {
        return animating;
    }
}
