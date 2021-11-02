package Launcher;
import java.io.FileNotFoundException;
import java.io.PrintStream;


import Others.Executor;
import Others.GhostRandom;
import Others.GhostsAggresive;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class TrainModel {

	public static void main(String[] args) {
		String json = "model7.json";
		System.out.println(json);
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom();
		QLearner model = executor.runGameQtrain7(ghosts, 10);
		
		try(PrintStream ps = new PrintStream(json)){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
