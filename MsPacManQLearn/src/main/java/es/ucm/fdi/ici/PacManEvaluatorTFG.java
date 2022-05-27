package es.ucm.fdi.ici;
/**
 * PacManEvaluatorTFG
 * Class to evaluate the MsPacMan developed
 * during the TFG
 */


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Vector;

import mspacmans.Executor;
import pacman.controllers.Controller;
import pacman.controllers.GhostController;
import pacman.controllers.PacmanController;
import pacman.game.util.Stats;

public class PacManEvaluatorTFG {
	
	private static final String CONFIG_FILE = "config.properties";
	
	public static final String KEY_TRIALS = "trials";
	public static final String KEY_MsPACMAN_TEAMS = "MsPacManTeams";
	public static final String KEY_GHOSTS_TEAMS = "GhostsTeams";
	public static final String KEY_PO = "po";
	public static final String KEY_PO_SIGHT_LIMIT = "sightLimit";	
	public static final String KEY_TICKS_LIMIT = "ticks";
	public static final String KEY_TIME_LIMIT = "time";
	public static final String OUTPUT_FILE_NAME = "file";
	
	
	
	//ONLY FOR CBR
	public static final int LIMIT = 2000;

	private Executor executor;
	private Properties properties;
	private Vector<PacmanController> list_pacMan; 
	private Vector<GhostController> list_ghosts;
	private Scores scores;
	private int trials;
	

	void setDefaultProperites()
	{
		properties = new Properties();
		properties.setProperty(KEY_PO, "false");
		properties.setProperty(KEY_TRIALS, "100");
		properties.setProperty(KEY_TICKS_LIMIT, "4000");
		properties.setProperty(KEY_TIME_LIMIT, "40");
		properties.setProperty(KEY_PO_SIGHT_LIMIT, "50");
		properties.setProperty(OUTPUT_FILE_NAME, "output.txt");
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
		trials =  Integer.parseInt(properties.getProperty(KEY_TRIALS));
		Vector<String> names_pacMan = new Vector<String>();
		Vector<String> names_ghosts = new Vector<String>();
		for(Controller<?> c: list_pacMan)
			names_pacMan.add(c.getName());
		for(Controller<?> c: list_ghosts)
			names_ghosts.add(c.getName());
		
		for(int numTrainings=1000;numTrainings<=10000000;numTrainings=numTrainings*10) {  //numTrainings cambiar segun los que usemos	
			names_pacMan.add("MsPacManQLearn"+numTrainings);
		}
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
	    
    	int g=0;
    	
    	String ghostType;
    	for(int numTrainings=1000;numTrainings<=10000000;numTrainings=numTrainings*10) {  //numTrainings cambiar segun los que usemos
    		for(GhostController ghosts: list_ghosts) {
                try {  
                	ghostType = ghosts.getClass().getName();
    	    		Stats[] result = executor.runFSMExperiment(ghosts, trials,"MsPacManQLearn"+numTrainings+" - " + ghostType, numTrainings, ghostType.replace("Others.Ghost", ""));
    	    		scores.put("MsPacManQLearn"+numTrainings, ghosts.getName(), result[0]);
    	    		g++; //TODO
            	}catch(Exception e) {
            		System.err.println("Error executing pacman "+p+"  ghost: "+g);
            		System.err.println(e);	
            	}
            }
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
			
			String file = properties.getProperty(OUTPUT_FILE_NAME);
			scores.exportToFile(file,trials);
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return scores;
	}
	
	
	public static void main(String[] args) {
			PacManEvaluatorTFG evaluator = new PacManEvaluatorTFG();
			Scores scores = evaluator.evaluate();
			scores.printScoreAndRanking();


		
         
        
    }
}
