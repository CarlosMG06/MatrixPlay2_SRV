package com.server;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import com.shared.ClientData;
import com.shared.ClientPerfil;

import java.util.List;
import java.util.Map;

public class CrearClientHandler {

    private final List<ClientPerfil> perfils;
    private final ClientRegistry clients;
    private final Map<String, ClientData> clientsData;
    private final Main server;
    private final ServerUtils serverUtils;

    public CrearClientHandler(List<ClientPerfil> perfils, ClientRegistry clients, Map<String, ClientData> clientsData,
                              Main server, ServerUtils serverUtils) {
        this.perfils = perfils;
        this.clients = clients;
        this.clientsData = clientsData;
        this.server = server;
        this.serverUtils = serverUtils;
    }

    public void handleNouPerfil(JSONObject obj) {
        JSONObject value = obj.getJSONObject(Missatges.K_VALUE);
        ClientPerfil perf = ClientPerfil.fromJSON(value);

        boolean exists = perfils.stream().anyMatch(p -> p.name.equals(perf.name));
        if (!exists) {
            perfils.add(perf);
        }

        JSONArray arrPerfils = new JSONArray();
        for (ClientPerfil p : perfils) {
            arrPerfils.put(p.toJSON());
        }

        JSONObject perfMsg = new JSONObject();
        perfMsg.put(Missatges.K_TYPE, Missatges.LLISTA_PERFILS);
        perfMsg.put(Missatges.K_VALUE, arrPerfils);

        server.broadcast(perfMsg.toString());
    }

    public void handleClientSetName(WebSocket conn, JSONObject obj) {
        String userName = obj.getString(Missatges.K_VALUE);
        if (clients.snapshot().containsValue(userName)) {
            JSONObject error = new JSONObject();
            error.put(Missatges.K_TYPE, Missatges.ERROR);
            error.put(Missatges.K_VALUE, "Nom ja utilitzat. Tria un altre.");
            serverUtils.sendSafe(conn, error.toString());
            return;
        }

        clients.add(conn, userName);

        String color = (clients.snapshot().size() == 1) ? Missatges.RED : Missatges.YELLOW;
        clientsData.put(userName, new ClientData(userName, color));

        System.out.println("Nou client connectat: " + userName);
    }
}
