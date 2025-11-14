package com.server;

import org.json.JSONObject;

public class PlayPong {

    private String p1Name;
    private String p2Name;

    private int recHeight;
    private int recWitdh;

    private int p1poss;
    private int p2poss;

    private int p1possX;
    private int p2possX;

    private int p1Points;
    private int p2Points;



    private int ballX;
    private int ballY;

    private double angle;

    private double speed;

    private double screenSize;

    public void addPlayer(String name){
        if(p1Name.equals("")){
            p1Name=name;
            return;
        }
        if(p2Name.equals("")){
            p2Name=name;
        }
        
    }
    

    public PlayPong(String p1Name, String p2Name){

        this.p1Name=p1Name;
        this.p2Name=p2Name;

        recHeight = 16;
        recWitdh = 3;
        
        p1poss=32;
        p2poss=32;

        p1possX=0;
        p2possX=64;

        p1Points=0;
        p2Points=0;

        ballX=32;
        ballY=32;

        angle=45;
        speed=1;
        
        screenSize = 64;

    }

    public PlayPong(){

        this.p1Name="";
        this.p2Name="";

        recHeight = 16;
        recWitdh = 3;
        
        p1poss=32;
        p2poss=32;

        p1possX=0;
        p2possX=64;

        p1Points=0;
        p2Points=0;

        ballX=32;
        ballY=32;

        angle=45;
        speed=1;
        
        screenSize = 64;

    }


    public void tick(){
        ballX+=speed;
        ballY+=angle;
    }

    public void p1Move(int move){
        if(p1poss+(recHeight*0.5)+move>screenSize){ return;}
        if(p1poss+(recHeight*0.5)+move<0){ return;}
        p1poss+=move;
    }
    public void p2Move(int move){
        if(p2poss+(recHeight*0.5)+move>screenSize){ return;}
        if(p2poss+(recHeight*0.5)+move<0){ return;}
        p2poss+=move;
    }

    public void p1Score(int move){
        p1Points++;
    }
    public void p2Score(int move){
        p2Points++;
    }

    public void setPlayPong(JSONObject obj){
        
        this.p1poss=obj.optInt("p1poss");
        this.p2poss=obj.optInt("p2poss");

        this.p1Points=obj.optInt("p1Points");
        this.p2Points=obj.optInt("p2Points");

        ballX=obj.optInt("ballX");
        ballY=obj.optInt("ballY");

        angle=obj.optDouble("angle");
        speed=obj.optDouble("speed");
        

    }


     // Converteix l'objecte a JSON
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("p1poss", p1poss);
        obj.put("p2poss", p2poss);
        obj.put("p1Points", p1Points);
        obj.put("p2Points", p2Points);
        obj.put("ballX", ballX);
        obj.put("ballY", ballY);
        obj.put("angle", angle);
        obj.put("speed", speed);
        return obj;
    }




}
