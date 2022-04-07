package launchers;


import java.io.IOException;
import java.net.Socket;

import engine.pacman.controllers.GhostController;
import ghosts.GhostAggressive;
import ghosts.GhostAlgorithmic;
import ghosts.GhostRandom;
import pacman.ExecutorDeepLearn;
import pacman.MsPacMan;

public class MainTrain {
	
	public static void main(String[] args) {
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
	            .setTickLimit(250)
	            .setVisual(true)
	            .setScaleFactor(3.0)
	            .build();
		
		try {
			
			Socket socket = new Socket("localhost", Integer.parseInt(args[0]));
			
			MsPacMan pacMan = new MsPacMan(socket, "");
	        GhostController ghosts = new GhostAlgorithmic();
	        
	        System.out.println(executor.runEpisodesTrainEdible(pacMan, ghosts, "Deep Learn"));
			
        } catch (IOException e1) {
            e1.printStackTrace();
        }
	}
}
