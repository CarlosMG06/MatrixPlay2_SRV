package com.client.Gestio;

import org.json.JSONObject;

import com.client.Main;
import com.client.Controllers.CtrlPlay;
import com.shared.ClientData;
import com.shared.GameObject;

import javafx.scene.input.MouseEvent;

public class GestorMouse {

    private final CtrlPlay ctrlPlay;

    public GestorMouse(CtrlPlay ctrlPlay) {
        this.ctrlPlay = ctrlPlay;
    }

    // Al mover el mouse
    public void onMouseMoved(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();

        String color = Main.clients.stream()
            .filter(c -> c.name.equals(Main.clientName))
            .map(c -> c.color)
            .findFirst()
            .orElse("gray");

        ClientData cd = new ClientData(
            Main.clientName,
            color,
            (int) x,
            (int) y,
            ctrlPlay.grid.isPositionInsideGrid(x, y) ? ctrlPlay.grid.getRow(y) : -1,
            ctrlPlay.grid.isPositionInsideGrid(x, y) ? ctrlPlay.grid.getCol(x) : -1
        );

        JSONObject msg = new JSONObject();
        msg.put("type", "clientMouseMoving"); //envias que cliente esta moviendo el mouse
        msg.put("value", cd.toJSON());

        if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
    }

    // Al pulsar el mouse
    public void onMousePressed(MouseEvent e) {
        if (ctrlPlay.block || Main.partidaFinalizada) {
            return;
        }

        String colorActual = Main.getClientColor(); 

        for (GameObject go : Main.objects) { //con esto miramos las fichas del jugador actual(si no esta colocada,si es de su color,y es correcta)
            if (go.isPiece && go.color.equalsIgnoreCase(colorActual) &&
                insideCircle(e.getX(), e.getY(), go.x, go.y, go.radius)) {

                ctrlPlay.selectedObject = go;
                ctrlPlay.mouseOffsetX = e.getX() - go.x;
                ctrlPlay.mouseOffsetY = e.getY() - go.y;
                ctrlPlay.mouseDragging = true;

                JSONObject msg = new JSONObject();
                msg.put("type", "PeticioSelect");
                msg.put("player", Main.clientName);
                msg.put("color", colorActual);
                msg.put("value", go.toJSON()); //Envia el objeto actualizado

                if (Main.wsClient != null) Main.wsClient.safeSend(msg.toString());
                break;
            }
        }
    }


    // -Al arrastrar al mouse
    public void onMouseDragged(MouseEvent e) {
        if (ctrlPlay.block || Main.partidaFinalizada) {
            return;
        }
            
        if (!ctrlPlay.mouseDragging || ctrlPlay.selectedObject == null) {
            return;
        }

        ctrlPlay.selectedObject.x = (int) (e.getX() - ctrlPlay.mouseOffsetX);
        ctrlPlay.selectedObject.y = (int) (e.getY() - ctrlPlay.mouseOffsetY);

        JSONObject msg = new JSONObject();
        msg.put("type", "clientObjectMoving"); //Mensaje de que esta moviendo la ficha
        msg.put("value", ctrlPlay.selectedObject.toJSON());

        if (Main.wsClient != null) {
            Main.wsClient.safeSend(msg.toString());
        }

        onMouseMoved(e);         // Actualizamos también la posición del mouse

    }

    // -Al soltar el mouse
    public void onMouseReleased(MouseEvent e) {
        if (ctrlPlay.block || Main.partidaFinalizada) {
            return;
        }
            
        if (ctrlPlay.selectedObject == null) {
            return;
        }

        int col = ctrlPlay.grid.getCol(e.getX());


        if (col >= 0 && col < CtrlPlay.COLS) {
            JSONObject obj = new JSONObject();
            obj.put("type", "ColocarFitxa");
            obj.put("name", Main.clientName);
            obj.put("color", Main.getClientColor());
            obj.put("col", col);
            obj.put("id", ctrlPlay.selectedObject.id);

            if (Main.wsClient != null){
                Main.wsClient.safeSend(obj.toString());
            } 
        }

        ctrlPlay.selectedObject = null;
        ctrlPlay.mouseDragging = false;
    }

    // comprobar si el mouse esta tocando ficha.
    private boolean insideCircle(double x, double y, double cx, double cy, double r) {
        double dx = x - cx;
        double dy = y - cy;
        return dx * dx + dy * dy <= r * r;
    }
}
