package launchers;

import chen0040.rl.learning.qlearn.QLearner;
import ghosts.GhostAggressive;
import mspacmans.Executor;
import pacman.controllers.GhostController;

/**
 * Class that trains a model then runs it
 */
public class MainQ {
	
	public static void main(String[] args) {
		//Class that executes the game
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostAggressive();			//The ghosts that we want to play against
		QLearner model = executor.runGameQtrainRandom(ghosts, 100); 	//Trains the model a number of episodes (second argument )
		
		System.out.println(executor.runGameQ(model, ghosts, 40));		//Runs and shows the game executing the trained model
	}
	
}
