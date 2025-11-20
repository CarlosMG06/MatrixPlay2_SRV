package com.server;

import org.java_websocket.WebSocket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

public class PlayPong {

    private enum states{ 
        WAITING_START , ROUND_RUNNING , ROUND_END
    }


    private int playersReady = 0;

    private BlockingDeque<Input> playerInputs = new LinkedBlockingDeque<>();


    private states gameState = states.WAITING_START;

    private ScheduledExecutorService game;

    private String p1Name;
    private String p2Name;


    private int recHeight;
    private int recWitdh;

    private volatile int p1possY;
    private volatile int p2possY;

    
    private int p1possX;
    private int p2possX;

    private int p1Points;
    private int p2Points;


    private int ballSize;
    private int ballX;
    private int ballY;

    private double ballXDouble;
    private double ballYDouble;

    private double speedY;

    private double speedX;

    private double screenSize;

    public void closeGame(){
        game.shutdownNow();
    }

    public int getPlayersReady(){
        return playersReady;
    }

    public void setPlayersReady(int playersReady){
        this.playersReady= playersReady;
    }

    public void addPlayer(String name){
        if(p1Name.equals("")){
            p1Name=name;
            Main.clientsData.get(p1Name).player=1;
            return;
        }
        if(p2Name.equals("")){
            p2Name=name;
            Main.clientsData.get(p2Name).player=2;
        }
        
    }


    public void setPlayer1(String name){
        p1Name=name;

    }
    public void setPlayer2(String name){
        p2Name=name;
        
    }

    public void addInputs(JSONObject json){

        String player= json.optString(Missatges.C_NAME);
        int input = json.optInt(Missatges.C_INPUT);



        playerInputs.add(new Input(player, input));
    }

    

    public PlayPong(String p1Name, String p2Name){

        this.p1Name=p1Name;
        this.p2Name=p2Name;



        recHeight = 16;
        recWitdh = 3;

        
        p1possY=32;
        p2possY=32;

        p1possX=0;
        p2possX=61;

        p1Points=0;
        p2Points=0;

        ballX=32;
        ballY=32;
        ballSize = 2;

        ballXDouble=32f;
        ballYDouble=32f;

        speedY=1;
        speedX=2;
        
        screenSize = 64;

    }

    public void restartGame(){
        this.p1Name="";
        this.p2Name="";

        recHeight = 16;
        recWitdh = 3;
        
        p1possY=32;
        p2possY=32;

        p1possX=0;
        p2possX=61;

        p1Points=0;
        p2Points=0;

        ballX=32;
        ballY=32;
        ballSize = 2;

        ballXDouble = 32f;
        ballYDouble = 32f;

        speedY=0.7;
        speedX=1;
        
        screenSize = 64;

        playersReady= 0;
    }

    public PlayPong(){
        restartGame();
    }

    // public void gameCountDown(){
    //     countdownRunning = true;
        
    //     new Thread(() -> {
    //         try {

    //             JSONObject json= msg(Missatges.K_TYPE)
    //             .put(Missatges.K_TYPE, Missatges.INIT_COUNT_DOWN);

    //             broadcastExcept(null,json.toString());


    //             Thread.sleep(750);

    //             JSONObject jo = new JSONObject();
    //             int num = 1;

    //             for(String name : clientsData.keySet()){
                    
    //                 if(name.equals("raspberryClient")) { continue;}

    //                 System.out.println("agregando :"+name+" al countdown");

    //                 jo.put("player"+num, name);
    //                 num++;
    //             }



    //             for (int i = 5; i >= 0; i--) {
    //                 int raspberryCount = 0;

    //                 if(clients.socketByName("raspberryClient")!=null){
    //                     raspberryCount++;
    //                 }
    //                 // Si durant el compte enrere ja no hi ha els clients requerits, cancel·la
    //                 if (clients.snapshot().size()-raspberryCount < Missatges.REQUIRED_CLIENTS) {
    //                     System.out.println("se cerro XD");
    //                     break;
    //                 }
    //                 jo.put("msgCountDown", i);

    //                 sendCountdownToAll(jo);

    //                 if (i > 0) Thread.sleep(750); // ritme del compte enrere
    //             }
    //         } catch (InterruptedException ie) {
    //             Thread.currentThread().interrupt();
    //         } finally {
    //             countdownRunning = false;
    //         }
    //     }, "CountdownThread").start();
    // }

    public boolean isPlayer(WebSocket conn,String name){

        return name.equals(p1Name) || name.equals(p2Name);
    }


    public void tick(long ms){
        switch (gameState) {
            case WAITING_START:
                
                gameState= states.ROUND_RUNNING;
                //gameCountDown();
                break;
            
            case ROUND_RUNNING:

                processPlayersInputs();
                updateGame(ms);
                checkGoal();

                break;


            case ROUND_END:

                nextRound();
                
                break;




            default:
                break;
        }
    }

    public void nextRound(){
        ballXDouble=32f;
        ballX= 32;
        ballY=((int) (Math.random()*40))+10;
        ballYDouble=(long)ballY;

        speedX=1f;
        
        gameState= states.WAITING_START;
    }

    public void processPlayersInputs(){
        Input input;

        while ((input = playerInputs.poll()) != null) {

            //si la possY + lo que mide el rec pasa del camvas no se mueve
            if(input.getPossY()>(screenSize-recHeight)||(input.getPossY()<0)){
                    continue;
                }

            //primer player
            if(input.getPlayer().equals(p1Name)){
                p1possY=input.getPossY();
                Main.clientsData.get(p1Name).poss=p1possY;
                continue;
            }
            
            else{
            //segundo player
            p2possY=input.getPossY();
            Main.clientsData.get(p2Name).poss=p2possY;
            }
            
            
            
        }
    }

    public void checkGoal(){
        
        if (ballX == 0){
            p2Points++;
            Main.clientsData.get(p2Name).points++;
            gameState= states.ROUND_END;
            
            //System.out.println("gol P2 X="+ballX+"  Y="+ballY);
        }
        if(ballX+ballSize == screenSize-1){
            p1Points++;
            Main.clientsData.get(p1Name).points++;
            gameState= states.ROUND_END;
            
            //System.out.println("gol P1 X="+ballX+"  Y="+ballY);
        }
        
    }

    public boolean colisionArribaAbajo(){
        return ballY==0 || ballY+ballSize == screenSize-1;
    }

    public boolean colisionDerechaIzquiedaWithRect(){
        return ballX<recWitdh+1 || ballX+ballSize> screenSize-recWitdh-1;
    }


    public boolean colisionPlayers(){

            if(!colisionDerechaIzquiedaWithRect()){
                return false;
            }

            //balon en area de P1
            if(ballX<32){
                //p1 le da al balon
                if(p1possY+recHeight>ballY&& ballY>p1possY){
                    return true;
                }
            }

            else{
                //p2 le da al balon
                if(p2possY+recHeight>ballY&& ballY+ballSize>p2possY){
                    return true;
                }

            }

            return false;
    }

    public void updateGame(long ms){

        ballXDouble += speedX * (ms / 80.0);
        ballX= (int) ballXDouble;

        ballYDouble += speedY * (ms / 80.0);
        ballY= (int) ballYDouble;

        if(colisionPlayers()){
            //System.out.println("XrebotoX X="+ballX+"  Y="+ballY);
            speedX*=-1;

            if(ballX<32){
                speedX+=0.1;
            }else{
                speedX-=0.1;
            }
        }

        if(colisionArribaAbajo()){
            //System.out.println("YrebotoY X="+ballX+"  Y="+ballY);
            speedY*=-1;
        }

    }


     // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("p1PossY", p1possY);
        obj.put("p2PossY", p2possY);
        obj.put("p1Points", p1Points);
        obj.put("p2Points", p2Points);
        obj.put("ballX", ballX);
        obj.put("ballY", ballY);
        return obj;
    }

    public void setGameThread(){
        
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "PingPong");
            t.setDaemon(true);
            return t;
        };
        this.game = Executors.newSingleThreadScheduledExecutor(tf);

    }

    private boolean checkWinner(){
        
        if(p1Points>=Missatges.REQUIRED_POINTS_TO_WIN){
            UtilsLog.info(p1Name+" gano el juego"); 
            return true;}
        if(p2Points>=Missatges.REQUIRED_POINTS_TO_WIN){
            UtilsLog.info(p2Name+" gano el juego"); 
            return true;}
        return false;
    }

    public void startGame() {
        //System.out.println("game iniciado!!");
        setGameThread();

        long periodMs = Math.max(1, 1000 / Missatges.SEND_FPS);
        game.scheduleAtFixedRate(() -> {
            try {
                //System.out.println("dentro de game --------------------------------------------------------\n----------------------------------");
                tick(periodMs);
                //System.out.println("game fun");
                //System.out.println("X: "+ballX+" Y:"+ballY);
                if(checkWinner()){
                    UtilsLog.info("Juego acabado");
                    game.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, periodMs, TimeUnit.MILLISECONDS);
    }
    public void stopGame() {
        try {
            game.shutdownNow();
            game.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
