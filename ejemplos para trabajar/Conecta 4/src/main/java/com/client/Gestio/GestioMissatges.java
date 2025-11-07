package com.client.Gestio;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONObject;

import com.client.Controllers.CtrlPlay;
import com.shared.ClientData;
import com.shared.ClientPerfil;
import com.shared.GameObject;
import com.client.*;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
public class GestioMissatges {

  


    public static void crearJugador(String clientName, UtilsWS wsClient) {
        if (clientName == null || clientName.isEmpty()) {
            Alert alerta = new Alert(AlertType.WARNING);
            alerta.setTitle("Nom buit");
            alerta.setHeaderText("Nom de jugador no vàlid");
            alerta.setContentText("Si us plau, introdueix un nom abans de continuar.");
            alerta.showAndWait();
            return; // Aturem el mètode aquí
        }

        JSONObject nom = new JSONObject();
        nom.put("type", "setName");
        nom.put("value", clientName);
        wsClient.safeSend(nom.toString());

        JSONObject nouPerfil = new JSONObject();
        nouPerfil.put("type", "nouPerfil");
        nouPerfil.put("value", new ClientPerfil(clientName, "Disponible").toJSON());
        wsClient.safeSend(nouPerfil.toString());
    }



    public static void processMessage(String response) {
        JSONObject msgObj;
        try {
            msgObj = new JSONObject(response);
        } catch (Exception ex) {
            System.err.println("JSON invàlid: " + response);
            return;
        }

        switch (msgObj.optString("type", "")) {
            case "serverData":
                gestionarServerData(msgObj);
                break;
            case "fitxaColocada":
                gestionarFitxaColocada(msgObj);
                break;
            case "PartidaFinalitzada":
                gestionarFiPartida(msgObj);
                break;
            case "countdown":
                gestionarCountdown(msgObj);
                break;
            case "llistaPerfils":
                gestionarLlistaPerfils(response);
                break;
            case "peticioResposta":
                gestionarPeticioResposta(msgObj);
                break;
            case "invitacioRebuda":
                gestionarInvitacioRebuda(msgObj);
                break;
            case "invitacioEnviada":
                gestionarInvitacioEnviada(msgObj);
                break;
            case "emparellat":
                gestionarEmparellat(msgObj);
                break;
            case "error" :
                String missatge = msgObj.optString("value", "Error desconegut");
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("No es pot enviar la invitació");
                    alert.setHeaderText(null);
                    alert.setContentText(missatge);
                    alert.showAndWait();
                });
            


            

            default:
                System.out.println("Missatge desconegut: " + response);
                break;
        }
    }

            private static void gestionarServerData(JSONObject msgObj) {
                String activeView = UtilsViews.getActiveView();
                
                if (!activeView.equals("ViewWait") && !activeView.equals("ViewPlay") && !activeView.equals("ViewInvitacions")) {
                    return;
                }

                Main.clientName = msgObj.optString("clientName", Main.clientName);

                JSONArray arrClients = msgObj.optJSONArray("clientsList");
                List<ClientData> newClients = new ArrayList<>();
                if (arrClients != null) {
                    for (int i = 0; i < arrClients.length(); i++) {
                        newClients.add(ClientData.fromJSON(arrClients.getJSONObject(i)));
                    }
                }
                Main.clients = newClients;

                JSONArray arrObjects = msgObj.optJSONArray("objectsList");
                List<GameObject> newObjects = new ArrayList<>();
                if (arrObjects != null) {
                    for (int i = 0; i < arrObjects.length(); i++) {
                        newObjects.add(GameObject.fromJSON(arrObjects.getJSONObject(i)));
                    }
                }
                
                Main.objects = newObjects;
                JSONObject game = msgObj.optJSONObject("game");
                if (game != null) {
                    updateGameData(game);
                }
            }






        private static void updateGameData(JSONObject game) {
            CtrlPlay ctrlPlay = Main.ctrlPlay;

            JSONArray boardArr = game.optJSONArray("board");
            if (boardArr != null) {
                for (int r = 0; r < boardArr.length(); r++) {
                    JSONArray rowArr = boardArr.getJSONArray(r);
                    for (int c = 0; c < rowArr.length(); c++) {
                        ctrlPlay.board[r][c] = rowArr.getString(c);
                    }
                }
            }

            String turnoNombre = game.optString("turn", "");
            ctrlPlay.currentPlayer = turnoNombre.equals(Main.clientName) ? 1 : 2;

            Platform.runLater(() -> {
                ctrlPlay.title.setText("Torn de " + turnoNombre);
                ctrlPlay.title.setTextFill(Main.getColorByName(turnoNombre));
            });

            JSONObject lastMove = game.optJSONObject("lastMove");
            if (lastMove != null) {
                int lastRow = lastMove.optInt("row", -1);
                int lastCol = lastMove.optInt("col", -1);
                String color = lastMove.optString("color", null);
                if (lastRow >= 0 && lastCol >= 0 && color != null) {
                    ctrlPlay.gestorAnimacio.startAnimation(color, lastRow, lastCol);
                }
            }

            String winner = game.optString("winner", "");
            
            if (!winner.isEmpty()) {
                ctrlPlay.block = true;
                ctrlPlay.mouseDragging = false;
                Main.partidaFinalizada = true; 

                Platform.runLater(() -> {
                    if (winner.equals("EMPAT")) {
                        ctrlPlay.title.setText("EMPAT");
                        ctrlPlay.title.setTextFill(Color.BLACK);
                    } else {
                        ctrlPlay.title.setText("HA GUANYAT " + winner);
                        ctrlPlay.title.setTextFill(Main.getColorByName(winner));
                    }
                });
            } else {
                Main.partidaFinalizada = false;
                ctrlPlay.block = false;
            }
        }


    private static void gestionarFitxaColocada(JSONObject msgObj) {
        int row = msgObj.optInt("row", -1);
        int col = msgObj.optInt("col", -1);
        String valor = msgObj.optString("value", null);
        String id = msgObj.optString("id", null);

        if (row >= 0 && col >= 0 && valor != null && id != null) {
            String color = valor.equals("R") ? "red" : "yellow";

            for (GameObject go : Main.objects) {
                if (go.id.equals(id)) {
                    go.isPiece = false;
                    go.color = color;
                    break;
                }
            }

            if (Main.ctrlPlay.selectedObject != null && Main.ctrlPlay.selectedObject.id.equals(id)) {
                Main.ctrlPlay.selectedObject = null;
                Main.ctrlPlay.mouseDragging = false;
            }

            Main.ctrlPlay.gestorAnimacio.startAnimation(color, row, col);
        }
    }
    private static void gestionarFiPartida(JSONObject msgObj) {
        String winner = msgObj.optString("guanyador", "");
        String color = msgObj.optString("color", "");
        JSONArray winningCells = msgObj.optJSONArray("winningCells");

        CtrlPlay ctrlPlay = Main.ctrlPlay;

        new Thread(() -> {
            Platform.runLater(() -> {
                // Netejar i redibuixar el canvas
                if (ctrlPlay.canvas != null) {
                    GraphicsContext gc = ctrlPlay.canvas.getGraphicsContext2D();
                    gc.clearRect(0, 0, ctrlPlay.canvas.getWidth(), ctrlPlay.canvas.getHeight());
                }

                resetearEstatJoc();

                if (ctrlPlay.animationTimer != null) {
                    ctrlPlay.draw();
                }

                // ⚡ Mostrar fitxes guanyadores si no és empat
                if (winningCells != null && !winner.equals("EMPAT")) {
                    List<int[]> coords = new ArrayList<>();
                    for (int i = 0; i < winningCells.length(); i++) {
                        JSONArray arr = winningCells.getJSONArray(i);
                        coords.add(new int[]{arr.getInt(0), arr.getInt(1)});
                    }
                    ctrlPlay.gestorAnimacio.marcarfitxesGuanyadores(coords);
                }

                // Bloquejar joc i mostrar títol de resultat
                ctrlPlay.block = true;
                ctrlPlay.mouseDragging = false;
                Main.partidaFinalizada = true;

                if (winner.equals("EMPAT")) {
                    ctrlPlay.title.setText("EMPAT");
                    ctrlPlay.title.setTextFill(Color.BLACK);
                } else {
                    ctrlPlay.title.setText("HA GUANYAT " + winner);
                    ctrlPlay.title.setTextFill(Main.getColorByName(color));
                }
            });

            // ⏳ Esperar 2 segons per veure l’animació
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Mostrar alerta després de la pausa
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Fi de partida");
                alert.setHeaderText("La partida ha finalitzat");

                ButtonType btnYes = new ButtonType("Enrere");
                ButtonType btnNo = new ButtonType("Tancar");
                alert.getButtonTypes().setAll(btnYes, btnNo);

                Optional<ButtonType> opcions = alert.showAndWait();

                if (opcions.isPresent() && opcions.get() == btnYes) {
                    UtilsViews.setViewAnimating("ViewInvitacions");
                } else if (opcions.isPresent() && opcions.get() == btnNo) {
                    Platform.exit();
                }
            });
        }).start();
    }



    private static void gestionarCountdown(JSONObject msgObj) {
        int value = msgObj.optInt("value", -1);
        String txt = value == 0 ? "GO" : String.valueOf(value);
        Main.ctrlWait.txtTitle.setText(txt);
        if (value == 0) UtilsViews.setViewAnimating("ViewPlay");
    }

    private static void gestionarLlistaPerfils(String response) {
        Platform.runLater(() -> {
            Main.ctrlInvitacions.wsMessage(response);
            if (UtilsViews.getActiveView().equals("ViewConfig")) {
                UtilsViews.setViewAnimating("ViewInvitacions");
                Main.ctrlInvitacions.setJugadorNom(Main.clientName);
            }
        });
    }

    private static void gestionarPeticioResposta(JSONObject msgObj) {
        boolean correcte = msgObj.optBoolean("correcte", false);
        String fitxaID = msgObj.optString("fitxaID", null);

        if (fitxaID != null && Main.ctrlPlay.selectedObject != null &&
            Main.ctrlPlay.selectedObject.id.equals(fitxaID)) {
            if (!correcte) {
                Main.ctrlPlay.selectedObject.x = Main.ctrlPlay.selectedObject.startX;
                Main.ctrlPlay.selectedObject.y = Main.ctrlPlay.selectedObject.startY;
                Main.ctrlPlay.mouseDragging = false;
            } else {
                Main.ctrlPlay.mouseDragging = true;
            }
        }
    }

    private static void gestionarInvitacioRebuda(JSONObject msgObj) {
        String challenger = msgObj.optString("challenger", "");
        Main.ctrlInvitacions.ChallengerEntrant = challenger;
        Main.ctrlInvitacions.invitat = Main.clientName;

        Main.ctrlInvitacions.invitacioAlerta(challenger);
    }

    private static void gestionarInvitacioEnviada(JSONObject msgObj) {
        String rival = msgObj.optString("value", "");
        Platform.runLater(() ->
            Main.ctrlWait.txtTitle.setText("Esperant resposta de " + rival)
        );
    }
    private static void gestionarEmparellat(JSONObject msgObj) {
        String player0 = msgObj.optString("jugador1", "");
        String player1 = msgObj.optString("jugador2", "");


        new Thread(() -> {
            try {
                Thread.sleep(100); 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            Platform.runLater(() -> {
                Main.partidaFinalizada = false;
                
                resetearEstatJoc();
                
                Main.ctrlWait.txtPlayer0.setText(player0);
                Main.ctrlWait.txtPlayer1.setText(player1);
                Main.ctrlPlay.title.setText(player0 + " vs " + player1);
                UtilsViews.setView("ViewWait");
                
            });
        }).start();
    }

    private static void resetearEstatJoc() {
        CtrlPlay ctrlPlay = Main.ctrlPlay;
            Main.objects.clear();
        if (ctrlPlay != null) {
            ctrlPlay.resetearJoc();
        }
        Main.partidaFinalizada = false;
        
        System.gc(); 
        
    }
    }
