package launchers;

import pacman.controllers.GhostController;
import ghosts.GhostNormal;
import mspacman.ExecutorDeepLearn;


/**
 * Class that executes the game using a FSM, connecting it with python using a socket.
 */
public class MainFSM {
	public static void main(String[] args) {
		//Class that executes the game
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();

		GhostController ghosts = new GhostNormal();  //The ghosts that we want to play against

		System.out.println(executor.runGameFSM(ghosts, 40)); //Shows and runs the FSM game. 
	}
}