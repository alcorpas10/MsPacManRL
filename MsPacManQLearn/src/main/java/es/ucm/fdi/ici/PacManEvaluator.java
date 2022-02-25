package es.ucm.fdi.ici;
/**
 * PacManEvaluator
 * Clase para evaluar las entregas de MsPacMan
 * Ingeniería de Comportamientos Inteligentes
 * @author Juan A. Recio-Garcia
 */


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

import pacman.Executor;
import pacman.controllers.Controller;
import pacman.controllers.GhostController;
import pacman.controllers.PacmanController;
import pacman.game.util.Stats;

public class PacManEvaluator {
	
	private static final String CONFIG_FILE = "config.properties";
	
	public static final String KEY_TRIALS = "trials";
	public static final String KEY_MsPACMAN_TEAMS = "MsPacManTeams";
	public static final String KEY_GHOSTS_TEAMS = "GhostsTeams";
	public static final String KEY_PO = "po";
	public static final String KEY_PO_SIGHT_LIMIT = "sightLimit";	
	public static final String KEY_TICKS_LIMIT = "ticks";
	public static final String KEY_TIME_LIMIT = "time";
	
	
	//ONLY FOR CBR
	public static final int LIMIT = 2000;

	private Executor executor;
	private Properties properties;
	private Vector<PacmanController> list_pacMan; 
	private Vector<GhostController> list_ghosts;
	private Scores scores;

	void setDefaultProperites()
	{
		properties = new Properties();
		properties.setProperty(KEY_PO, "false");
		properties.setProperty(KEY_TRIALS, "100");
		properties.setProperty(KEY_TICKS_LIMIT, "4000");
		properties.setProperty(KEY_TIME_LIMIT, "40");
		properties.setProperty(KEY_PO_SIGHT_LIMIT, "50");
	}
	
	void loadConfig() throws IOException
	{
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);

		if (inputStream != null) {
			properties.load(inputStream);
			inputStream.close();
		} else {
			throw new FileNotFoundException("property file '" + CONFIG_FILE + "' not found in the classpath");
		}
		
		
	}
	
	void configureExecutor()
	{
		Boolean po = properties.getProperty(KEY_PO).equalsIgnoreCase("true");
		Integer sightLimit =  Integer.parseInt(properties.getProperty(KEY_PO_SIGHT_LIMIT));
		Integer ticks = Integer.parseInt(properties.getProperty(KEY_TICKS_LIMIT));
		Integer time =  Integer.parseInt(properties.getProperty(KEY_TIME_LIMIT));

	       executor = new Executor.Builder()
	    		    .setTickLimit(ticks)
	    		    .setTimeLimit(time)
	                .setVisual(false)
	                .setPacmanPO(po)
	                .setGhostPO(po)
	                .setSightLimit(sightLimit)
	                .build();
	}
	
	
	void loadTeams()
	{
		list_pacMan = new Vector<PacmanController>();
		list_ghosts = new Vector<GhostController>();
		String pacManTeams = properties.getProperty(KEY_MsPACMAN_TEAMS);
		String ghostTeams = properties.getProperty(KEY_GHOSTS_TEAMS);
		
		for(String s: pacManTeams.split(","))
		{
			String className = s.trim();
			try {
				PacmanController pmc = (PacmanController)Class.forName(className).newInstance();
				pmc.setName(className);
				list_pacMan.add(pmc);
			} catch (Exception e) {
				System.err.println("Error loading MsPacMan class "+className);
			} 
		}

		for(String s: ghostTeams.split(","))
		{
			String className = s.trim();
			try {
				GhostController gc = (GhostController)Class.forName(className).newInstance();
				gc.setName(className);
				list_ghosts.add(gc);
			} catch (Exception e) {
				System.err.println("Error loading Ghosts class "+className);
			} 
		}
	}
	
	 void run()
	{
		int trials =  Integer.parseInt(properties.getProperty(KEY_TRIALS));
		Vector<String> names_pacMan = new Vector<String>();
		Vector<String> names_ghosts = new Vector<String>();
		for(Controller<?> c: list_pacMan)
			names_pacMan.add(c.getName());
		for(Controller<?> c: list_ghosts)
			names_ghosts.add(c.getName());
		scores = new Scores(names_pacMan,names_ghosts);
	    int p = 0;
	    for(PacmanController pacMan: list_pacMan)
	    {
	    	int g=0;
	    	for(GhostController ghosts: list_ghosts)
	    	{
	            try {  
		    		Stats[] result = executor.runExperiment(pacMan, ghosts, trials, pacMan.getClass().getName()+ " - " + ghosts.getClass().getName());
		    		scores.put(pacMan.getName(),ghosts.getName(), result[0]);
	        	}catch(Exception e) {
	        		System.err.println("Error executing pacman "+p+"  ghost: "+g);
	        		System.err.println(e);	
	        	}
	        }
	    	p++;
	    }

	}
	
	public Scores evaluate()
	{
		try {
			this.setDefaultProperites();
			this.loadConfig();
			this.configureExecutor();
			this.loadTeams();
			this.run();
			scores.computeRanking();
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return scores;
	}
	
	
	public static void main(String[] args) {
			PacManEvaluator evaluator = new PacManEvaluator();
			Scores scores = evaluator.evaluate();
			scores.printScoreAndRanking();


		
         
        
    }
}
