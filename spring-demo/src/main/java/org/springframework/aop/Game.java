package org.springframework.aop;

public interface Game {
 
    void playGame();
 
}
 
class GameImpl implements Game{
 
    @Override
    public void playGame(){
        System.out.println("play game");
    }
}