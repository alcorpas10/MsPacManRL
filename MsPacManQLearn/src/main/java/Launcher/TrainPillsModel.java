package Launcher;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import Others.Executor;
import Others.GhostAggressive;
import Others.GhostAlgorithmic;
import Others.GhostRandom;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class TrainPillsModel {
	public static void main(String[] args) {
		Executor executor = new Executor.Builder()
				.setTickLimit(500)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostAlgorithmic();
		QLearner model = executor.runGameQtrainPills(ghosts, Integer.parseInt(args[1]));
		
		try(PrintStream ps = new PrintStream(args[0])){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
