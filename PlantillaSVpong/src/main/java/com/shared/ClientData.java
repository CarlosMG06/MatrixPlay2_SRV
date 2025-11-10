package com.shared; // todos

import org.json.JSONObject;

public class ClientData {
    public String name;
    public String color;
    public float paddleY;
    public float paddleX; // opcional

    public ClientData(String name, String color, float paddleX, float paddleY) {
        this.name = name;
        this.color = color;
        this.paddleX = paddleX;
        this.paddleY = paddleY;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("color", color);
        obj.put("paddleX", paddleX);
        obj.put("paddleY", paddleY);
        return obj;
    }

    public static ClientData fromJSON(JSONObject obj) {
        return new ClientData(
            obj.optString("name", null),
            obj.optString("color", "white"),
            obj.optDouble("paddleX", 0),
            obj.optDouble("paddleY", 0)
        );
    }
}
