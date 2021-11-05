package Launcher;
import java.io.FileNotFoundException;
import java.io.PrintStream;


import Others.Executor;
import Others.GhostRandom;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class TrainModel {

	public static void main(String[] args) {
		System.out.println(args[0]);
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom();
		QLearner model = executor.runGameQtrain7(ghosts, Integer.parseInt(args[1]));
		
		try(PrintStream ps = new PrintStream(args[0])){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
