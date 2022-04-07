package launchers;

import engine.pacman.controllers.GhostController;
import ghosts.GhostAggressive;
import ghosts.GhostAlgorithmic;
import ghosts.GhostRandom;
import pacman.ExecutorDeepLearn;


public class MainFSM {
	public static void main(String[] args) {

		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();

		GhostController ghosts = new GhostAlgorithmic();

		System.out.println(executor.runGameFSM(ghosts, 40));
	}
}