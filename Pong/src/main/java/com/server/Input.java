package com.server;

public class Input {
    private String player;
    private int possY;

    public Input(String player, int possY){
        this.player=player;
        this.possY=possY;
    }

    public String getPlayer(){
        return player;
    }
    public int getPossY(){
        return possY;
    }

}
