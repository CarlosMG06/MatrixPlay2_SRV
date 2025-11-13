package com.shared;

import org.json.JSONObject;

public class GameObject {
    public String id;
    public int x;
    public int y;
    public int startX; // posición original X
    public int startY; // posición original Y
    public int col;
    public int row;
    public boolean isPiece;
    public String color;
    public int radius;

    public GameObject(String id, int x, int y, int cols, int rows, int radius) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.col = cols;
        this.row = rows;
        this.isPiece = false;
        this.color = null;
        this.radius = radius;
        this.startX = x;
        this.startY = y;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("x", x);
        obj.put("y", y);
        obj.put("cols", col);
        obj.put("rows", row);
        obj.put("isPiece", isPiece);
        obj.put("color", color);
        obj.put("radius", radius);
        return obj;
    }

    public static GameObject fromJSON(JSONObject obj) {
        GameObject go = new GameObject(
            obj.optString("id", null),
            obj.optInt("x", 0),
            obj.optInt("y", 0),
            obj.optInt("cols", 1),
            obj.optInt("rows", 1),
            obj.optInt("radius", 0)
        );
        go.isPiece = obj.optBoolean("isPiece", false);
        go.color = obj.optString("color", null);
        return go;
    }
}
