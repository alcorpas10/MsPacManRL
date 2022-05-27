package launchers;

import java.io.IOException;
import java.net.Socket;

import pacman.controllers.GhostController;
import ghosts.GhostNormal;
import mspacman.ExecutorDeepLearn;
import mspacman.MsPacMan;

/**
 * Class that trains the notEdible model a number of episodes 
 */
public class MainTrainNotEdible {
	public static void main(String[] args) {
		//Class that executes the game
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();

		try {
			Socket socket = new Socket("localhost", Integer.parseInt(args[0])); //Socket that connects with python at the given port in the first argument

			MsPacMan pacMan = new MsPacMan(socket, "", 1); //MsPacMan that receives its actions from the socket
			GhostController ghosts = new GhostNormal();   //The ghosts that we want to play against

			System.out.println(executor.runEpisodesTrainNotEdible(pacMan, ghosts, "Deep Learn")); //Trains the model a number of episodes 
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
