package com.shared; // todos

import org.json.JSONObject;

public class GameObject {
    public String id;
    public float x;
    public float y;
    public float startX;
    public float startY;
    public int radius;
    public String color; // opcional, para palas o bola

    public GameObject(String id, float x, float y, float radius, String color) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.radius = radius;
        this.color = color;
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("radius", radius);
        obj.put("color", color);
        return obj;
    }

    public static GameObject fromJSON(JSONObject obj) {
        return new GameObject(
            obj.optString("id", null),
            obj.optDouble("x", 0),
            obj.optDouble("y", 0),
            obj.optInt("radius", 0),
            obj.optString("color", null)
        );
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }
}
