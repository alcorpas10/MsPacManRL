package Launcher;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import Others.Executor;
import Others.GhostRandom;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.controllers.GhostController;

public class LoadAndTrainModel {
	public static void main(String[] args) {
		Executor executor = new Executor.Builder()
				.setTickLimit(500)
				.setVisual(true)
				.setScaleFactor(3.0)
				.build();
		
		StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
		
		GhostController ghosts = new GhostRandom();
		QLearner model = executor.runGameLoadQtrainAlex(contentBuilder.toString(), ghosts, Integer.parseInt(args[1]));
		
		try(PrintStream ps = new PrintStream(args[0])){
            ps.println(model.toJson());
            ps.flush();
            ps.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
	}
}
