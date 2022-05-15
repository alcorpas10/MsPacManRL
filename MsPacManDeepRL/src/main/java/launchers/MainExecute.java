package launchers;

import java.io.IOException;
import java.net.Socket;

import pacman.controllers.GhostController;
import ghosts.GhostNormal;
import mspacman.ExecutorDeepLearn;
import mspacman.MsPacMan;

public class MainExecute {
	public static void main(String[] args) {
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
	            .setTickLimit(4000)
	            .setVisual(true)
	            .setScaleFactor(3.0)
	            .build();
		
		try {
			Socket socket = new Socket("localhost", 38514);
			
			MsPacMan pacMan = new MsPacMan(socket, "", 2);
	        GhostController ghosts = new GhostNormal();
	        
	        System.out.println(executor.runGame(pacMan, ghosts, 40));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
	}
}
