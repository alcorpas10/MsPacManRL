package launchers;

import pacman.controllers.GhostController;
import ghosts.GhostNormal;
import mspacman.ExecutorDeepLearn;

public class MainFSM {
	public static void main(String[] args) {
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();

		GhostController ghosts = new GhostNormal();

		System.out.println(executor.runGameFSM(ghosts, 40));
	}
}