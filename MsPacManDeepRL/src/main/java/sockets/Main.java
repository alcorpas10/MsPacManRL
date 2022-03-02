package sockets;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import engine.pacman.Executor;
import engine.pacman.controllers.GhostController;
import engine.pacman.controllers.PacmanController;
import ghosts.GhostRandom;
import pacman.ExecutorDeepLearn;
import pacman.MsPacMan;

public class Main {
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
	            .setTickLimit(4000)
	            .setVisual(true)
	            .setScaleFactor(3.0)
	            .build();
		
		try {
			Socket socket = new Socket("localhost",38514);
			
			MsPacMan pacMan = new MsPacMan(socket);
	        GhostController ghosts = new GhostRandom();
	        
	        System.out.println(executor.runGame(pacMan, ghosts, 20));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
		
        
        

	}

}
