package Launcher;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import Others.Executor;
import Others.GhostAggressive;
import Others.GhostAlgorithmic;
import Others.GhostRandom;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

/**
 * Class that trains the chase model a number of episodes
 */
public class TrainChaseModel {
	public static void main(String[] args) {
		//Class that executes the game
		Executor executor = new Executor.Builder()
				.setTickLimit(500)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostAlgorithmic();  		//The ghosts that we want to play against
		QLearner model = executor.runGameQtrainChase(ghosts, Integer.parseInt(args[1]));	//Trains the model a number of episodes (second argument )
		
		//Exports model in a json
		try(PrintStream ps = new PrintStream(args[0])){ 	//name given in the first argument from the command line
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
