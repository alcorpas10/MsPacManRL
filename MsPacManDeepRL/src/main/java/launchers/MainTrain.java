package launchers;

import java.io.IOException;
import java.net.Socket;

import pacman.controllers.GhostController;
import ghosts.GhostNormal;
import mspacman.ExecutorDeepLearn;
import mspacman.MsPacMan;

public class MainTrain {
	public static void main(String[] args) {
		ExecutorDeepLearn executor = new ExecutorDeepLearn.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();

		try {
			Socket socket = new Socket("localhost", Integer.parseInt(args[0]));

			MsPacMan pacMan = new MsPacMan(socket, "", 0);
			GhostController ghosts = new GhostNormal();

			System.out.println(executor.runEpisodesTrain(pacMan, ghosts, "Deep Learn"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}
