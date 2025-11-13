package com.client.Controllers;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.client.Main.wsClient;

import com.client.Main;
import com.client.UtilsViews;
import com.shared.ClientPerfil;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class CtrlInvitacions implements Initializable {

    public static List<ClientPerfil> perfils = new ArrayList<>();

    @FXML
    private TableView<ClientPerfil> taulaUsers;

    @FXML
    private TableColumn<ClientPerfil, String> jugadors;

    @FXML
    private TableColumn<ClientPerfil, String> estat;
    @FXML
    private Button invitarButton;
    private String nomJugador;
    @FXML
    private Label LabelJugador;


    private CtrlConfig ctrlConfig = (CtrlConfig) UtilsViews.getController("ViewConfig");
    String name = ctrlConfig.getUserText();
    
    public String ChallengerEntrant,invitat;



@Override
public void initialize(URL url, ResourceBundle rb) {
    jugadors.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().name));
    estat.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().estat));
    taulaUsers.setPlaceholder(new Label("No n'hi han jugadors connectats"));
    taulaUsers.widthProperty().addListener((obs, oldVal, newVal) -> {
        double width = newVal.doubleValue();
        jugadors.setPrefWidth(width / 2);
        estat.setPrefWidth(width / 2);
    });
}


    public void wsMessage(String response) {
        JSONObject msgObj = new JSONObject(response);
        if ("llistaPerfils".equals(msgObj.getString("type"))) {
            JSONArray arrPerfils = msgObj.getJSONArray("value");

            perfils.clear();
            for (int i = 0; i < arrPerfils.length(); i++) {
                ClientPerfil perfil = ClientPerfil.fromJSON(arrPerfils.getJSONObject(i));
                if (!perfil.name.equals(Main.clientName)){
                    perfils.add(perfil);
                
                }
            }
            Platform.runLater(() -> {
                taulaUsers.getItems().setAll(perfils);
                taulaUsers.refresh(); 
            });

        }
    }



    public void actualitzarTaula() {
        Platform.runLater(() -> {
            taulaUsers.getItems().clear();
            taulaUsers.getItems().addAll(perfils);
        });
    }

    @FXML
    public void sendInvitacion(){

        ClientPerfil clientRival = taulaUsers.getSelectionModel().getSelectedItem();
        if (clientRival != null){
            String clientName = clientRival.name;
            JSONObject invitacion = new JSONObject();
            invitacion.put("type", "invitacion");
            invitacion.put("rival", clientName);
            wsClient.safeSend(invitacion.toString());  
        }else{
            Platform.runLater(()->{
                AlertaError();


            });

        }


    }

    public void invitacioAlerta(String challenger){ //https://www.youtube.com/watch?v=U-ZUOCSycRw
        Alert msg = new Alert(Alert.AlertType.CONFIRMATION);
        msg.setTitle("invitació");
        msg.setHeaderText("T'ESTAN RETANT");
        msg.setContentText("Vols acceptar?");
        ButtonType buttonAcceptar = new ButtonType("Acceptar");
        ButtonType buttonRebuig = new ButtonType("Rebutjar");
        msg.getButtonTypes().setAll(buttonAcceptar,buttonRebuig);
        Optional<ButtonType> opcions = msg.showAndWait();
        if (opcions.get() == buttonAcceptar){
            
            JSONObject accept = new JSONObject();
            accept.put("type", "acceptInvitation");
            accept.put("challenger", challenger);
            accept.put("invited", Main.clientName);
            Main.wsClient.safeSend(accept.toString());
            Main.ctrlWait.txtPlayer0.setText(challenger);
            Main.ctrlWait.txtPlayer1.setText(Main.clientName);
            Main.ctrlPlay.title.setText(challenger + " vs " + Main.clientName);

            UtilsViews.setView("ViewWait");

            ChallengerEntrant = null;
            invitat = null;


        
        }
    }

    public void AlertaError(){
        Alert msg = new Alert(Alert.AlertType.ERROR);
        msg.setTitle("ERROR");
        msg.setHeaderText("ERROR");
        msg.setContentText("No has seleccionat cap jugador!");
        msg.showAndWait();
    }
    public void setJugadorNom(String nomJugador) {
        this.nomJugador = nomJugador;
        LabelJugador.setText("BENVINGUT " + nomJugador);
    }
}