package com.server;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends WebSocketServer {

    private final ClientRegistry clients;
    private static final List<String> PLAYER_NAMES = Arrays.asList(
            "Bulbasaur", "Charizard", "Blaziken", "Umbreon", "Mewtwo", "Pikachu", "Wartortle"
    );

    public static final int DEFAULT_PORT = 3000;

    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(PLAYER_NAMES);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Asignar un nombre del pool
        String name = clients.add(conn, "Player" + (int)(Math.random() * 1000));
        System.out.println("Cliente conectado: " + name);
        sendClientsListToAll();
        broadcastText("Bienvenido " + name + "!", 5000);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        System.out.println("Cliente desconectado: " + name);
        sendClientsListToAll();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Aquí procesarás mensajes del Pong, por ejemplo: movimiento o power-ups
        System.out.println("Mensaje recibido de " + clients.nameBySocket(conn) + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket iniciado en puerto " + getPort());
        setConnectionLostTimeout(100);
    }

    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        System.out.println("Servidor corriendo en puerto " + DEFAULT_PORT + ". Ctrl+C para detener.");
    }

    // ===================== FUNCIONES PARA CLIENTES =====================

    /** Envía mensaje de texto a todos los clientes */
    public void broadcastText(String message, long ttl_ms) {
        JSONObject o = new JSONObject();
        o.put("type", "text");
        o.put("message", message);
        o.put("ttl_ms", ttl_ms);
        broadcastSafe(o.toString());
    }

    /** Envía imagen a todos los clientes (Base64) */
    public void broadcastImage(byte[] imageBytes, long ttl_ms, String name) {
        JSONObject o = new JSONObject();
        o.put("type", "image");
        o.put("b64", Base64.getEncoder().encodeToString(imageBytes));
        o.put("ttl_ms", ttl_ms);
        o.put("name", name);
        broadcastSafe(o.toString());
    }

    /** Envía mensaje solo a un cliente */
    public void sendText(WebSocket conn, String message, long ttl_ms) {
        if (conn != null && conn.isOpen()) {
            JSONObject o = new JSONObject();
            o.put("type", "text");
            o.put("message", message);
            o.put("ttl_ms", ttl_ms);
            sendSafe(conn, o.toString());
        }
    }

    /** Envía imagen solo a un cliente */
    public void sendImage(WebSocket conn, byte[] imageBytes, long ttl_ms, String name) {
        if (conn != null && conn.isOpen()) {
            JSONObject o = new JSONObject();
            o.put("type", "image");
            o.put("b64", Base64.getEncoder().encodeToString(imageBytes));
            o.put("ttl_ms", ttl_ms);
            o.put("name", name);
            sendSafe(conn, o.toString());
        }
    }

    /** Envía la lista de clientes a todos */
    public void sendClientsListToAll() {
        JSONArray list = clients.currentNames();
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            JSONObject msg = new JSONObject();
            msg.put("type", "clients");
            msg.put("id", e.getValue());
            msg.put("list", list);
            sendSafe(e.getKey(), msg.toString());
        }
    }

    /** Envía de manera segura a un cliente (maneja desconexiones) */
    private void sendSafe(WebSocket to, String payload) {
        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            System.out.println("Cliente desconectado durante send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Broadcast seguro para todos los clientes */
    private void broadcastSafe(String payload) {
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            sendSafe(e.getKey(), payload);
        }
    }
}
