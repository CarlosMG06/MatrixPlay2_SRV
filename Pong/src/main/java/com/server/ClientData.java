package com.server;

import org.json.JSONObject;

public class ClientData {
    public String name;
    public int player;
    public int poss;
    public int points;

    public ClientData(String name) {
        this.name = name;
        this.player = -1;
        this.poss=-1;
        this.points = 0;
    }

    public ClientData(String name, int player, int poss, int points) {
        this.name = name;
        this.player = player;
        this.poss=poss;
        this.points=points;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("player", player);
        obj.put("poss", poss);
        obj.put("points", points);

        return obj;
    }

    // Crea un ClientData a partir de JSON
    public static ClientData fromJSON(JSONObject obj) {
        String name = obj.optString("name", null);

        ClientData cd = new ClientData(name);
        cd.player = obj.optInt("player", -1);
        cd.poss = obj.optInt("poss", -1);
        cd.points = obj.optInt("points", -1);
        return cd;
    }
}
