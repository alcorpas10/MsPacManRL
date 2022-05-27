package launchers;

import ghosts.GhostRandom;
import mspacmans.Executor;
import pacman.controllers.GhostController;

/**
 * Class that runs the game with a fsm 
 */
public class RunFsmModel {
	public static void main(String[] args) {
		//Class that executes the game
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom(); 	//The ghosts that we want to play against
		

		System.out.println(executor.runGameFSM(ghosts, 40)); //Runs and shows the game executing the FSM
	} 
}
