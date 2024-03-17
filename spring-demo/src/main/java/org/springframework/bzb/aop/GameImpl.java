package org.springframework.bzb.aop;

interface Game {

	void playGame();

}

public class GameImpl implements Game {

	@Override
	public void playGame() {
		System.out.println("play game");
	}
}