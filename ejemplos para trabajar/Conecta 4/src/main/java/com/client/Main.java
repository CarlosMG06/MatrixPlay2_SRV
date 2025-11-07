package com.client;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.client.Controllers.CtrlConfig;
import com.client.Controllers.CtrlInvitacions;
import com.client.Controllers.CtrlPlay;
import com.client.Controllers.CtrlWait;
import com.client.Gestio.GestioMissatges;
import com.shared.ClientData;
import com.shared.ClientPerfil;
import com.shared.GameObject;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main extends Application {

    public static UtilsWS wsClient;
    public static String clientName = "";
    public static String rivalName = "";
    public static List<ClientData> clients = new ArrayList<>();
    public static List<GameObject> objects = new ArrayList<>();
    public static boolean partidaFinalizada = false;

    public static CtrlConfig ctrlConfig;
    public static CtrlWait ctrlWait;
    public static CtrlPlay ctrlPlay;
    public static CtrlInvitacions ctrlInvitacions;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        initViews();
        Scene scene = new Scene(UtilsViews.parentContainer);
        stage.setScene(scene);
        configureStage(stage);
    }

    private void initViews() {
        try {
            UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
            UtilsViews.addView(getClass(), "ViewConfig", "/assets/viewConfig.fxml"); 
            UtilsViews.addView(getClass(), "ViewWait", "/assets/viewWait.fxml");
            UtilsViews.addView(getClass(), "ViewPlay", "/assets/viewPlay.fxml");
            UtilsViews.addView(getClass(), "ViewInvitacions", "/assets/viewInvitacions.fxml");

            ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
            ctrlWait = (CtrlWait) UtilsViews.getController("ViewWait");
            ctrlPlay = (CtrlPlay) UtilsViews.getController("ViewPlay");
            ctrlInvitacions = (CtrlInvitacions) UtilsViews.getController("ViewInvitacions");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void configureStage(Stage stage) {
        final int width = 900, height = 900;
        stage.setTitle("JavaFX");
        stage.setMinWidth(width);
        stage.setMinHeight(height);
        stage.show();

        if (!System.getProperty("os.name").contains("Mac")) {
            stage.getIcons().add(new Image("file:/icons/icon.png"));
        }
    }

    @Override
    public void stop() {
        if (wsClient != null) wsClient.forceExit();
        System.exit(1);
    }

    public static void pauseDuring(long millis, Runnable action) {
        PauseTransition pause = new PauseTransition(Duration.millis(millis));
        pause.setOnFinished(e -> Platform.runLater(action));
        pause.play();
    }

    public static <T> List<T> jsonArrayToList(JSONArray array, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) list.add(clazz.cast(array.get(i)));
        return list;
    }

    public static void connectToServer() {
        ctrlConfig.txtMessage.setTextFill(Color.BLACK);
        ctrlConfig.txtMessage.setText("Connecting ...");

        pauseDuring(1500, () -> {
            String url = ctrlConfig.txtProtocol.getText() + "://" + ctrlConfig.txtHost.getText() + ":" + ctrlConfig.txtPort.getText();
            wsClient = UtilsWS.getSharedInstance(url);

            wsClient.onMessage(response -> Platform.runLater(() -> GestioMissatges.processMessage(response)));
            wsClient.onError(response -> Platform.runLater(() -> handleError(response)));
            wsClient.onOpen(response -> Platform.runLater(() -> GestioMissatges.crearJugador(clientName,wsClient)));
        });
    }

    private static void handleError(String response) {
        if (response.contains("Connection refused")) {
            ctrlConfig.txtMessage.setTextFill(Color.RED);
            ctrlConfig.txtMessage.setText("Connection refused");
            pauseDuring(1500, () -> ctrlConfig.txtMessage.setText(""));
        }
    }
    public static String getClientColor() {
        return clients.stream().filter(c -> c.name.equals(clientName)).map(c -> c.color).findFirst().orElse("gray");
    }

    public static Color getColorByName(String name) {
        String color = clients.stream().filter(c -> c.name.equals(name)).map(c -> c.color).findFirst().orElse("gray");
        return ctrlPlay.getColor(color);
    }

}