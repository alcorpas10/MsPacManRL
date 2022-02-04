package Launcher;

import Others.Executor;
import Others.GhostAggressive;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class MainQ {
	
	public static void main(String[] args) {
        
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostAggressive();
		QLearner model = executor.runGameQtrainRandom(ghosts, 100);
		
		System.out.println(executor.runGameQ(model, ghosts, 40));
	}
	
}
