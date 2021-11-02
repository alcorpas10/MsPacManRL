package Launcher;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import Others.Executor;
import Others.GhostsAggresive;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class LoadAndTrainModel {
	public static void main(String[] args) {
		Executor executor = new Executor.Builder()
				.setTickLimit(4000)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("model.json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
		
		GhostController ghosts = new GhostsAggresive();
		QLearner model = executor.runGameLoadQtrain4(contentBuilder.toString(), ghosts, 10000);
		
		try(PrintStream ps = new PrintStream("model.json")){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
