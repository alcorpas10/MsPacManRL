package launchers;

import java.io.IOException;
import java.net.Socket;

import pacman.controllers.GhostController;
import ghosts.GhostNormal;
import mspacman.ExecutorDeepLearn;
import mspacman.MsPacMan;

/**
 * Class that executes the game connecting it with python using a socket.
 */
public class MainExecute {
	public static void main(String[] args) {
		//Class that executes the game
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
	            .setTickLimit(4000)
	            .setVisual(true)
	            .setScaleFactor(3.0)
	            .build();
		
		try {
			Socket socket = new Socket("localhost", 38514);  //Socket that connects with python at the given port
			//MsPacMan that receives its actions from the socket
			MsPacMan pacMan = new MsPacMan(socket, "", 0);   
	        GhostController ghosts = new GhostNormal();		//The ghosts that we want to play against
	        
	        System.out.println(executor.runGame(pacMan, ghosts, 40));  //Shows and runs the game 
        } catch (IOException e1) {
            e1.printStackTrace();
        }
	}
}
