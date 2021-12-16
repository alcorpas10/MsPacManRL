package Launcher;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import Others.Executor;
import Others.GhostRandom;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class TrainAlexModel {
	public static void main(String[] args) {
		Executor executor = new Executor.Builder()
				.setTickLimit(500)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom();
		QLearner model = executor.runGameQtrainAlex(ghosts, Integer.parseInt(args[1]));
		
		try(PrintStream ps = new PrintStream(args[0])){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
