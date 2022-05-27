package launchers;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import chen0040.rl.learning.qlearn.QLearner;
import ghosts.GhostRandom;
import mspacmans.Executor;
import pacman.controllers.GhostController;

/**
 * Class that loads a model (json) then train its more
 */
public class LoadAndTrainModel {
	public static void main(String[] args) {
		//Class that executes the game
		Executor executor = new Executor.Builder()
				.setTickLimit(500)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		//Load the model from json
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
		
		GhostController ghosts = new GhostRandom(); 	////The ghosts that we want to play against
		//Trains the loaded model the number of times that we pass in the third argument
		QLearner model = executor.runGameLoadQtrainPills(contentBuilder.toString(), ghosts, Integer.parseInt(args[1]));  	
		
		//Export the model in a json format
		try(PrintStream ps = new PrintStream(args[0])){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
