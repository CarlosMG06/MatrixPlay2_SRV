package com.server;

import com.server.Missatges;

import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main extends WebSocketServer {


    private boolean countdownRunning = false;
    public final ClientRegistry clients;

    /** Mapa d’estat per client (source of truth del servidor). Clau = name/id. */
    public static final Map<String, ClientData> clientsData = new HashMap<>();

    public final PlayPong gameData;

    private static final List<String> PLAYER_NAMES = Arrays.asList(
        "Bulbasaur", "Charizard", "Blaziken", "Umbreon", "Mewtwo", "Pikachu", "Wartortle"
    );

    public static final int DEFAULT_PORT = 3000;
    
    private final ScheduledExecutorService ticker;


    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(PLAYER_NAMES);
        gameData = new PlayPong();
        gameData.setGameEndListener(winnerName -> onGameEnd(winnerName));
        
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "ServerTicker");
            t.setDaemon(true);
            return t;
        };
        this.ticker = Executors.newSingleThreadScheduledExecutor(tf);
    }
    

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Asignar un nombre aleatorio del pool
        // String name = clients.add(conn, "Player" + (int)(Math.random() * 1000));
        // System.out.println("Cliente conectado: " + name);

        // Enviar broadcast de bienvenida a todos los clientes
        // JSONObject msg = new JSONObject();
        // msg.put("type", "broadcast");
        // msg.put("message", "Hola a todos!");
        // broadcast(msg.toString());

        getName(conn);
        logDB("nueva coneccion.");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        clientsData.remove(clients.nameBySocket(conn));
        String name = clients.remove(conn);
        if (gameData != null && gameData.isPlayer(conn,name)) {
            gameData.closeGame();
        }
        logDB(name + "se desconecto.");
        //System.out.println("Cliente desconectado: " + name);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //System.out.println("Mensaje recibido: " + message);
        // mensajes futuros

        JSONObject obj;
        try {
            obj = new JSONObject(message);
        } catch (Exception ex) {
            return; // JSON invàlid
        }

        String type = obj.optString(Missatges.K_TYPE, "");
        switch (type) {

            case Missatges.CHECK_NAME :
                
                Boolean used = false;

                String clientName = obj.getString(Missatges.K_VALUE);
                JSONArray namesUsed = clients.currentNames();

                //System.out.println("Nombres actuales en el server: "+namesUsed.toString());
                //System.out.println("entro "+clientName);

                //comprueba si el nombre esta usado = true
                for (int i = 0; i<namesUsed.length();i++){
                    //System.out.println("nombre usado:  "+namesUsed.get(i));
                        if(clientName.equals(namesUsed.get(i))){
                            
                            //System.out.println(clientName+ " ya usado!!");
                            used=true;
                        }
                }

                //envia si el nombre esta disponible o usado

                JSONObject jo = msg(Missatges.CHECK_NAME_STATUS);
                
                if(used){

                    jo.put(Missatges.K_VALUE, Missatges.K_NAME_USED);
                    

                }else{
                    jo.put(Missatges.K_VALUE, Missatges.K_NAME_AVAILABLE);
                    
                    // Si es la Raspberry, la marcamos como especial
                    if ("raspberryClient".equals(clientName)) {
                        //System.out.println("Raspberry identificada!");
                        logDB(clientName+" conectado");
                        sendRaspberryConfig(conn);
                        sendTextToRaspberry("¡Hola Raspberry! Conexión OK.");
                    }else {
                        clientsData.put(clientName,new ClientData(clientName));
                    }
                    clients.add(conn,clientName);
                    //System.out.println("Cliente conectado: " + clientName);
                    logDB(clientName+" añadido a clientData (jugadores)");
                }

                
                sendSafe(conn, jo.toString());
                
                //si esta en uso desconecta al usuario
                if(used){conn.close(); break;}
                
                
                //sendCountdown();
                break;



            case Missatges.C_WAITING_COUNTDOWN :
                WebSocket ws = clients.socketByName("raspberryClient");
                if(ws!=null&&clientsData.size()==1){
                    JSONObject jmsg = msg(Missatges.R_WAITING_SCREEN);
                    sendSafe(ws,jmsg.toString());
                }
                

                sendCountdown();
                break;

            case Missatges.C_READY_STARTGAME :
                gameData.setPlayersReady(gameData.getPlayersReady()+1);
                
                if(gameData.getPlayersReady()==2){
                    logDB("Juego empezado");
                    gameData.startGame();
                }
                break;
                

            case Missatges.C_EXIT :
                String name = clients.remove(conn);
                clientsData.remove(name);
                break;


            case Missatges.C_MOVE :
                JSONObject json = obj.optJSONObject(Missatges.K_VALUE);
                //System.out.println(json.toString());
                gameData.addInputs(json);
                break;

            case Missatges.C_PLAY_AGAIN :
                clientName = obj.getString(Missatges.K_VALUE);
                clientsData.put(clientName,new ClientData(clientName));
                logDB(clientName+" quiere jugar de nuevo.");

                break;
        }
    }

    private void sendRaspberryConfig(WebSocket rpi) {
        JSONObject config = new JSONObject();
        config.put("type", "config");
        config.put("groupName", "matrixplay2");
        config.put("url", "wss://matrixplay2.ieti.site:443");
        sendSafe(rpi, config.toString());
    }
    private void sendGameEventToRaspberry(String event, JSONObject extra) {
        WebSocket rpi = clients.socketByName("raspberryClient");
        if (rpi == null) return;

        JSONObject msg = new JSONObject();
        msg.put("type", "jocData");
        msg.put("estatPartida", event);
        if (extra != null) {
            for(String key : extra.keySet()) {
                msg.put(key, extra.get(key));
            }
        }
        sendSafe(rpi, msg.toString());
    }



    /** Envía un mensaje de texto a **todos los clientes**, incluyendo Raspberry */
    public void broadcastTextToAll(String text, long ttlMs) {
        JSONObject payload = msg("text")
                .put("message", text)
                .put("ttl_ms", ttlMs);
        for (WebSocket ws : clients.snapshot().keySet()) {
            sendSafe(ws, payload.toString());
        }
    }

    /** Envía un mensaje solo a la Raspberry */
    public void sendTextToRaspberry(String text) {
        WebSocket rpi = clients.socketByName("raspberryClient");
        if (rpi == null) return;

        JSONObject payload = msg("text")
                .put("message", text)
                .put("ttl_ms", 5000);  // 5 segundos de duración
        sendSafe(rpi, payload.toString());
    }


    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
            try {
            GestioDB.crearDB();          // Crea el archivo y la tabla si no existen
            GestioDB.iniciarConnexio();  // Abre la conexión SQLite
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error inicializando la BD: " + e.getMessage());
        }

        logDB("Servidor WebSocket iniciado en puerto " + getPort());
        System.out.println("Servidor WebSocket iniciado en puerto " + getPort());
        setConnectionLostTimeout(100);
        startTicker();
    }

    
    // pide al cliente un nombre para verificar si ya existe
    public void getName(WebSocket ws){
        JSONObject jo = msg(Missatges.K_GET_NAME);
        sendSafe(ws, jo.toString());
        
    }


    
    // ----------------- Lifecycle util -----------------

    /** Registra un shutdown hook per aturar netament el servidor en finalitzar el procés. */
    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturando servidor...");
            try {
                server.stopTicker();
                server.stop(1000);
                GestioDB.tancarConnexio(); // <--- Cierra la conexión SQLite
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
            System.out.println("Servidor aturat.");
        }));
    }


    /** Bloqueja el fil principal indefinidament fins que sigui interromput. */
    private static void awaitForever() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void broadcastRoundCountdown(){
        if(!gameData.isRoundCountdownRunning()){return;}

        JSONObject json = msg(Missatges.INIT_ROUND_COUNT_DOWN)
        .put(Missatges.K_VALUE,gameData.getCountDown());
        System.out.println(json.toString());
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            WebSocket conn = e.getKey();
            sendSafe(conn, json.toString());
        }

    }

    private void broadcastStatus() {

        JSONArray arrClients = new JSONArray();
        for (ClientData c : clientsData.values()) {
            arrClients.put(c.toJSON());
        }

        JSONObject arrGameData = gameData.toJSON();
        
        

        JSONObject rst = msg(Missatges.T_SERVER_DATA)
                        .put(Missatges.K_CLIENTS_LIST, arrClients)
                        .put(Missatges.K_GAME_DATA, arrGameData);

        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            WebSocket conn = e.getKey();
            String name = clients.nameBySocket(conn);
            rst.put(Missatges.K_CLIENT_NAME, name);
            sendSafe(conn, rst.toString());
        }
    }

    /** Envia de forma segura un payload i, si el socket no està connectat, el neteja del registre. */
    private void sendSafe(WebSocket to, String payload) {

        //System.out.println(payload);

        if (to == null) return;
        try {
            to.send(payload);
        } catch (WebsocketNotConnectedException e) {
            String name = clients.cleanupDisconnected(to);
            clientsData.remove(name);
            logDB("Client desconnectat durant send: " + name);
            System.out.println("Client desconnectat durant send: " + name);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JSONObject msg(String type) {
        return new JSONObject().put(Missatges.K_TYPE, type);
    }

    // ----------------- Ticker util -----------------

    private void startTicker() {

        long periodMs = Math.max(1, 1000 / Missatges.SEND_FPS);
        ticker.scheduleAtFixedRate(() -> {
            try {
                // Opcional: si no hi ha clients, evita enviar
                if (clients.snapshot().size()>1) {
                    broadcastStatus();
                    broadcastRoundCountdown();
                }

                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }

    private void stopTicker() {
        try {
            ticker.shutdownNow();
            ticker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Punt d'entrada. */
    public static void main(String[] args) {
        Main server = new Main(new InetSocketAddress(DEFAULT_PORT));
        server.start();
        registerShutdownHook(server);

        System.out.println("Server running on port " + DEFAULT_PORT + ". Press Ctrl+C to stop it.");
        awaitForever();
    }

    /** Envia a tots els clients el compte enrere. */
    private void sendCountdownToAll(JSONObject jo) {

        //System.out.println("Enviando countdown");
        JSONObject rst = msg(Missatges.COUNTDOWN).put(Missatges.K_VALUE, jo);
        //System.out.println(rst.toString());
        broadcastExcept(null, rst.toString());
    }


    /** Envia un missatge a tots els clients excepte l'emissor. */
    public void broadcastExcept(WebSocket sender, String payload) {
        //System.out.println(payload);
        for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
            WebSocket conn = e.getKey();
            if (!Objects.equals(conn, sender)) sendSafe(conn, payload);
        }
    }

    /** Envia un compte enrere (5..0) com a part del mateix STATE.
     *  Evita comptes simultanis i es cancel·la si baixa el nombre de clients. */
    private void sendCountdown() {


        synchronized (this) {

            
            if (countdownRunning) return;
            
            //System.out.println("countdown iniciado!");

            if (clientsData.size() < Missatges.REQUIRED_CLIENTS) return;

            //System.out.println("paso el return");

            countdownRunning = true;
        }

        new Thread(() -> {
            try {

                gameData.restartGameData();

                JSONObject json= msg(Missatges.K_TYPE)
                .put(Missatges.K_TYPE, Missatges.INIT_COUNT_DOWN);

                broadcastExcept(null,json.toString());


                Thread.sleep(750);

                JSONObject jo = new JSONObject();
                int num = 1;

                for(String name : clientsData.keySet()){
                    
                    if(name.equals("raspberryClient")) { continue;}

                    //System.out.println("agregando :"+name+" al countdown");

                    jo.put("player"+num, name);

                    gameData.addPlayer(name);
                    num++;
                }



                for (int i = 5; i >= 0; i--) {
                    int raspberryCount = 0;

                    if(clients.socketByName("raspberryClient")!=null){
                        raspberryCount++;
                    }
                    // Si durant el compte enrere ja no hi ha els clients requerits, cancel·la
                    if (clients.snapshot().size()-raspberryCount < Missatges.REQUIRED_CLIENTS) {
                        //System.out.println("se cerro XD");
                        break;
                    }
                    jo.put("msgCountDown", i);

                    sendCountdownToAll(jo);

                    if (i > 0) Thread.sleep(750); // ritme del compte enrere
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                countdownRunning = false;
            }
        }, "CountdownThread").start();
    }

    private void onGameEnd(String winnerName) {
        clientsData.clear();
        
        logDB("clientsData size:" + clientsData.size());

        JSONObject msg = msg(Missatges.T_WINNER)
            .put(Missatges.K_VALUE, winnerName);
        broadcastExcept(null, msg.toString());
    }

    private void logDB(String msg) {
        try {
            String now = java.time.LocalDateTime.now().toString();
            GestioDB.afegeixEntradaLog(msg);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
