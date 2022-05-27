package mspacmans;
import static pacman.game.Constants.DELAY;
import static pacman.game.Constants.INTERVAL_WAIT;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import FSM.MsPacMan;
import pacman.controllers.Controller;
import pacman.controllers.GhostController;
import pacman.controllers.HumanController;
import pacman.game.Constants.MOVE;
import pacman.game.Drawable;
import pacman.game.Game;
import pacman.game.GameView;
import pacman.game.comms.BasicMessenger;
import pacman.game.comms.Messenger;
import pacman.game.internal.POType;
import pacman.game.util.Stats;
import Utils.QConstants;
import chen0040.rl.learning.qlearn.QLearner;

/**
 * This class may be used to execute the game in timed or un-timed modes, with or without
 * visuals. Competitors should implement their controllers in game.entries.ghosts and
 * game.entries.pacman respectively. The skeleton classes are already provided. The package
 * structure should not be changed (although you may create sub-packages in these packages).
 */
@SuppressWarnings("unused")
public class Executor {
    private final boolean pacmanPO;
    private final boolean ghostPO;
    private final boolean ghostsMessage;
    private final Messenger messenger;
    private final double scaleFactor;
    private final boolean setDaemon;
    private final boolean visuals;
    private final int tickLimit;
    private final int timeLimit;
    private final POType poType;
    private final int sightLimit;
    private final Random rnd = new Random();
    private final Function<Game, String> peek;
    private final Logger logger = LoggerFactory.getLogger(Executor.class);
	private boolean pacmanPOvisual;
    private static String VERSION = "2.2.0 (ICI 20/21 - Parallel Evaluator Included)";

    public static class Builder {
        private boolean pacmanPO = false;
        private boolean ghostPO = false;
        private boolean ghostsMessage = false;
        private Messenger messenger = new BasicMessenger();
        private double scaleFactor = 3.0d;
        private boolean setDaemon = false;
        private boolean visuals = false;
        private int tickLimit = 4000;
        private int timeLimit = 40;
        private POType poType = POType.LOS;
        private int sightLimit = 50;
        private Function<Game, String> peek = null;
		private boolean pacmanPOvisual;

        public Builder setPacmanPO(boolean po) {
            this.pacmanPO = po;
            return this;
        }

        public Builder setGhostPO(boolean po) {
            this.ghostPO = po;
            return this;
        }

        public Builder setGhostsMessage(boolean canMessage) {
            this.ghostsMessage = canMessage;
            if (canMessage) {
                messenger = new BasicMessenger();
            } else {
                messenger = null;
            }
            return this;
        }

        public Builder setMessenger(Messenger messenger) {
            this.ghostsMessage = true;
            this.messenger = messenger;
            return this;
        }

        public Builder setScaleFactor(double scaleFactor) {
            this.scaleFactor = scaleFactor;
            return this;
        }

        public Builder setGraphicsDaemon(boolean daemon) {
            this.setDaemon = daemon;
            return this;
        }

        public Builder setVisual(boolean visual) {
            this.visuals = visual;
            return this;
        }

        public Builder setTickLimit(int tickLimit) {
            this.tickLimit = tickLimit;
            return this;
        }

        public Builder setTimeLimit(int timeLimit) {
            this.timeLimit = timeLimit;
            return this;
        }

        public Builder setPOType(POType poType) {
            this.poType = poType;
            return this;
        }

        public Builder setSightLimit(int sightLimit) {
            this.sightLimit = sightLimit;
            return this;
        }

        public Builder setPeek(Function<Game, String> peek){
            this.peek = peek;
            return this;
        }

        public Executor build() {
        	System.err.println("MsPacMan Engine - Ingeniería de Comportamientos Inteligentes. Version "+Executor.VERSION);
            return new Executor(pacmanPO, ghostPO, ghostsMessage, messenger, scaleFactor, setDaemon, visuals, tickLimit, timeLimit, poType, sightLimit, peek, pacmanPOvisual);
        }

		public Builder setPacmanPOvisual(boolean b) {
			this.pacmanPOvisual = b;
			return this;
		}
    }

    /**
	 * Initialize the params.
	 */
    private Executor(
            boolean pacmanPO,
            boolean ghostPO,
            boolean ghostsMessage,
            Messenger messenger,
            double scaleFactor,
            boolean setDaemon,
            boolean visuals,
            int tickLimit,
            int timeLimit,
            POType poType,
            int sightLimit,
            Function<Game, String> peek,
            boolean pacmanPOvisual
            ) {
        this.pacmanPO = pacmanPO;
        this.ghostPO = ghostPO;
        this.ghostsMessage = ghostsMessage;
        this.messenger = messenger;
        this.scaleFactor = scaleFactor;
        this.setDaemon = setDaemon;
        this.visuals = visuals;
        this.tickLimit = tickLimit;
        this.timeLimit = timeLimit;
        this.poType = poType;
        this.sightLimit = sightLimit;
        this.peek = peek;
        this.pacmanPOvisual = pacmanPOvisual;
    }

    private static void writeStat(FileWriter writer, Stats stat, int i) throws IOException {
        writer.write(String.format("%s, %d, %f, %f, %f, %f, %d, %f, %f, %f, %d%n",
                stat.getDescription(),
                i,
                stat.getAverage(),
                stat.getSum(),
                stat.getSumsq(),
                stat.getStandardDeviation(),
                stat.getN(),
                stat.getMin(),
                stat.getMax(),
                stat.getStandardError(),
                stat.getMsTaken()));
    }

    //save file for replays
    public static void saveToFile(String data, String name, boolean append) {
        try (FileOutputStream outS = new FileOutputStream(name, append)) {
            PrintWriter pw = new PrintWriter(outS);

            pw.println(data);
            pw.flush();
            outS.close();

        } catch (IOException e) {
            System.out.println("Could not save data!");
        }
    }

    //load a replay
    private static ArrayList<String> loadReplay(String fileName) {
        ArrayList<String> replay = new ArrayList<String>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)))) {
            String input = br.readLine();

            while (input != null) {
                if (!input.equals("")) {
                    replay.add(input);
                }

                input = br.readLine();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return replay;
    }

    /**
     * For running multiple games without visuals. This is useful to get a good idea of how well a controller plays
     * against a chosen opponent: the random nature of the game means that performance can vary from game to game.
     * Running many games and looking at the average score (and standard deviation/error) helps to get a better
     * idea of how well the controller is likely to do in the competition.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController  The Ghosts controller
     * @param trials           The number of trials to be executed
     * @param description      Description for the stats
     * @return Stats[] containing the scores in index 0 and the ticks in position 1
     */
    public Stats[] runExperiment(Controller<MOVE> pacManController, GhostController ghostController, int trials, String description) {
        Stats stats = new Stats(description);
        Stats ticks = new Stats(description + " Ticks");
        GhostController ghostControllerCopy = ghostController.copy(ghostPO);
        Game game;


        Long startTime = System.currentTimeMillis();
        for (int i = 0; i < trials; ) {
            try {
                game = setupGame();
                precompute(pacManController, ghostController);
                while (!game.gameOver()) {
                    if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                        break;
                    }
                    handlePeek(game);
                    game.advanceGame(
                            pacManController.getMove(getPacmanCopy(game), System.currentTimeMillis() + timeLimit),
                            ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
                }
                stats.add(game.getScore());
                ticks.add(game.getCurrentLevelTime());
                i++;
                postcompute(pacManController, ghostController);
                System.out.println("Game finished: " + i + "   " + description);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        stats.setMsTaken(timeTaken);
        ticks.setMsTaken(timeTaken);

        
        return new Stats[]{stats, ticks};
    }

    /**
	 * Initializes the normal game
	 */
    private Game setupGame() {
        return (this.ghostsMessage) ? new Game(rnd.nextLong(), 0, messenger.copy(), poType, sightLimit) : new Game(rnd.nextLong(), 0, null, poType, sightLimit);
    }

    private void handlePeek(Game game){
        if(peek != null) logger.info(peek.apply(game));
    }

    public Stats[] runExperimentTicks(Controller<MOVE> pacManController, GhostController ghostController, int trials, String description) {
        Stats stats = new Stats(description);
        Stats ticks = new Stats(description);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);
        Game game;

        Long startTime = System.currentTimeMillis();
        for (int i = 0; i < trials; i++) {
            game = setupGame();
            precompute(pacManController, ghostController);

            while (!game.gameOver()) {
                handlePeek(game);
                game.advanceGame(
                        pacManController.getMove(getPacmanCopy(game), System.currentTimeMillis() + timeLimit),
                        ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
            }
            stats.add(game.getScore());
            ticks.add(game.getTotalTime());
            postcompute(pacManController, ghostController);

        }
        stats.setMsTaken(System.currentTimeMillis() - startTime);
        ticks.setMsTaken(System.currentTimeMillis() - startTime);


        return new Stats[]{stats, ticks};
    }

    /**
     * Run a game in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController  The Ghosts controller
     * @param delay            The delay between time-steps
     */
    public int runGame(Controller<MOVE> pacManController, GhostController ghostController, int delay) {
        Game game = setupGame();

        precompute(pacManController, ghostController);
        
        GameView gv = (visuals) ? setupGameView(pacManController, game) : null;

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                break;
            }
            handlePeek(game);
            game.advanceGame(
                    pacManController.getMove(getPacmanCopy(game), System.currentTimeMillis() + timeLimit),
                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));

            try {
                Thread.sleep(delay);
            } catch (Exception e) {
            }

            if (visuals) {
                gv.repaint();
            }
        }
        System.out.println(game.getScore());
        
        postcompute(pacManController, ghostController);
        
        return game.getScore();
    }

    private void postcompute(Controller<MOVE> pacManController, GhostController ghostController) {
		pacManController.postCompute();
		ghostController.postCompute();
	}

	private void precompute(Controller<MOVE> pacManController, GhostController ghostController) {
		String ghostName = ghostController.getClass().getCanonicalName();
		String pacManName = pacManController.getClass().getCanonicalName();
		
		pacManController.preCompute(ghostName);
		ghostController.preCompute(pacManName);
	}

	private Game getPacmanCopy(Game game) {
        return game.copy((pacmanPO) ? Game.PACMAN : Game.CLONE);
    }
    
    private Game getGhostsCopy(Game game) {
    	return game.copy((ghostPO) ? Game.ANY_GHOST : Game.CLONE);
       
    }

    private GameView setupGameView(Controller<MOVE> pacManController, Game game) {
        GameView gv;
        gv = new GameView(game, setDaemon);
        gv.setScaleFactor(scaleFactor);
        gv.showGame();
        if(pacmanPOvisual) gv.setPO(this.pacmanPO);
        if (pacManController instanceof HumanController) {
            gv.setFocusable(true);
            gv.requestFocus();
            gv.setPO(this.pacmanPO);
            gv.addKeyListener(((HumanController) pacManController).getKeyboardInput());
        }

        if (pacManController instanceof Drawable) {
            gv.addDrawable((Drawable) pacManController);
        }
        return gv;
    }

    /**
     * Run the game with time limit (asynchronous mode).
     * Can be played with and without visual display of game states.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController  The Ghosts controller
     */
    public void runGameTimed(Controller<MOVE> pacManController, GhostController ghostController) {
        Game game = setupGame();

        GameView gv = (visuals) ? setupGameView(pacManController, game) : null;
        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        precompute(pacManController, ghostController);
        
        new Thread(pacManController).start();
        new Thread(ghostControllerCopy).start();

        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                break;
            }
            handlePeek(game);
            pacManController.update(getPacmanCopy(game), System.currentTimeMillis() + DELAY);
            ghostControllerCopy.update(getGhostsCopy(game), System.currentTimeMillis() + DELAY);

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            game.advanceGame(pacManController.getMove(), ghostControllerCopy.getMove());

            if (visuals) {
                gv.repaint();
            }
        }

        pacManController.terminate();
        ghostControllerCopy.terminate();
        
        postcompute(pacManController, ghostController);

    }

    /**
     * Run the game in asynchronous mode but proceed as soon as both controllers replied. The time limit still applies so
     * so the game will proceed after 40ms regardless of whether the controllers managed to calculate a turn.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController  The Ghosts controller
     * @param fixedTime        Whether or not to wait until 40ms are up even if both controllers already responded
     * @param desc             the description for the stats
     * @return Stat score achieved by Ms. Pac-Man
     */
    public Stats runGameTimedSpeedOptimised(Controller<MOVE> pacManController, GhostController ghostController, boolean fixedTime, String desc) {
        Game game = setupGame();

        GameView gv = (visuals) ? setupGameView(pacManController, game) : null;
        GhostController ghostControllerCopy = ghostController.copy(ghostPO);
        Stats stats = new Stats(desc);

        precompute(pacManController, ghostController);

        new Thread(pacManController).start();
        new Thread(ghostControllerCopy).start();
        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                break;
            }
            handlePeek(game);
            pacManController.update(getPacmanCopy(game), System.currentTimeMillis() + DELAY);
            ghostControllerCopy.update(getGhostsCopy(game), System.currentTimeMillis() + DELAY);

            try {
                long waited = DELAY / INTERVAL_WAIT;

                for (int j = 0; j < DELAY / INTERVAL_WAIT; j++) {
                    Thread.sleep(INTERVAL_WAIT);

                    if (pacManController.hasComputed() && ghostControllerCopy.hasComputed()) {
                        waited = j;
                        break;
                    }
                }

                if (fixedTime) {
                    Thread.sleep(((DELAY / INTERVAL_WAIT) - waited) * INTERVAL_WAIT);
                }

                game.advanceGame(pacManController.getMove(), ghostControllerCopy.getMove());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (visuals) {
                gv.repaint();
            }
        }

        pacManController.terminate();
        ghostControllerCopy.terminate();
        stats.add(game.getScore());
        
        postcompute(pacManController, ghostController);

        return stats;
    }

    /**
     * Run a game in asynchronous mode and recorded.
     *
     * @param pacManController The Pac-Man controller
     * @param ghostController  The Ghosts controller
     * @param fileName         The file name of the file that saves the replay
     * @return Stats the statistics for the run
     */
    public Stats runGameTimedRecorded(Controller<MOVE> pacManController, GhostController ghostController, String fileName) {
        Stats stats = new Stats("");
        StringBuilder replay = new StringBuilder();

        Game game = setupGame();

        precompute(pacManController, ghostController);
        
        GameView gv = null;
        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        if (visuals) {
            gv = new GameView(game, setDaemon);
            gv.setScaleFactor(scaleFactor);
            gv.showGame();

            if (pacManController instanceof HumanController) {
                gv.getFrame().addKeyListener(((HumanController) pacManController).getKeyboardInput());
            }

            if (pacManController instanceof Drawable) {
                gv.addDrawable((Drawable) pacManController);
            }
        }

        new Thread(pacManController).start();
        new Thread(ghostControllerCopy).start();

        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                break;
            }
            handlePeek(game);
            pacManController.update(getPacmanCopy(game), System.currentTimeMillis() + DELAY);
            ghostControllerCopy.update(getGhostsCopy(game), System.currentTimeMillis() + DELAY);

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            game.advanceGame(pacManController.getMove(), ghostControllerCopy.getMove());

            if (visuals) {
                gv.repaint();
            }

            replay.append(game.getGameState() + "\n");
        }
        stats.add(game.getScore());

        pacManController.terminate();
        ghostControllerCopy.terminate();

        postcompute(pacManController, ghostController);

        
        saveToFile(replay.toString(), fileName, false);
        return stats;
    }

    /**
     * Replay a previously saved game.
     *
     * @param fileName The file name of the game to be played
     * @param visual   Indicates whether or not to use visuals
     */
    public void replayGame(String fileName, boolean visual) {
        ArrayList<String> timeSteps = loadReplay(fileName);

        Game game = setupGame();

        GameView gv = null;

        if (visual) {
            gv = new GameView(game, setDaemon);
            gv.setScaleFactor(scaleFactor);
            gv.showGame();
        }

        for (int j = 0; j < timeSteps.size(); j++) {
            game.setGameState(timeSteps.get(j));

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (visual) {
                gv.repaint();
            }
        }
    }
    

    
    
    /**
     * Run a number of games game to train the model in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavior of mspacman.
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameQtrain(GhostController ghostController, int partidas) {
        Game game = setupGame();
        MOVE actMove;
        QLearner learner = new QLearner(13333, QConstants.numMoves, 0.1, 0.7, 0.1); //2: isEdible, 4: direction Ghost and Pills, 4: Distance intervals
        QPacMan qPacMan = new QPacManOriginal(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupGame();
	        	qPacMan.setNewGame(game);
	        }	
        }
        return learner;   
    }
    /**
     * Run a number of games to train the random model in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavior of mspacman.
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameQtrainRandom(GhostController ghostController, int partidas) {
        Game game = setupRandomGame();
        MOVE actMove;
        QLearner learner = new QLearner(13333, QConstants.numMoves, 0.1, 0.7, 0.1); //2: isEdible, 4: direction Ghost and Pills, 4: Distance intervals
        QPacMan qPacMan = new QPacManOriginal(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupRandomGame();
	        	qPacMan.setNewGame(game);
	        }
	        if (i % 5000 == 4999)
	        	System.out.println("Partida " + (i+1));
        }
        return learner;   
    }
    /**
     * Run a number of games to train the pills model in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanPills and the Qlearner to implement the behavior of mspacman.
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameQtrainPills(GhostController ghostController, int partidas) {
        Game game = setupPillsGame();
        MOVE actMove;
        QLearner learner = new QLearner(40, QConstants.numMoves);
        QPacMan qPacMan = new QPacManPills(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupRandomGame();
	        	qPacMan.setNewGame(game);
	        }
	        if (i % 5000 == 4999)
	        	System.out.println("Partida " + (i+1));
        }
        return learner;   
    }
    /**
     * Run a number of games to train the flee model in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanFlee and the Qlearner to implement the behavior of mspacman.
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameQtrainFlee(GhostController ghostController, int partidas) {
        Game game = setupFleeGame();
        MOVE actMove;
        QLearner learner = new QLearner(3400, QConstants.numMoves);
        QPacMan qPacMan = new QPacManFlee(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupFleeGame();
	        	qPacMan.setNewGame(game);
	        }
	        if (i % 5000 == 4999)
	        	System.out.println("Partida " + (i+1));
        }
        return learner;   
    }
    /**
     * Run a number of games to train the chase model in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanChase and the Qlearner to implement the behavior of mspacman.
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameQtrainChase(GhostController ghostController, int partidas) {
        Game game = setupChaseGame();
        MOVE actMove;
        QLearner learner = new QLearner(40, QConstants.numMoves); //3: direction Ghost , 3: Distance ghost
        QPacMan qPacMan = new QPacManChase(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupChaseGame();
	        	qPacMan.setNewGame(game);
	        }
	        if (i % 5000 == 4999)
	        	System.out.println("Partida " + (i+1));
        }
        return learner;   
    }
    /**
     * Run a number of games to train the original model in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavior of mspacman.
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameQtrainOriginal(GhostController ghostController, int partidas) {
        Game game = setupRandomGame();
        MOVE actMove;
        QLearner learner = new QLearner(33, QConstants.numMoves);
        QPacMan qPacMan = new QPacManOriginal(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupRandomGame();
	        	qPacMan.setNewGame(game);
	        }
	        if (i % 5000 == 4999)
	        	System.out.println("Partida " + (i+1));
        }
        return learner;   
    }
    /**
     * Load a trained original model and run a number of games to train more the  model  in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavior of mspacman.
     * @param model 		QPacMan trained model
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    
    public QLearner runGameLoadQtrain(String model, GhostController ghostController, int partidas) {
        Game game = setupGame();
        MOVE actMove;
        QLearner learner = QLearner.fromJson(model);
        QPacMan qPacMan = new QPacManOriginal(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupGame();
	        	qPacMan.setNewGame(game);
	        }	
        }
        return learner;   
    }
    
    /**
     * Load a random model and run a number of games to train more the  model : the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavior of mspacman.
     * @param model 		QPacMan trained model
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of games.
     */
    public QLearner runGameLoadQtrainRandom(String model, GhostController ghostController, int partidas) {
        Game game = setupRandomGame();
        MOVE actMove;
        QLearner learner = QLearner.fromJson(model);
        QPacMan qPacMan = new QPacManOriginal(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupRandomGame();
	        	qPacMan.setNewGame(game);
	        }	
        }
        return learner;   
    }
    
    /**
     * Load a pills model and run a number of games to train more the  model: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanPills and the Qlearner to implement the behavoir of mspacman.
     * @param model 		QPacMan trained model
     * @param ghostController  The Ghosts controller
     * @param partidas         Number of matches.
     */
    public QLearner runGameLoadQtrainPills(String model, GhostController ghostController, int partidas) {
        Game game = setupRandomGame();
        MOVE actMove;
        QLearner learner = QLearner.fromJson(model);
        QPacMan qPacMan = new QPacManPills(learner);
        qPacMan.setNewGame(game);

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        for(int i=0; i < partidas; ++i) {
	        while (!game.gameOver()) {
	            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
	                break;
	            }
	            handlePeek(game);
	            actMove = qPacMan.act();
	            game.advanceGame(
	            		actMove,
	                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
	            
	            qPacMan.updateStrategy();      
	        }
	        if(i != partidas -1) {
	        	game = setupRandomGame();
	        	qPacMan.setNewGame(game);
	        }	
        }
        return learner;   
    }
    
    
    
    /*private GameView setupQGameView(Game game) { // TODO posible quitarlo si se usa setupGameView con null
        GameView gv;
        gv = new GameView(game, setDaemon);
        gv.setScaleFactor(scaleFactor);
        gv.showGame();
        if(pacmanPOvisual) gv.setPO(this.pacmanPO);
        return gv;
    }*/
    
   
    /**
     * Run a game of a trained original model given: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavoir of mspacman.
     * @param model 		QPacMan trained model
     * @param ghostController  The Ghosts controller
     * @param delay            The delay between time-steps
     */
    public int runGameQ(QLearner model, GhostController ghostController, int delay) {
        Game game = setupRandomGame();
        
        MOVE actMove;
        QPacMan qPacMan = new QPacManOriginal(model);
        qPacMan.setNewGame(game);
        
        GameView gv = (visuals) ? setupGameView(null, game) : null;       

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime())
                break;
            
            handlePeek(game);
            
            actMove = qPacMan.act();
            game.advanceGame(
            		actMove,
                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
            qPacMan.updateStrategy();   
            
            try {
                Thread.sleep(delay);
            } catch (Exception e) {
            }
            
            if (visuals)
                gv.repaint();
        }
        
        return game.getScore();
    }
    
    /**
     * Run a game of a random trained model given in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *	Uses the QPacmanOriginal and the Qlearner to implement the behavoir of mspacman.
     * @param model 		QPacMan trained model
     * @param ghostController  The Ghosts controller
     * @param delay            The delay between time-steps
     */
    public int runRandomQGame(QLearner model, GhostController ghostController, int delay) {
        Game game = setupRandomGame();
        
        MOVE actMove;
        QPacManOriginal qPacMan = new QPacManOriginal(model);
        qPacMan.setNewGame(game);
        
        GameView gv = (visuals) ? setupGameView(null, game) : null;       

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime())
                break;
            
            handlePeek(game);
            
            actMove = qPacMan.act();
            game.advanceGame(
            		actMove,
                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
            qPacMan.updateStrategy();   
            
            try {
                Thread.sleep(delay);
            } catch (Exception e) {
            }
            
            if (visuals)
                gv.repaint();
        }
        
        return game.getScore();
    }
    
    /**
     * Run a FSM game in asynchronous mode: the game waits until a move is returned. In order to slow thing down in case
     * the controllers return very quickly, a time limit can be used. If fasted gameplay is required, this delay
     * should be put as 0.
     *
     * @param ghostController  The Ghosts controller
     * @param delay            The delay between time-steps
     */
    public int runGameFSM(GhostController ghostController, int delay) {
        Game game = setupGame();
        MsPacMan pacManController = new MsPacMan(game, 0, "");
        
        precompute(pacManController, ghostController);
        
        GameView gv = (visuals) ? setupGameView(pacManController, game) : null;

        GhostController ghostControllerCopy = ghostController.copy(ghostPO);

        while (!game.gameOver()) {
            if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                break;
            }
            handlePeek(game);
            game.advanceGame(
                    pacManController.getMove(getPacmanCopy(game), System.currentTimeMillis() + timeLimit),
                    ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));

            try {
                Thread.sleep(delay);
            } catch (Exception e) {
            }

            if (visuals) {
                gv.repaint();
            }
        }
        System.out.println(game.getScore());
        
        postcompute(pacManController, ghostController);
        
        return game.getScore();
    }
    /**
     *
     * @param ghostController  The Ghosts controller
     * @param trials            The number of experiments
     * 
     */
    /**
     *  Run a number of FSM games and get the results in stats[]
     * @param ghostController  The Ghosts controller
     * @param trials   Number of experiments 
     * @param description  Description of the experiment
     * @param numTrainings    Number of trainings that the model has
     * @param ghostType  Type of the ghost
     */
    public Stats[] runFSMExperiment(GhostController ghostController, int trials, String description,int numTrainings, String ghostType) {
        Stats stats = new Stats(description);
        Stats ticks = new Stats(description + " Ticks");
        GhostController ghostControllerCopy = ghostController.copy(ghostPO);
        Game game;

        Long startTime = System.currentTimeMillis();
        for (int i = 0; i < trials; ) {
            try {
                game = setupGame();
                MsPacMan pacManController = new MsPacMan(game,numTrainings, ghostType);
                precompute(pacManController, ghostController);
                while (!game.gameOver()) {
                    if (tickLimit != -1 && tickLimit < game.getTotalTime()) {
                        break;
                    }
                    handlePeek(game);
                    game.advanceGame(
                            pacManController.getMove(getPacmanCopy(game), System.currentTimeMillis() + timeLimit),
                            ghostControllerCopy.getMove(getGhostsCopy(game), System.currentTimeMillis() + timeLimit));
                }
                stats.add(game.getScore());
                ticks.add(game.getCurrentLevelTime());
                i++;
                postcompute(pacManController, ghostController);
                System.out.println("Game finished: " + i + "   " + description);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        long timeTaken = System.currentTimeMillis() - startTime;
        stats.setMsTaken(timeTaken);
        ticks.setMsTaken(timeTaken);

        
        return new Stats[]{stats, ticks};
    }

    /**
     * Method that sets a new random game
     */
	private Game setupRandomGame() {
		return new Game(rnd.nextLong(), 0, null, poType, sightLimit, false);
	}
	
	/**
     * Method that sets a new  game with only pills
     */
	private Game setupPillsGame() {
		return new Game(rnd.nextLong(), 0, null, poType, sightLimit, true);
	}
	
	/**
     * Method that sets a new  game where the ghosts are edible
     */
	private Game setupChaseGame() {
		return new Game(rnd.nextLong(), 0, poType, sightLimit, true);
	}
	
	/**
     * Method that sets a new  game where the ghosts are not edible
     */
	private Game setupFleeGame() {
		return new Game(rnd.nextLong(), 0, null, poType, sightLimit, true,true);
	}
}