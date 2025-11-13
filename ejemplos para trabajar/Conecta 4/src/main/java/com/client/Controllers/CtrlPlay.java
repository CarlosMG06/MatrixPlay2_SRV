package com.client.Controllers;

import java.net.URL;
import java.util.ResourceBundle;

import com.client.Main;
import com.client.PlayGrid;
import com.client.PlayTimer;
import com.client.UtilsViews;
import com.client.Gestio.GestioTauler;
import com.client.Gestio.GestorAnimacio;
import com.client.Gestio.GestorMouse;
import com.shared.GameObject;

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
    public PlayGrid grid;
    public GestorMouse gestorMouse;
    public GestorAnimacio gestorAnimacio;

    // Connect 4 config
    public static final int ROWS = 6;
    public static final int COLS = 7;
    public final String[][] board = new String[ROWS][COLS];
    public int currentPlayer = 1;
    public boolean block = false;

    // Drag
    public GameObject selectedObject = null;
    public boolean mouseDragging = false;
    public double mouseOffsetX, mouseOffsetY;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        this.gc = canvas.getGraphicsContext2D();
        gestorMouse = new GestorMouse(this);

        // Listeners de tamaño
        UtilsViews.parentContainer.heightProperty().addListener((o, ov, nv) -> onSizeChanged());
        UtilsViews.parentContainer.widthProperty().addListener((o, ov, nv) -> onSizeChanged());

        // Eventos mouse
        canvas.setOnMouseMoved(gestorMouse::onMouseMoved);
        canvas.setOnMousePressed(gestorMouse::onMousePressed);
        canvas.setOnMouseDragged(gestorMouse::onMouseDragged);
        canvas.setOnMouseReleased(gestorMouse::onMouseReleased);

        grid = new PlayGrid(25, 25, 80, ROWS, COLS);

        // Gestor de animaciones
        gestorAnimacio = new GestorAnimacio((int) grid.getCellSize(), grid.getStartX(), grid.getStartY());

        // Timer
        animationTimer = new PlayTimer(this::run, this::draw, 60);
        animationTimer.start();

        // Inicializar tablero
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = "";
            }
        }
    }

    private void onSizeChanged() {
        double width = UtilsViews.parentContainer.getWidth();
        double height = UtilsViews.parentContainer.getHeight();
        canvas.setWidth(width);
        canvas.setHeight(height);
    }

    // --- Lógica del timer ---
    private void run(double fps) {
        gestorAnimacio.run(fps);
    }

    // --- Dibujar ---
    public void draw() {
        if (gc == null) return;

        // Limpiar fondo
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Dibujar tablero y fichas colocadas
        GestioTauler.drawBoard(gc, grid, ROWS, COLS,board);

        // Dibujar clientes
        GestioTauler.drawClients(gc, grid, Main.clients);

        // Dibujar objetos (fichas en mano)
        GestioTauler.drawObjects(gc, Main.objects);

        // Dibujar animación
        gestorAnimacio.draw(gc);
    }

    public static javafx.scene.paint.Color getColor(String colorName) {
        switch (colorName.toLowerCase()) {
            case "red": return Color.RED;
            case "yellow": return Color.GOLD;
            case "blue": return Color.BLUE;
            case "green": return Color.GREEN;
            case "orange": return Color.ORANGE;
            case "purple": return Color.PURPLE;
            case "pink": return Color.PINK;
            case "brown": return Color.BROWN;
            case "gray": return Color.GRAY;
            case "black": return Color.BLACK;
            default: return Color.LIGHTGRAY;
        }
    }
public void resetearJoc() {
    
    for (int r = 0; r < ROWS; r++) {
        for (int c = 0; c < COLS; c++) {
            board[r][c] = "";
        }
    }
    
    //RESETEAR JUEGO
    currentPlayer = 1;
    block = false;
    selectedObject = null;
    mouseDragging = false;
    mouseOffsetX = 0;
    mouseOffsetY = 0;
    
    if (gestorAnimacio != null) {
        gestorAnimacio.reset();
    }
        if (canvas != null) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }
        javafx.application.Platform.runLater(() -> {
        title.setText("Conecta 4");
        title.setTextFill(Color.BLACK);
    });
    
}
        public void clearObjects() {        // Este metodo se llama cuando Main.objects se limpia
        selectedObject = null;
        mouseDragging = false;
    }
}
