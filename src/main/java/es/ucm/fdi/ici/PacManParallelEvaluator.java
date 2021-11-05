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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import pacman.Executor;
import pacman.controllers.GhostController;
import pacman.controllers.PacmanController;
import pacman.game.util.Stats;

public class PacManParallelEvaluator {
	
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

	private Properties properties;
	private Vector<Class<?>> list_pacMan; 
	private Vector<Class<?>> list_ghosts;
	private Scores scores;
	
	private int cores;
	
	/**
	 * Default constructor. Uses AvailableCores - 1 threads
	 */
	public PacManParallelEvaluator() {
		cores = Runtime.getRuntime().availableProcessors()-1;
	}
	
	/**
	 * Constructor where clients can indicate the number of cores to parallelize.
	 * @param cores
	 */
	public PacManParallelEvaluator(int cores)
	{
		this.cores = cores;
	}
	
	
	

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
	
	public Executor configureExecutor()
	{
		Boolean po = properties.getProperty(KEY_PO).equalsIgnoreCase("true");
		Integer sightLimit =  Integer.parseInt(properties.getProperty(KEY_PO_SIGHT_LIMIT));
		Integer ticks = Integer.parseInt(properties.getProperty(KEY_TICKS_LIMIT));
		Integer time =  Integer.parseInt(properties.getProperty(KEY_TIME_LIMIT));

		Executor  executor = new Executor.Builder()
	    		    .setTickLimit(ticks)
	    		    .setTimeLimit(time)
	                .setVisual(false)
	                .setPacmanPO(po)
	                .setGhostPO(po)
	                .setSightLimit(sightLimit)
	                .build();
	     return executor;
	}
	
	
	void loadTeams()
	{
		list_pacMan = new Vector<Class<?>>();
		list_ghosts = new Vector<Class<?>>();
		String pacManTeams = properties.getProperty(KEY_MsPACMAN_TEAMS);
		String ghostTeams = properties.getProperty(KEY_GHOSTS_TEAMS);
		
		for(String s: pacManTeams.split(","))
		{
			String className = s.trim();
			try {
				list_pacMan.add(Class.forName(className));
			} catch (Exception e) {
				System.err.println("Error loading MsPacMan class "+className);
			} 
		}

		for(String s: ghostTeams.split(","))
		{
			String className = s.trim();
			try {
				list_ghosts.add(Class.forName(className));
			} catch (Exception e) {
				System.err.println("Error loading Ghosts class "+className);
			} 
		}
	}
	
	void run()
	{
		int trials =  Integer.parseInt(properties.getProperty(KEY_TRIALS));
		System.err.println("Parallel Evaluation using "+cores+" cores");
	    ExecutorService exec = Executors.newFixedThreadPool(cores);

        try {
			Vector<String> list_pacManObj = new Vector<String>(); 
			Vector<String> list_ghostsObj  = new Vector<String>();
			for(Class<?> pacManClass: list_pacMan)
				list_pacManObj.add(((PacmanController)pacManClass.newInstance()).getName());
	    	for(Class<?> ghostsClass: list_ghosts)
	    		list_ghostsObj.add(((GhostController)ghostsClass.newInstance()).getName());
	
			scores = new Scores(list_pacManObj,list_ghostsObj);
			List<Task> tasks = new ArrayList<Task>();
		    
			for(Class<?> pacManClass: list_pacMan)
		    	for(Class<?> ghostsClass: list_ghosts)
		    	{
		    		PacmanController pacMan = (PacmanController)pacManClass.newInstance();
		    		//pacMan.setName(pacManClass.getCanonicalName());
		    		
		    		GhostController ghosts = (GhostController)ghostsClass.newInstance();
		    		//ghosts.setName(ghostsClass.getCanonicalName());
		           tasks.add(new Task(pacMan,ghosts,trials));
		    	}
		    		

            List<Future<Result>> results = exec.invokeAll(tasks);
            //exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            for (Future<Result> fr : results)
            	scores.put(fr.get().getPacMan().getName(),fr.get().getGhosts().getName(), fr.get().getStats());
         }
         catch(Exception e)
         {
        	 System.err.println(e.getLocalizedMessage());
        	 e.printStackTrace();
         } finally {
            exec.shutdown();
        }
	    
	    


	}
	
	class Result {
		PacmanController pacMan;
		GhostController ghosts;
		Stats stats;
		public Result(PacmanController pacMan, GhostController ghosts, Stats stats) {
			super();
			this.pacMan = pacMan;
			this.ghosts = ghosts;
			this.stats = stats;
		}
		public PacmanController getPacMan() {
			return pacMan;
		}
		public GhostController getGhosts() {
			return ghosts;
		}
		public Stats getStats() {
			return stats;
		}
		
		
	}

	class Task implements Callable<Result> {
		PacmanController pacMan;
		GhostController ghosts;
		int trials;
		
		
		public Task(PacmanController pacMan, GhostController ghosts, int trials) {
			super();
			this.pacMan = pacMan;
			this.ghosts = ghosts;
			this.trials = trials;
		}

		@Override
		public Result call() throws Exception {
			Executor executor = configureExecutor();
			Stats[] stats = executor.runExperiment(pacMan, ghosts, trials, pacMan.getClass().getName()+ " - " + ghosts.getClass().getName());;
			return new Result(pacMan, ghosts, stats[0]);
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
			PacManParallelEvaluator evaluator = new PacManParallelEvaluator();
			Scores scores = evaluator.evaluate();
			scores.printScoreAndRanking();


		
         
        
    }
}
