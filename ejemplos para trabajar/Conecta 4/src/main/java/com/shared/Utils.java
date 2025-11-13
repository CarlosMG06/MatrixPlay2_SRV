package com.shared;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.java_websocket.WebSocket;

import com.client.Main;
import com.client.Controllers.CtrlPlay;

import javafx.scene.paint.Color;




public class Utils {


    public static List<ClientData> clients;
    public static CtrlPlay ctrlPlay;

    public Utils(CtrlPlay CrtlPlay,List<ClientData> clients){
        this.clients = clients;
    }

    private Color getColorByName(String name) {
        String color = clients.stream()
                .filter(c -> c.name.equals(name))
                .map(c -> c.color)
                .findFirst()
                .orElse("gray");
        return ctrlPlay.getColor(color);
    }

    public static String getClientColor(){
            return clients.stream()
                .filter(c -> c.name.equals(Main.clientName))
                .map(c -> c.color)
                .findFirst()
                .orElse("gray");}  

}
