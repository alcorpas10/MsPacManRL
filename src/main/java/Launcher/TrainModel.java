package Launcher;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import com.github.chen0040.rl.learning.qlearn.QLearner;

import Others.Executor;
import Others.GhostRandom;
import Others.GhostsAggresive;
import pacman.controllers.GhostController;

public class TrainModel {

	public static void main(String[] args) {
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostRandom();
		QLearner model = executor.runGameQtrain6(ghosts, 100000);
		
		try(PrintStream ps = new PrintStream("model6Random.json")){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}

}
