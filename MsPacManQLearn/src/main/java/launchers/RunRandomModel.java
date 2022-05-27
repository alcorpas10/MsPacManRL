package launchers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import chen0040.rl.learning.qlearn.QLearner;
import ghosts.GhostRandom;
import mspacmans.Executor;
import pacman.controllers.GhostController;

/**
 * Class that runs a loaded original model (json) in a randomly initialized game.
 */
public class RunRandomModel {
public static void main(String[] args) {
		//Class that executes the game
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom();  	//The ghosts that we want to play against
		
		//Loads the trained random model(json) 
		StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) { 
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
		
        QLearner learner = QLearner.fromJson(contentBuilder.toString());  	//Loads the model to the QLearner
		System.out.println(executor.runRandomQGame(learner, ghosts, 40));	//Runs and shows the game executing the trained model
	}
}
