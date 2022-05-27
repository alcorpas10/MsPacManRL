package launchers;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import chen0040.rl.learning.qlearn.QLearner;
import ghosts.GhostAggressive;
import ghosts.GhostAlgorithmic;
import ghosts.GhostRandom;
import mspacmans.Executor;
import pacman.controllers.GhostController;


/**
 * Class that trains the original(algorithmic) model a number of episodes
 */
public class TrainOriginalModel {
	public static void main(String[] args) {
		//Class that executes the game
		Executor executor = new Executor.Builder()
				.setTickLimit(500)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostAlgorithmic(); //The ghosts that we want to play against
		QLearner model = executor.runGameQtrainOriginal(ghosts, Integer.parseInt(args[1])); //Trains the model a number of episodes (second argument)
		
		//Exports model in a json
		try(PrintStream ps = new PrintStream(args[0])){ //name given in the first argument from the command line
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
