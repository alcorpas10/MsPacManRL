package sockets;


import java.io.IOException;
import java.net.Socket;

import engine.pacman.controllers.GhostController;
import ghosts.GhostRandom;
import pacman.ExecutorDeepLearn;
import pacman.MsPacMan;

public class Main {
	
	public static void main(String[] args) {
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
	            .setTickLimit(400)
	            .setVisual(true)
	            .setScaleFactor(3.0)
	            .build();
		
		try {
			//for (int i = 0; i < 2; i++) {
				Socket socket = new Socket("localhost",38514);
				
				MsPacMan pacMan = new MsPacMan(socket);
		        GhostController ghosts = new GhostRandom();
		        
		        System.out.println(executor.runEpisodes(pacMan, ghosts, "Deep Learn"));
			//}
        } catch (IOException e1) {
            e1.printStackTrace();
        }
	}
}
