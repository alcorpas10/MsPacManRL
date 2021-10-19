package Launcher;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import com.github.chen0040.rl.learning.qlearn.QLearner;

import Others.Executor;
import Others.GhostsAggresive;
import pacman.controllers.GhostController;

public class TrainModel {

	public static void main(String[] args) {
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		GhostController ghosts = new GhostsAggresive();
		QLearner model = executor.runGameQtrain4(ghosts, 1000);
		
		try(PrintStream ps = new PrintStream("model.json")){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}

}
