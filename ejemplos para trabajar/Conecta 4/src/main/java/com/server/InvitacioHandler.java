package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import com.shared.ClientPerfil;

import java.util.List;

public class InvitacioHandler {

    private final List<ClientPerfil> perfils;
    private final ClientRegistry clients;
    private final Main server; 
    private final GameHandler gameHandler;
    private final ServerUtils serverUtils;

    public InvitacioHandler(List<ClientPerfil> perfils, ClientRegistry clients, Main server, 
                            GameHandler gameHandler, ServerUtils serverUtils) {
        this.perfils = perfils;
        this.clients = clients;
        this.server = server;
        this.gameHandler = gameHandler;
        this.serverUtils = serverUtils;
    }

    public void handleInvitacion(WebSocket conn, JSONObject obj) {
        String client = clients.nameBySocket(conn);
        String rival = obj.getString(Missatges.RIVAL);

        ClientPerfil perfRival = perfils.stream()
                .filter(p -> p.name.equals(rival))
                .findFirst()
                .orElse(null);

        if (perfRival == null || !perfRival.estat.equals(Missatges.ESTAT_DISPONIBLE)) {
            JSONObject msg = new JSONObject();
            msg.put(Missatges.K_TYPE, Missatges.ERROR);
            msg.put(Missatges.K_VALUE, "El jugador ja està jugant.");
            serverUtils.sendSafe(conn, msg.toString());
            return;
        }

        WebSocket contrincant = clients.socketByName(rival);
        if (contrincant == null) return;

        JSONObject invitacioRebuda = new JSONObject();
        invitacioRebuda.put(Missatges.K_TYPE, Missatges.INVITACIO_REBUDA);
        invitacioRebuda.put(Missatges.CHALLENGER, client);
        serverUtils.sendSafe(contrincant, invitacioRebuda.toString());

        JSONObject confirmacio = new JSONObject();
        confirmacio.put(Missatges.K_TYPE, Missatges.INVITACIO_ENVIADA);
        confirmacio.put(Missatges.K_VALUE, rival);
        serverUtils.sendSafe(conn, confirmacio.toString());
    }

    public void handleAcceptInvitation(JSONObject obj) {
        String challenger = obj.getString(Missatges.CHALLENGER);
        String invited = obj.getString(Missatges.INVITED);

        WebSocket chalSock = clients.socketByName(challenger);
        WebSocket invSock = clients.socketByName(invited);

        if (chalSock != null && invSock != null) {
            gameHandler.crearPartida(chalSock, invSock);
        }
    }
}
