package sockets;


import java.io.IOException;
import java.net.Socket;

import engine.pacman.controllers.GhostController;
import ghosts.GhostAggressive;
import ghosts.GhostRandom;
import pacman.ExecutorDeepLearn;
import pacman.MsPacMan;

public class MainExecute {
	
	public static void main(String[] args) {
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
	            .setTickLimit(4000)
	            .setVisual(true)
	            .setScaleFactor(3.0)
	            .build();
		
		try {
			Socket socket = new Socket("localhost",38514);
			
			MsPacMan pacMan = new MsPacMan(socket);
	        GhostController ghosts = new GhostRandom();
	        
	        System.out.println(executor.runGame(pacMan, ghosts, 40));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
	}
}
