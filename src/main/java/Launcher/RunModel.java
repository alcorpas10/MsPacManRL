package Launcher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


import Others.Executor;
import Others.GhostRandom;
import chen0040.rl.learning.qlearn.QLearner;
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
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
		
        QLearner learner = QLearner.fromJson(contentBuilder.toString());
		System.out.println(executor.runGameQ(learner, ghosts, 40));
	}
}
