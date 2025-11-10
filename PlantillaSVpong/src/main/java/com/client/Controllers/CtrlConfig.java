package com.client.Controllers;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.UUID;

import com.client.Main;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;

public class CtrlConfig implements Initializable {

    @FXML
    public TextField txtProtocol;

    @FXML
    public TextField txtHost;

    @FXML
    public TextField txtPort;

    @FXML
    public Label txtMessage;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuración por defecto
        txtProtocol.setText("ws");
        txtHost.setText("localhost");
        txtPort.setText("3000");

        // Generar nombre de jugador aleatorio
        Main.clientName = "Player-" + UUID.randomUUID().toString().substring(0, 5);

        // Conectar automáticamente
        Main.connectToServer();
    }

    // Métodos para cambiar la configuración si quieres
    @FXML
    private void setConfigLocal() {
        txtProtocol.setText("ws");
        txtHost.setText("localhost");
        txtPort.setText("3000");
    }

    @FXML
    private void setConfigProxmox() {
        txtProtocol.setText("wss");
        txtHost.setText("user.ieti.site");
        txtPort.setText("443");
    }
}
