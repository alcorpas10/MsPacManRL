package Launcher;

import Others.Executor;
import Others.GhostsAggresive;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class MainQ {
	
	public static void main(String[] args) {
        
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostsAggresive();
		QLearner model = executor.runGameQtrain5(ghosts, 100);
		
		System.out.println(executor.runGameQ(model, ghosts, 40));
	}
	
}
