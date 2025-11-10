package com.shared; // todos

import org.json.JSONObject;

public class ClientPerfil {
    public String name;
    public String status; 
    public int score;
    public String color;

    public ClientPerfil(String name, String status, int score, String color) {
        this.name = name;
        this.status = status;
        this.score = score;
        this.color = color;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("status", status);
        obj.put("score", score);
        obj.put("color", color);
        return obj;
    }

    public static ClientPerfil fromJSON(JSONObject obj) {
        return new ClientPerfil(
            obj.optString("name", null),
            obj.optString("status", "connected"),
            obj.optInt("score", 0),
            obj.optString("color", "white")
        );
    }
}
