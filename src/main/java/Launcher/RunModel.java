package Launcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.github.chen0040.rl.learning.qlearn.QLearner;

import Others.Executor;
import Others.GhostRandom;
import Others.GhostsAggresive;
import pacman.controllers.GhostController;

public class RunModel {
public static void main(String[] args) {
        
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom();
		
		StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("model6Random.json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
		
        QLearner learner = QLearner.fromJson(contentBuilder.toString());
		System.out.println(executor.runGameQ7(learner, ghosts, 40));
	}
}
