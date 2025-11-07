package com.shared;

import org.json.JSONObject;

public class ClientPerfil {
    public String name;
    public String estat; 
    
    public ClientPerfil(String name, String estat) {
        this.name = name;
        this.estat = estat;
    }

    @Override
    public String toString() {
        return this.toJSON().toString();
    }

    // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("estat", estat);
        return obj;
    }

    // Crea un ClientPerfil a partir de JSON
    public static ClientPerfil fromJSON(JSONObject obj) {
        String name = obj.optString("name", null);
        String estat = obj.optString("estat", null);
        return new ClientPerfil(name, estat);
    }
}
