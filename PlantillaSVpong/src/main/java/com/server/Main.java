package com.server;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Arrays;
import java.util.Base64;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
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
        // Asignar un nombre aleatorio del pool
        String name = clients.add(conn, "Player" + (int)(Math.random() * 1000));
        System.out.println("Cliente conectado: " + name);

        // Enviar broadcast de bienvenida a todos los clientes
        broadcastText("Hola a todos!", 5000);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        System.out.println("Cliente desconectado: " + name);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);
        // interpretar mensajes del Pong, por ejemplo "move" o "shoot"
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

    // ===================== NUEVAS FUNCIONES PARA RPI =====================

    /**
     * Envia un mensaje de texto a todos los clientes conectados (broadcast)
     * Compatible con el cliente RPi
     */
    public void broadcastText(String message, long ttl_ms) {
        JSONObject o = new JSONObject();
        o.put("type", "text");
        o.put("message", message);
        o.put("ttl_ms", ttl_ms);

        broadcast(o.toString());
    }

    /**
     * Envia una imagen (en bytes) a todos los clientes conectados
     * Compatible con el cliente RPi
     */
    public void broadcastImage(byte[] imageBytes, long ttl_ms, String name) {
        JSONObject o = new JSONObject();
        o.put("type", "image");
        o.put("b64", Base64.getEncoder().encodeToString(imageBytes));
        o.put("ttl_ms", ttl_ms);
        o.put("name", name);

        broadcast(o.toString());
    }

    /**
     * Enviar mensaje de texto solo a un cliente específico
     */
    public void sendText(WebSocket conn, String message, long ttl_ms) {
        if (conn != null && conn.isOpen()) {
            JSONObject o = new JSONObject();
            o.put("type", "text");
            o.put("message", message);
            o.put("ttl_ms", ttl_ms);
            conn.send(o.toString());
        }
    }

    /**
     * Enviar imagen solo a un cliente específico
     */
    public void sendImage(WebSocket conn, byte[] imageBytes, long ttl_ms, String name) {
        if (conn != null && conn.isOpen()) {
            JSONObject o = new JSONObject();
            o.put("type", "image");
            o.put("b64", Base64.getEncoder().encodeToString(imageBytes));
            o.put("ttl_ms", ttl_ms);
            o.put("name", name);
            conn.send(o.toString());
        }
    }
}
