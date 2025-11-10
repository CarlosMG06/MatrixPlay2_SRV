package com.server;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONArray;
import org.json.JSONObject;
import com.shared.ClientData;
import com.shared.ClientPerfil;
import com.shared.GameObject;
import com.shared.Partida;

/**
 * Servidor WebSocket que manté l'estat complet dels clients i objectes seleccionables.
 *
 * Protocol simplificat:
 *  - Client -> Server:  { Missatges.K_TYPE: "clientData", "data": { ...ClientData... } }
 *  - Server -> Clients: { Missatges.K_TYPE: "state", "clientId": <clientId>, "clients": [ ...ClientData... ], "gameObjects": { ... }, "countdown": n? }
 */
public class Main extends WebSocketServer {

    /** Port per defecte on escolta el servidor. */
    public static final int DEFAULT_PORT = 3000;

    /** Llista de noms disponibles per als clients connectats. */
    private static final List<String> PLAYER_NAMES = Arrays.asList(
        "Bulbasaur", "Charizard", "Blaziken", "Umbreon", "Mewtwo", "Pikachu", "Wartortle"
    );

    /** Nombre de clients necessaris per iniciar el compte enrere. */
    private static final int REQUIRED_CLIENTS = 2;
    // Para mantener última jugada y ganador
    private String winnerName = "";
    private final List<Partida> partidas = new ArrayList<>();
    

  

    // Tipus de missatge nous i (alguns) heretats

    /** Registre de clients i assignació de noms (pool integrat). */
    private final GameHandler jocHandler;
    private final ServerUtils serverUtils;


    /** Mapa d’estat per client (source of truth del servidor). Clau = name/id. */
    private final Map<String, ClientData> clientsData = new HashMap<>();

    /** Mapa d'objectes seleccionables compartits. */
    private final Map<String, GameObject> gameObjects = new HashMap<>();

    private volatile boolean countdownRunning = false;

    /** Freqüència d’enviament de l’estat (frames per segon). */
    private static final int SEND_FPS = 30;
    private final ScheduledExecutorService ticker;
    private final List<ClientPerfil> perfils = new ArrayList<>();
    /**
     * Crea un servidor WebSocket que escolta a l'adreça indicada.
     *
     * @param address adreça i port d'escolta del servidor
     */
    public Main(InetSocketAddress address) {
        super(address);
        this.clients = new ClientRegistry(PLAYER_NAMES);
        
        serverUtils = new ServerUtils(this, clients, clientsData, perfils, partidas);
        jocHandler = new GameHandler(clientsData, clients, partidas, perfils, this,serverUtils);

        crearClientHandler = new CrearClientHandler(perfils, clients, clientsData, this,serverUtils);

        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "ServerTicker");
            t.setDaemon(true);
            return t;
        };
        this.ticker = Executors.newSingleThreadScheduledExecutor(tf);
    }

    /**
     * Inicialitza els objectes seleccionables predefinits.
     */
    public void initializegameObjects(Partida p) {
        resetBoard(p);
        drawFitxes(p);
    }


    private void resetBoard(Partida p) {
        for (int r = 0; r < 6; r++) {
            Arrays.fill(p.board[r], "");
        }
        p.lastRow = -1;
        p.lastCol = -1;
        p.winnerName = "";
        p.currentPlayer = 1; // <<--- Cambiar
    }

    private void drawFitxes(Partida p) { // Cambiar todo
        p.gameObjects.clear();
        int numPiecesPerPlayer = 21;
        int radius = 30;
        int startY = 50;    
        int spacing = 70;   

        int xRed1 = 625, xRed2 = 695;       
        int xYellow1 = 775, xYellow2 = 845; 
        int piecesPerColumn = (int)Math.ceil(numPiecesPerPlayer / 2.0);

        // --- Fichas rojas ---
        for (int i = 0; i < numPiecesPerPlayer; i++) {
            String objId = "R" + i;
            int col = (i < piecesPerColumn) ? 0 : 1;
            int y = startY + (i % piecesPerColumn) * spacing;
            int x = (col == 0) ? xRed1 : xRed2;

            GameObject piece = new GameObject(objId, x, y, 1, 1, radius);
            piece.color = "red";
            piece.isPiece = true;
            piece.startX = x;
            piece.startY = y;
            p.gameObjects.put(objId, piece);
        }

        // --- Fichas amarillas ---
        for (int i = 0; i < numPiecesPerPlayer; i++) {
            String objId = "Y" + i;
            int col = (i < piecesPerColumn) ? 0 : 1;
            int y = startY + (i % piecesPerColumn) * spacing;
            int x = (col == 0) ? xYellow1 : xYellow2;

            GameObject piece = new GameObject(objId, x, y, 1, 1, radius);
            piece.color = "yellow";
            piece.isPiece = true;
            piece.startX = x;
            piece.startY = y;
            p.gameObjects.put(objId, piece);
        }
    }

    
    /**
     * Obté el color per un nom de client.
     *
     * @return color assignat
     */


    /** Envia un compte enrere (5..0) com a part del mateix STATE.
     *  Evita comptes simultanis i es cancel·la si baixa el nombre de clients. */
    public void sendCountdown(WebSocket jugador1,WebSocket jugador2) {

        new Thread(() -> {
            try {
                for (int i = 5; i >= 0; i--) {
                    // Si durant el compte enrere ja no hi ha els clients requerits, cancel·la
                    if (!clients.snapshot().keySet().contains(jugador1) || !clients.snapshot().containsKey(jugador2)) {
                        break;
                    }

                    sendCountdownToAll(i,jugador1,jugador2);
                    if (i > 0) Thread.sleep(750); // ritme del compte enrere
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            } finally {
                countdownRunning = false;
            }
        }, "CountdownThread").start();
    }

    // ----------------- Helpers JSON -----------------

    /** Crea un objecte JSON amb el camp type inicialitzat. */
    private static JSONObject msg(String type) {
        return new JSONObject().put(Missatges.K_TYPE, type);
    }


    /** Envia a tots els clients el compte enrere. */
    private void sendCountdownToAll(int n,WebSocket jugador1,WebSocket jugador2) {
        JSONObject rst = msg(Missatges.COUNTDOWN).put(Missatges.K_VALUE, n);
        serverUtils.sendSafe(jugador1, rst.toString());
        serverUtils.sendSafe(jugador2, rst.toString());
    }

    // ----------------- WebSocketServer overrides -----------------

    /** Assigna un nom i color al client i envia l’STATE complet. */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

        System.out.println("Cliente conectado: " + conn.getRemoteSocketAddress());

        
    }

    /** Elimina el client del registre i envia l’STATE complet. */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        Partida partida = serverUtils.getPartidaByJugador(conn);
        String name = clients.remove(conn);
        clientsData.remove(name);
        System.out.println("WebSocket client disconnected: " + name);

        perfils.removeIf(p -> p.name.equals(name));

        partidas.removeIf(p -> Objects.equals(p.jugador1, conn) || Objects.equals(p.jugador2, conn));

        JSONArray arrPerfils = new JSONArray();
        for (ClientPerfil p : perfils) {
            arrPerfils.put(p.toJSON());
        }
        JSONObject llistaPerfils = new JSONObject();
        llistaPerfils.put(Missatges.K_TYPE, Missatges.LLISTA_PERFILS);
        llistaPerfils.put(Missatges.K_VALUE, arrPerfils);
        broadcast(llistaPerfils.toString());

        if (clients.snapshot().isEmpty()) {
            partidas.clear();

            JSONObject msg = new JSONObject();
            msg.put(Missatges.K_TYPE, Missatges.PARTIDA_FINALITZADA);
            msg.put("guanyador", "cap");
            broadcast(msg.toString());
        } else {
            ClientData nextPlayer = clientsData.values().stream().findFirst().orElse(null);
            if (nextPlayer != null) {
                partida.currentPlayer = nextPlayer.color.equalsIgnoreCase("RED") ? 1 : 2;

                JSONObject msgTorn = new JSONObject();
                msgTorn.put(Missatges.K_TYPE, Missatges.TORN_ACTUALIZTAT);
                msgTorn.put("player", nextPlayer.name);
                msgTorn.put("color", nextPlayer.color);
                broadcast(msgTorn.toString());
            }
        }
    }



    public void onMessage(WebSocket conn, String message) {
            System.out.println("Mensaje recibido: " + message);
            printConnectedClients();

            JSONObject obj;
            try {
                obj = new JSONObject(message);
            } catch (Exception ex) {
                return; // JSON inválido
            }

            String type = obj.optString(Missatges.K_TYPE, "");
            switch (type) {
                case Missatges.CLIENT_MOUSE_MOVING -> jocHandler.handleClientMouseMoving(conn, obj);
                case Missatges.T_NOU_PERFIL -> crearClientHandler.handleNouPerfil(obj);
                case Missatges.T_PETICIO_SELECT -> jocHandler.handlePeticioSelect(conn, obj,serverUtils.getPartidaByJugador(conn));
                case Missatges.CLIENT_OBJECT_MOVING -> jocHandler.handleClientObjectMoving(conn, obj,serverUtils.getPartidaByJugador(conn));
                case Missatges.T_CLIENT_SET_NAME -> crearClientHandler.handleClientSetName(conn, obj);
                case Missatges.T_COLOCAR_FITXA -> jocHandler.handleColocarFitxa(conn, obj,serverUtils.getPartidaByJugador(conn));
                case Missatges.T_INVITACIO -> invitacioHandler.handleInvitacion(conn, obj);
                case Missatges.T_ACCEPT_INVITACIO -> invitacioHandler.handleAcceptInvitation(obj);
                default -> {

                }
            }
        }
       


    /** Log d'error global o de socket concret. */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
    }

    /** Arrencada: log i configuració del timeout de connexió perduda. */
    @Override
    public void onStart() {
        System.out.println("WebSocket server started on port: " + getPort());
        setConnectionLostTimeout(100);
        startTicker();
    }

    // ----------------- Lifecycle util -----------------

    /** Registra un shutdown hook per aturar netament el servidor en finalitzar el procés. */
    private static void registerShutdownHook(Main server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Aturant servidor (shutdown hook)...");
            try {
                server.stopTicker();      // <- atura el bucle periòdic
                server.stop(1000);
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


    // ----------------- Ticker util -----------------

    private void startTicker() {
        long periodMs = Math.max(1, 1000 / SEND_FPS);
        ticker.scheduleAtFixedRate(() -> {
            try {
                for (Partida p : partidas) {
                    // Solo si hay jugadores conectados
                    if (p.jugador1 != null && p.jugador2 != null) {
                        serverUtils.broadcastStatus(p); // Envía estado solo a los jugadores de esa partida
                    }
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


    private void printConnectedClients() {
    // Assuming clients.snapshot() returns a Map<WebSocket, String> where String is the client ID
    for (Map.Entry<WebSocket, String> e : clients.snapshot().entrySet()) {
        System.out.println("Client ID: " + e.getValue() + " connected.");
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
}
