package com.server;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Arrays;

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
        JSONObject msg = new JSONObject();
        msg.put("type", "broadcast");
        msg.put("message", "Hola a todos!");
        broadcast(msg.toString());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String name = clients.remove(conn);
        System.out.println("Cliente desconectado: " + name);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Mensaje recibido: " + message);
        // mensajes futuros
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
}
