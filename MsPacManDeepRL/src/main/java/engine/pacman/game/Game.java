package engine.pacman.game;

import static engine.pacman.game.Constants.AWARD_LIFE_LEFT;
import static engine.pacman.game.Constants.COMMON_LAIR_TIME;
import static engine.pacman.game.Constants.EAT_DISTANCE;
import static engine.pacman.game.Constants.EDIBLE_TIME;
import static engine.pacman.game.Constants.EDIBLE_TIME_REDUCTION;
import static engine.pacman.game.Constants.EXTRA_LIFE_SCORE;
import static engine.pacman.game.Constants.GHOST_EAT_SCORE;
import static engine.pacman.game.Constants.GHOST_REVERSAL;
import static engine.pacman.game.Constants.GHOST_SPEED_REDUCTION;
import static engine.pacman.game.Constants.LAIR_REDUCTION;
import static engine.pacman.game.Constants.LEVEL_LIMIT;
import static engine.pacman.game.Constants.LEVEL_RESET_REDUCTION;
import static engine.pacman.game.Constants.MAX_TIME;
import static engine.pacman.game.Constants.NUM_GHOSTS;
import static engine.pacman.game.Constants.NUM_LIVES;
import static engine.pacman.game.Constants.NUM_MAZES;
import static engine.pacman.game.Constants.PILL;
import static engine.pacman.game.Constants.POWER_PILL;

import java.util.BitSet;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.bouncycastle.util.Arrays;

import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;
import engine.pacman.game.comms.Messenger;
import engine.pacman.game.info.GameInfo;
import engine.pacman.game.internal.Ghost;
import engine.pacman.game.internal.Maze;
import engine.pacman.game.internal.Node;
import engine.pacman.game.internal.POType;
import engine.pacman.game.internal.PacMan;
import engine.pacman.game.internal.PathsCache;

/**
 * The implementation of Ms Pac-Man. This class contains the game engine and all
 * methods required to query the state of the game. First, the mazes are loaded
 * once only as they are immutable. The game then proceeds to initialise all
 * variables using default values. The game class also provides numerous methods
 * to extract the game state as a string (used for replays and for communication
 * via pipes during the competition) and to create copies. Care has been taken
 * to implement the game efficiently to ensure that copies can be created
 * quickly.
 * <p>
 * The game has a central update method called advanceGame which takes a move
 * for Ms Pac-Man and up to 4 moves for the ghosts. It then updates the
 * positions of all characters, check whether pills or power pills have been
 * eaten and updates the game state accordingly.
 * <p>
 * All other methods are to access the gamestate and to compute numerous aspects
 * such as directions to taken given a target or a shortest path from a to b.
 * All shortest path distances from any node to any other node are pre-computed
 * and loaded from file. This makes these methods more efficient. Note about the
 * ghosts: ghosts are not allowed to reverse. Hence it is not possible to simply
 * look up the shortest path distance. Instead, one can approximate this
 * greedily or use A* to compute it properly. The former is somewhat quicker and
 * has a low error rate. The latter takes a bit longer but is absolutely
 * accurate. We use the pre-computed shortest path distances as admissable
 * heuristic so it is very efficient.
 * <p>
 * Cloning
 * <p>
 * The game can be cloned to produce a CO Forward Model
 * <p>
 * The game can be cloned and given PO constraints - it cannot be forwarded from
 * here unless it has been provided with a GameInfo. Exact details tbc
 */
public final class Game {
	private static PathsCache[] caches = new PathsCache[NUM_MAZES];
	// mazes are only loaded once since they don't change over time
	private static Maze[] mazes = new Maze[NUM_MAZES];

	static {
		for (int i = 0; i < mazes.length; i++) {
			mazes[i] = new Maze(i);
		}
	}

	static {
		for (int i = 0; i < mazes.length; i++) {
			caches[i] = new PathsCache(i);
		}
	}

	public static final int CLONE = -1;
	public static final int BLINKY = GHOST.BLINKY.ordinal();
	public static final int INKY = GHOST.INKY.ordinal();
	public static final int PINKY = GHOST.PINKY.ordinal();
	public static final int SUE = GHOST.SUE.ordinal();
	public static final int PACMAN = 5;
	public static final int ANY_GHOST = 6;

	private final POType poType;
	private final int sightLimit;
	private boolean ghostsPresent = true;
	private boolean pillsPresent = true;
	private boolean powerPillsPresent = true;
	// pills stored as bitsets for efficient copying
	private BitSet pills;
	private BitSet powerPills;
	// all the game's variables
	private int mazeIndex;
	private int levelCount;
	private int currentLevelTime;
	private int totalTime;
	private int score;
	private int ghostEatMultiplier;
	private int timeOfLastGlobalReversal;
	private boolean gameOver;
	private boolean pacmanWasEaten;
	private boolean pillWasEaten;
	private boolean powerPillWasEaten;
	private EnumMap<GHOST, Boolean> ghostsEaten;
	// the data relating to internalPacman and the ghosts are stored in respective
	// data structures for clarity
	private PacMan internalPacman;
	private EnumMap<GHOST, Ghost> ghosts;
	// PO State
	private boolean po;
	private boolean beenBlanked;
	// either ID of a ghost or higher for internalPacman
	private int agent = 0;
	private Maze currentMaze;
	private Random rnd;
	private long seed;
	// Messenger - null if not available
	private Messenger messenger;
	private String PacManControllerName = "";
	private String GhostsControllerName = "";

	private int ghostDefaultEdibleTime = 0;
	private boolean isTraining;

	/**
	 * Instantiates a new game. The seed is used to initialise the pseudo-random
	 * number generator. This way, a game may be replicated exactly by using
	 * identical seeds. Note: in the competition, the games received from the game
	 * server are using different seeds. Otherwise global reversal events would be
	 * predictable.
	 *
	 * @param seed The seed for the pseudo-random number generator
	 */
	public Game(long seed) {
		this(seed, null);
	}

	/**
	 * Initiates a new game specifying the maze to start with.
	 *
	 * @param seed        Seed used for the pseudo-random numbers
	 * @param initialMaze The maze to start the game with
	 */
	public Game(long seed, int initialMaze) {
		this(seed, initialMaze, null);
	}

	public Game(long seed, Messenger messenger) {
		this(seed, 0, messenger);
	}

	public Game(long seed, int initialMaze, Messenger messenger) {
		this(seed, initialMaze, messenger, POType.LOS, 100);
	}

	public Game(long seed, int initialMaze, Messenger messenger, POType poType, int sightLimit) {
		this.seed = seed;
		rnd = new Random(seed);
		this.messenger = messenger;

		init(initialMaze);
		this.poType = poType;
		this.sightLimit = sightLimit;
	}

	public Game(long seed, int initialMaze, Messenger messenger, POType poType, int sightLimit, boolean training,
			boolean edible) {
		if (edible)
			this.ghostDefaultEdibleTime = Integer.MAX_VALUE;
		this.isTraining = training;
		this.seed = seed;
		rnd = new Random(seed);
		this.messenger = messenger;
		this.poType = poType;
		this.sightLimit = sightLimit;
		if (edible)
			initRandomlyEdible(initialMaze);
		else
			init(initialMaze);
	}

	/*
	 * public Game(long seed, int initialMaze, Messenger messenger, POType poType,
	 * int sightLimit, boolean random) { //Alex this.seed = seed; rnd = new
	 * Random(seed); this.messenger = messenger;
	 * 
	 * if (random) initPills(initialMaze); else init(initialMaze); this.poType =
	 * poType; this.sightLimit = sightLimit; }
	 * 
	 * public Game(long seed, int initialMaze, POType poType, int sightLimit,
	 * boolean random) { //David this.seed = seed; rnd = new Random(seed);
	 * this.messenger = null;
	 * 
	 * if (random) initRandomly2(initialMaze); else init(initialMaze); this.poType =
	 * poType; this.sightLimit = sightLimit; }
	 * 
	 * public Game(long seed, int initialMaze, Messenger messenger, POType poType,
	 * int sightLimit, boolean random, boolean onlyNotEdible) { //Dani this.seed =
	 * seed; rnd = new Random(seed); this.messenger = messenger;
	 * 
	 * if (random) initRandomlyOnlyNotEdible(initialMaze); else
	 * initOnlyNotEdible(initialMaze); this.poType = poType; this.sightLimit =
	 * sightLimit; }
	 */

	/////////////////////////////////////////////////////////////////////////////
	/////////////////// Constructors and initialisers /////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Empty constructor used by the copy method.
	 */
	private Game(POType poType, int sightLimit) {
		this.poType = poType;
		this.sightLimit = sightLimit;
	}

	private int getNodeIndexOfOwner() {
		if (agent >= NUM_GHOSTS) {
			return internalPacman.currentNodeIndex;
		} else {
			return ghosts.get(GHOST.values()[agent]).currentNodeIndex;
		}
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean isNodeObservable(int nodeIndex) {
		if (!po) {
			return true;
		}
		if (nodeIndex == -1) {
			return false;
		}
		Node currentNode = (mazes[mazeIndex]).graph[getNodeIndexOfOwner()];

		if (nodeIndex >= mazes[mazeIndex].graph.length)
			return false;
		Node check = (mazes[mazeIndex]).graph[nodeIndex];

		switch (poType) {
		case LOS:
			return handleLOS(currentNode, check) && straightRouteBlocked(currentNode, check);
		case RADIUS:
			double manhattan = getManhattanDistance(currentNode.nodeIndex, check.nodeIndex);
			return (manhattan <= sightLimit);
		case FF_LOS:
			Boolean x = handleFFLOS(currentNode, check);
			if (x != null)
				return x;
			break;
		}
		return false;
	}

	private Boolean handleFFLOS(Node currentNode, Node check) {
		if (currentNode.x == check.x || currentNode.y == check.y) {
			// Get direction currently going in
			MOVE previousMove = (agent >= NUM_GHOSTS) ? internalPacman.lastMoveMade
					: ghosts.get(GHOST.values()[agent]).lastMoveMade;
			switch (previousMove) {
			case UP:
				if (currentNode.x == check.x && currentNode.y >= check.y) {
					return straightRouteBlocked(currentNode, check);
				}
				break;
			case DOWN:
				if (currentNode.x == check.x && currentNode.y <= check.y) {
					return straightRouteBlocked(currentNode, check);
				}
				break;
			case LEFT:
				if (currentNode.y == check.y && currentNode.x >= check.x) {
					return straightRouteBlocked(currentNode, check);
				}
				break;
			case RIGHT:
				if (currentNode.y == check.y && currentNode.x <= check.x) {
					return straightRouteBlocked(currentNode, check);
				}
				break;
			}
		}
		return null;
	}

	private boolean handleLOS(Node currentNode, Node check) {
		return currentNode.x == check.x || currentNode.y == check.y;
	}

	private boolean straightRouteBlocked(Node startNode, Node endNode) {
		double manhattan = getManhattanDistance(startNode.nodeIndex, endNode.nodeIndex);

		if (manhattan <= sightLimit) {
			double shortestPath = getShortestPathDistance(startNode.nodeIndex, endNode.nodeIndex);
			return (manhattan == shortestPath);
		}
		return false;
	}

	/**
	 * init.
	 *
	 * @param initialMaze the initial maze
	 */
	private void init(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, false);
		}

		currentMaze = mazes[mazeIndex];
		setPills();
		initGhosts();

		// RANDOM INIT
		computeRandomInitialPosition();
		internalPacman = new PacMan(initialNode, initialMove, NUM_LIVES, false);

		// FIXED INIT
		// internalPacman = new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT,
		// NUM_LIVES, false);
	}

	private void initRandomlyEdible(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, false);
		}

		currentMaze = mazes[mazeIndex];
		setPills();
		initGhostsRandomlyEdible();

		// RANDOM INIT
		computeRandomInitialPosition();
		internalPacman = new PacMan(initialNode, initialMove, NUM_LIVES, false);

		// FIXED INIT
		// internalPacman = new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT,
		// NUM_LIVES, false);
	}

	private void initRandomly(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, false);
		}

		currentMaze = mazes[mazeIndex];
		// RANDOM INIT PILLS
		setRandomPills();

		// RANDOM INIT GHOSTS
		initGhostsRandomly();

		// RANDOM INIT
		computeRandomInitialPosition();
		internalPacman = new PacMan(initialNode, initialMove, NUM_LIVES, false);

		// FIXED INIT
		// internalPacman = new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT,
		// NUM_LIVES, false);
	}

	private void initOnlyNotEdible(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, false);
		}

		currentMaze = mazes[mazeIndex];
		// RANDOM INIT PILLS
		setPillsOnlyNotEdible();

		// RANDOM INIT GHOSTS
		initGhosts();

		/*
		 * //RANDOM INIT MSPACMAN int initialNode = 0; do { initialNode =
		 * (int)(Math.random()*(double)currentMaze.graph.length); }while(initialNode ==
		 * currentMaze.lairNodeIndex); MOVE[] poss = this.getPossibleMoves(initialNode);
		 * MOVE init = poss[0]; internalPacman = new PacMan(initialNode, init,
		 * NUM_LIVES, false);
		 */
		// FIXED INIT
		internalPacman = new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT, NUM_LIVES, false);

	}

	private void initRandomlyOnlyNotEdible(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, false);
		}

		currentMaze = mazes[mazeIndex];
		// RANDOM INIT PILLS
		setRandomPillsOnlyNotEdible();
		// setPills();

		// RANDOM INIT GHOSTS
		initGhostsRandomlyOnlyNotEdible();
		// initGhosts();

		// RANDOM INIT MSPACMAN
		int initialNode = 0;
		do {
			initialNode = (int) (Math.random() * (double) currentMaze.graph.length);

		} while (initialNode == currentMaze.lairNodeIndex);

		MOVE[] poss = this.getPossibleMoves(initialNode);
		MOVE init = MOVE.NEUTRAL;
		internalPacman = new PacMan(initialNode, init, 1, false);
	}

	private void initPills(int initialMaze) {
		mazeIndex = initialMaze;
		score = currentLevelTime = levelCount = totalTime = 0;
		ghostEatMultiplier = 1;
		gameOver = false;
		timeOfLastGlobalReversal = -1;
		pacmanWasEaten = false;
		pillWasEaten = false;
		powerPillWasEaten = false;

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, false);
		}

		currentMaze = mazes[mazeIndex];
		// RANDOM INIT PILLS
		// setRandomPills();
		setPills();

		// RANDOM INIT GHOSTS
		initLairGhosts();

		// RANDOM INIT
		computeRandomInitialPosition();
		internalPacman = new PacMan(initialNode, initialMove, NUM_LIVES, false);

		// FIXED INIT
		// internalPacman = new PacMan(currentMaze.initialPacManNodeIndex, MOVE.LEFT,
		// NUM_LIVES, false);
	}

	private void computeRandomInitialPosition() {

		do {
			initialNode = (int) (Math.random() * (double) currentMaze.graph.length);
		} while (initialNode == currentMaze.lairNodeIndex);
		MOVE[] poss = this.getPossibleMoves(initialNode);
		initialMove = poss[0].opposite();
		// currentMaze.initialPacManNodeIndex = initialNode;
	}

	private void setRandomPills() {
		Random rnd = new Random();
		if (pillsPresent) {
			pills = new BitSet(currentMaze.pillIndices.length);
			pills.set(0, currentMaze.pillIndices.length);
			for (int i = 0; i < pills.size(); i++) {
				if (rnd.nextBoolean())
					pills.clear(i);
			}
		}
		if (getNumberOfActivePills() == 0) {
			int n = rnd.nextInt(currentMaze.pillIndices.length);
			pills.set(n, n + 1);
		}
		if (powerPillsPresent) {
			powerPills = new BitSet(currentMaze.powerPillIndices.length);
			powerPills.set(0, currentMaze.powerPillIndices.length);
			for (int i = 0; i < powerPills.size(); i++) {
				if (rnd.nextBoolean())
					powerPills.clear(i);
			}
		}
	}

	private void setRandomPillsOnlyNotEdible() {
		Random rnd = new Random();
		if (pillsPresent) {
			pills = new BitSet(currentMaze.pillIndices.length);
			pills.set(0, currentMaze.pillIndices.length);
			for (int i = 0; i < pills.size(); i++) {
				if (rnd.nextBoolean())
					pills.clear(i);
			}
		}
		if (getNumberOfActivePills() == 0) {
			int n = rnd.nextInt(currentMaze.pillIndices.length);
			pills.set(n, n + 1);
		}
		if (powerPillsPresent) {
			powerPills = new BitSet(currentMaze.powerPillIndices.length);
			powerPills.set(0, currentMaze.powerPillIndices.length);
			for (int i = 0; i < powerPills.size(); i++) {
				powerPills.clear(i);
			}
		}
	}

	private void setPillsOnlyNotEdible() {
		if (pillsPresent) {
			pills = new BitSet(currentMaze.pillIndices.length);
			pills.set(0, currentMaze.pillIndices.length);
		}
		if (powerPillsPresent) {
			powerPills = new BitSet(currentMaze.powerPillIndices.length);
			powerPills.set(0, currentMaze.powerPillIndices.length);
			for (int i = 0; i < powerPills.size(); i++) {
				powerPills.clear(i);
			}
		}
	}

	private void initLairGhosts() {
		ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {
			ghosts.put(ghostType, new Ghost(ghostType, currentMaze.lairNodeIndex, 0, Integer.MAX_VALUE, MOVE.NEUTRAL));
		}
	}

	private void initGhostsRandomly() {
		Random rnd = new Random();

		int currentNodeIndex;
		int edibleTime;
		int lairTime;
		MOVE lastMoveMade;

		ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {

			currentNodeIndex = (int) (Math.random() * (double) currentMaze.graph.length);
			if (rnd.nextBoolean())
				edibleTime = rnd.nextInt(
						(int) (EDIBLE_TIME * (Math.pow(EDIBLE_TIME_REDUCTION, levelCount % LEVEL_RESET_REDUCTION))));
			else
				edibleTime = 0;

			if (currentNodeIndex == currentMaze.lairNodeIndex)
				lairTime = rnd.nextInt((int) (ghostType.initialLairTime
						* (Math.pow(LAIR_REDUCTION, levelCount % LEVEL_RESET_REDUCTION))) - 1) + 1;
			else
				lairTime = 0;
			lastMoveMade = MOVE.NEUTRAL;

			ghosts.put(ghostType, new Ghost(ghostType, currentNodeIndex, edibleTime, lairTime, lastMoveMade));
		}
	}

	private void initGhostsRandomlyEdible() {
		int currentNodeIndex;

		ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {

			currentNodeIndex = (int) (Math.random() * (double) currentMaze.graph.length);

			while (currentNodeIndex == currentMaze.lairNodeIndex) {
				currentNodeIndex = (int) (Math.random() * (double) currentMaze.graph.length);
			}

			ghosts.put(ghostType, new Ghost(ghostType, currentNodeIndex, this.ghostDefaultEdibleTime, 0, MOVE.NEUTRAL));
		}
	}

	private void initGhostsRandomlyOnlyNotEdible() {
		Random rnd = new Random();

		int currentNodeIndex;
		int edibleTime;
		int lairTime;
		MOVE lastMoveMade;

		ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {

			currentNodeIndex = (int) (Math.random() * (double) currentMaze.graph.length);
			edibleTime = 0;

			if (currentNodeIndex == currentMaze.lairNodeIndex)
				lairTime = rnd.nextInt((int) (ghostType.initialLairTime
						* (Math.pow(LAIR_REDUCTION, levelCount % LEVEL_RESET_REDUCTION))) - 1) + 1;
			else
				lairTime = 0;
			lastMoveMade = MOVE.NEUTRAL;

			ghosts.put(ghostType, new Ghost(ghostType, currentNodeIndex, edibleTime, lairTime, lastMoveMade));
		}
	}

	int initialNode = 0;
	MOVE initialMove = MOVE.NEUTRAL;

	/**
	 * _new level reset.
	 */
	private void newLevelReset() {
		mazeIndex = ++mazeIndex % NUM_MAZES;
		levelCount++;
		currentMaze = mazes[mazeIndex];

		currentLevelTime = 0;
		ghostEatMultiplier = 1;

		setPills();
		levelReset();
	}

	/**
	 * _level reset.
	 */
	private void levelReset() {
		ghostEatMultiplier = 1;

		initGhosts();

		computeRandomInitialPosition();

		internalPacman.currentNodeIndex = initialNode;
		internalPacman.lastMoveMade = initialMove;
		internalPacman.lastDir = initialMove;
	}

	/**
	 * _set pills.
	 *
	 *
	 */
	private void setPills() {
		if (pillsPresent) {
			pills = new BitSet(currentMaze.pillIndices.length);
			pills.set(0, currentMaze.pillIndices.length);
		}
		if (powerPillsPresent) {
			powerPills = new BitSet(currentMaze.powerPillIndices.length);
			powerPills.set(0, currentMaze.powerPillIndices.length);
		}
	}

	/**
	 * init ghosts.
	 */
	private void initGhosts() {
		ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {
			ghosts.put(ghostType, new Ghost(ghostType, currentMaze.lairNodeIndex, this.ghostDefaultEdibleTime,
					(int) (ghostType.initialLairTime * (Math.pow(LAIR_REDUCTION, levelCount % LEVEL_RESET_REDUCTION))),
					MOVE.NEUTRAL));
		}
	}

	/**
	 * Gets the game state as a string: all variables are written to a string in a
	 * pre-determined order. The string may later be used to recreate a game state
	 * using the setGameState() method.
	 * <p>
	 * Variables not included: enableGlobalReversals
	 *
	 * @return The game state as a string
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public String getGameState() {
		if (po) {
			return "";
		}
		StringBuilder sb = new StringBuilder();

		sb.append(mazeIndex + "," + totalTime + "," + score + "," + currentLevelTime + "," + levelCount + ","
				+ internalPacman.currentNodeIndex + "," + internalPacman.lastMoveMade + ","
				+ internalPacman.numberOfLivesRemaining + "," + internalPacman.hasReceivedExtraLife + ",");

		for (Ghost ghost : ghosts.values()) {
			sb.append(ghost.currentNodeIndex + "," + ghost.edibleTime + "," + ghost.lairTime + "," + ghost.lastMoveMade
					+ ",");
		}

		for (int i = 0; i < currentMaze.pillIndices.length; i++) {
			if (pills.get(i)) {
				sb.append("1");
			} else {
				sb.append("0");
			}
		}

		sb.append(",");

		for (int i = 0; i < currentMaze.powerPillIndices.length; i++) {
			if (powerPills.get(i)) {
				sb.append("1");
			} else {
				sb.append("0");
			}
		}

		sb.append(",");
		sb.append(timeOfLastGlobalReversal);
		sb.append(",");
		sb.append(pacmanWasEaten);
		sb.append(",");

		for (GHOST ghost : GHOST.values()) {
			sb.append(ghostsEaten.get(ghost));
			sb.append(",");
		}

		sb.append(pillWasEaten);
		sb.append(",");
		sb.append(powerPillWasEaten);

		return sb.toString();
	}

	/**
	 * Sets the game state from a string: the inverse of getGameState(). It
	 * reconstructs all the game's variables from the string.
	 *
	 * @param gameState The game state represented as a string
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void setGameState(String gameState) {
		String[] values = gameState.split(",");

		int index = 0;

		mazeIndex = Integer.parseInt(values[index++]);
		totalTime = Integer.parseInt(values[index++]);
		score = Integer.parseInt(values[index++]);
		currentLevelTime = Integer.parseInt(values[index++]);
		levelCount = Integer.parseInt(values[index++]);

		internalPacman = new PacMan(Integer.parseInt(values[index++]), MOVE.valueOf(values[index++]),
				Integer.parseInt(values[index++]), Boolean.parseBoolean(values[index++]));

		ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {
			ghosts.put(ghostType,
					new Ghost(ghostType, Integer.parseInt(values[index++]), Integer.parseInt(values[index++]),
							Integer.parseInt(values[index++]), MOVE.valueOf(values[index++])));
		}

		currentMaze = mazes[mazeIndex];
		setPills();

		for (int i = 0; i < values[index].length(); i++) {
			if (values[index].charAt(i) == '1') {
				pills.set(i);
			} else {
				pills.clear(i);
			}
		}

		index++;

		for (int i = 0; i < values[index].length(); i++) {
			if (values[index].charAt(i) == '1') {
				powerPills.set(i);
			} else {
				powerPills.clear(i);
			}
		}

		timeOfLastGlobalReversal = Integer.parseInt(values[++index]);
		pacmanWasEaten = Boolean.parseBoolean(values[++index]);

		ghostsEaten = new EnumMap<>(GHOST.class);

		for (GHOST ghost : GHOST.values()) {
			ghostsEaten.put(ghost, Boolean.parseBoolean(values[++index]));
		}

		pillWasEaten = Boolean.parseBoolean(values[++index]);
		powerPillWasEaten = Boolean.parseBoolean(values[++index]);
	}

	/**
	 * Returns an exact copy of the game. This may be used for forward searches such
	 * as minimax. The copying is relatively efficient.
	 * <p>
	 * Copy will respect previous PO constraints
	 * <p>
	 * Need to dissallow the forwarding of games that have PO enabled. Then allow a
	 * copy of the game to be made with some information provided.
	 *
	 * @param copyMessenger should the messenger be deep copied or not
	 * @return the game
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Game copy(boolean copyMessenger) {
		Game copy = new Game(this.poType, this.sightLimit);

		copy.seed = seed;
		copy.rnd = new Random();
		copy.currentMaze = currentMaze;
		copy.pills = (BitSet) pills.clone();
		copy.powerPills = (BitSet) powerPills.clone();
		copy.mazeIndex = mazeIndex;
		copy.levelCount = levelCount;
		copy.currentLevelTime = currentLevelTime;
		copy.totalTime = totalTime;
		copy.score = score;
		copy.ghostEatMultiplier = ghostEatMultiplier;
		copy.gameOver = gameOver;
		copy.timeOfLastGlobalReversal = timeOfLastGlobalReversal;
		copy.pacmanWasEaten = pacmanWasEaten;
		copy.pillWasEaten = pillWasEaten;
		copy.powerPillWasEaten = powerPillWasEaten;
		copy.internalPacman = internalPacman.copy();

		copy.ghostsPresent = ghostsPresent;
		copy.pillsPresent = pillsPresent;
		copy.powerPillsPresent = powerPillsPresent;

		copy.ghostsEaten = new EnumMap<>(GHOST.class);
		copy.ghosts = new EnumMap<>(GHOST.class);

		for (GHOST ghostType : GHOST.values()) {
			copy.ghosts.put(ghostType, ghosts.get(ghostType).copy());
			copy.ghostsEaten.put(ghostType, ghostsEaten.get(ghostType));
		}

		copy.po = this.po;
		copy.agent = this.agent;
		if (hasMessaging()) {
			copy.messenger = (copyMessenger) ? messenger.copy() : this.messenger;
		}

		return copy;
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Game copy() {
		return copy(false);
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Game copy(GHOST ghost) {
		return copy(ghost, false);
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Game copy(GHOST ghost, boolean copyMessenger) {
		Game game = copy(copyMessenger);
		game.po = true;
		game.agent = ghost.ordinal();
		return game;
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Game copy(int agent) {
		Game game = copy();
		if (agent == CLONE) {
			return game;
		}
		game.po = true;
		game.agent = agent;
		return game;
	}

	private boolean canBeForwarded() {
		return !po || beenBlanked;
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////// Game-engine //////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Central method that advances the game state using the moves supplied by the
	 * controllers. It first updates Ms Pac-Man, then the ghosts and then the
	 * general game logic.
	 *
	 * @param pacManMove The move supplied by the Ms Pac-Man controller
	 * @param ghostMoves The moves supplied by the ghosts controller
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void advanceGame(MOVE pacManMove, Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		updatePacMan(pacManMove);
		updateGhosts(ghostMoves);
		updateGame();
	}

	public void advanceGameTrain(MOVE pacManMove, Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		updatePacMan(pacManMove);
		updateGhosts(ghostMoves);
		updateGame();
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void advanceGameWithoutReverse(MOVE pacManMove, Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		updatePacMan(pacManMove);
		updateGhostsWithoutReverse(ghostMoves);
		updateGame();
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void advanceGameWithForcedReverse(MOVE pacManMove, Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		updatePacMan(pacManMove);
		updateGhostsWithForcedReverse(ghostMoves);
		updateGame();
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void advanceGameWithPowerPillReverseOnly(MOVE pacManMove, Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		updatePacMan(pacManMove);

		if (powerPillWasEaten) {
			updateGhostsWithForcedReverse(ghostMoves);
		} else {
			updateGhostsWithoutReverse(ghostMoves);
		}

		updateGame();
	}

	/**
	 * Updates the state of Ms Pac-Man given the move returned by the controller.
	 *
	 * @param pacManMove The move supplied by the Ms Pac-Man controller
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void updatePacMan(MOVE pacManMove) {
		if (!canBeForwarded()) {
			return;
		}
		_updatePacMan(pacManMove); // move pac-man
		eatPill(); // eat a pill
		if (this.isTraining)
			dontEatPowerPill();
		else
			eatPowerPill();

	}

	/**
	 * Updates the states of the ghosts given the moves returned by the controller.
	 *
	 * @param ghostMoves The moves supplied by the ghosts controller
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void updateGhosts(Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		if (!ghostsPresent) {
			return;
		}
		Map<GHOST, MOVE> completedGhostMoves = completeGhostMoves(ghostMoves);

		if (!reverseGhosts(completedGhostMoves, false)) {
			_updateGhosts(completedGhostMoves);
		}
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void updateGhostsWithoutReverse(Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		if (!ghostsPresent) {
			return;
		}
		_updateGhosts(completeGhostMoves(ghostMoves));
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void updateGhostsWithForcedReverse(Map<GHOST, MOVE> ghostMoves) {
		if (!canBeForwarded()) {
			return;
		}
		if (!ghostsPresent) {
			return;
		}
		reverseGhosts(completeGhostMoves(ghostMoves), true);
	}

	/**
	 * Updates the game once the individual characters have been updated: check if
	 * anyone can eat anyone else. Then update the lair times and check if Ms
	 * Pac-Man should be awarded the extra live. Then update the time and see if the
	 * level or game is over.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void updateGame() {
		if (!canBeForwarded()) {
			return;
		}
		feast(); // ghosts eat pac-man or vice versa
		updateLairTimes();
		updatePacManExtraLife();

		totalTime++;
		currentLevelTime++;

		checkLevelState(); // check if level/game is over
		if (messenger != null) {
			messenger.update();
		}
	}

	/**
	 * This method is for specific purposes such as searching a tree in a specific
	 * manner. It has to be used cautiously as it might create an unstable game
	 * state and may cause the game to crash.
	 *
	 * @param feast           Whether or not to enable feasting
	 * @param updateLairTimes Whether or not to update the lair times
	 * @param updateExtraLife Whether or not to update the extra life
	 * @param updateTotalTime Whether or not to update the total time
	 * @param updateLevelTime Whether or not to update the level time
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void updateGame(boolean feast, boolean updateLairTimes, boolean updateExtraLife, boolean updateTotalTime,
			boolean updateLevelTime) {
		if (!canBeForwarded()) {
			return;
		}
		if (feast) {
			feast(); // ghosts eat pac-man or vice versa
		}
		if (updateLairTimes) {
			updateLairTimes();
		}
		if (updateExtraLife) {
			updatePacManExtraLife();
		}

		if (updateTotalTime) {
			totalTime++;
		}
		if (updateLevelTime) {
			currentLevelTime++;
		}

		checkLevelState(); // check if level/game is over
		if (messenger != null) {
			messenger.update();
		}
	}

	/**
	 * _update lair times.
	 */
	private void updateLairTimes() {
		if (!ghostsPresent) {
			return;
		}
		for (Ghost ghost : ghosts.values()) {
			if (ghost.lairTime > 0) {
				if (--ghost.lairTime == 0) {
					ghost.currentNodeIndex = currentMaze.initialGhostNodeIndex;
				}
			}
		}
	}

	/**
	 * _update pac man extra life.
	 */
	private void updatePacManExtraLife() {
		if (!internalPacman.hasReceivedExtraLife && score >= EXTRA_LIFE_SCORE) // award 1 extra life at 10000 points
		{
			internalPacman.hasReceivedExtraLife = true;
			internalPacman.numberOfLivesRemaining++;
		}
	}

	/**
	 * _update pac man.
	 *
	 * @param move the move
	 */
	private void _updatePacMan(MOVE move) {
		internalPacman.lastMoveMade = correctPacManDir(move);
		internalPacman.currentNodeIndex = internalPacman.lastMoveMade == MOVE.NEUTRAL ? internalPacman.currentNodeIndex
				: currentMaze.graph[internalPacman.currentNodeIndex].neighbourhood.get(internalPacman.lastMoveMade);
	}

	/**
	 * _correct pac man dir.
	 *
	 * @param direction the direction
	 * @return the mOVE
	 */
	private MOVE correctPacManDir(MOVE direction) {
		Node node = currentMaze.graph[internalPacman.currentNodeIndex];

		try {
			// direction is correct, return it
			if (node.neighbourhood.containsKey(direction)/* &&(direction != internalPacman.lastDir.opposite()) */) { // TODO
																														// changed
				internalPacman.lastDir = direction;
				return direction;
			} else {
				// try to use previous direction (i.e., continue in the same direction)
				if (node.neighbourhood.containsKey(internalPacman.lastMoveMade)) {
					return internalPacman.lastMoveMade;
					// else stay put
				} else {
					MOVE[] moves = node.allPossibleMoves.get(internalPacman.lastDir);
					if (moves == null)
						return internalPacman.lastDir;
					for (MOVE v : moves)
						if (v != internalPacman.lastDir.opposite()) {
							internalPacman.lastDir = v;
							return v;
						}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return MOVE.NEUTRAL;
	}

	/**
	 * _update ghosts.
	 *
	 * @param moves the moves
	 */
	private void _updateGhosts(Map<GHOST, MOVE> moves) {
		for (Entry<GHOST, MOVE> entry : moves.entrySet()) {
			Ghost ghost = ghosts.get(entry.getKey());

			if (ghost.lairTime == 0) {
				if (ghost.edibleTime == 0 || ghost.edibleTime % GHOST_SPEED_REDUCTION != 0) {
					ghost.lastMoveMade = checkGhostDir(ghost, entry.getValue());
					moves.put(entry.getKey(), ghost.lastMoveMade);
					ghost.currentNodeIndex = currentMaze.graph[ghost.currentNodeIndex].neighbourhood
							.get(ghost.lastMoveMade);
				}
			}
		}
	}

	private Map<GHOST, MOVE> completeGhostMoves(Map<GHOST, MOVE> moves) {
		Map<GHOST, MOVE> ghostMoves = moves;
		if (ghostMoves == null) {
			ghostMoves = new EnumMap<>(GHOST.class);

			for (Map.Entry<GHOST, Ghost> entry : ghosts.entrySet()) {
				ghostMoves.put(entry.getKey(), entry.getValue().lastMoveMade);
			}
		}

		if (ghostMoves.size() < NUM_GHOSTS) {
			for (GHOST ghostType : ghosts.keySet()) {
				if (!ghostMoves.containsKey(ghostType)) {
					ghostMoves.put(ghostType, MOVE.NEUTRAL);
				}
			}
		}

		return ghostMoves;
	}

	/**
	 * _check ghost dir.
	 *
	 * @param ghost     the ghost
	 * @param direction the direction
	 * @return the mOVE
	 */
	private MOVE checkGhostDir(Ghost ghost, MOVE direction) {
		// Gets the neighbours of the node with the node that would correspond to
		// reverse removed
		Node node = currentMaze.graph[ghost.currentNodeIndex];

		// The direction is possible and not opposite to the previous direction of that
		// ghost
		if (node.neighbourhood.containsKey(direction) && direction != ghost.lastMoveMade.opposite()) {
			return direction;
		} else {
			if (node.neighbourhood.containsKey(ghost.lastMoveMade)) {
				return ghost.lastMoveMade;
			} else {
				MOVE[] moves = node.allPossibleMoves.get(ghost.lastMoveMade);
				return moves[rnd.nextInt(moves.length)];
			}
		}
	}

	/**
	 * _eat pill.
	 */
	private void eatPill() {
		pillWasEaten = false;

		int pillIndex = currentMaze.graph[internalPacman.currentNodeIndex].pillIndex;

		if (pillIndex >= 0 && pills.get(pillIndex)) {
			score += PILL;
			pills.clear(pillIndex);
			pillWasEaten = true;
		}
	}

	/**
	 * _eat power pill.
	 */
	private void eatPowerPill() {
		powerPillWasEaten = false;

		int powerPillIndex = currentMaze.graph[internalPacman.currentNodeIndex].powerPillIndex;

		if (powerPillIndex >= 0 && powerPills.get(powerPillIndex)) {
			score += POWER_PILL;
			ghostEatMultiplier = 1;
			powerPills.clear(powerPillIndex);

			int newEdibleTime = (int) (EDIBLE_TIME
					* (Math.pow(EDIBLE_TIME_REDUCTION, levelCount % LEVEL_RESET_REDUCTION)));

			for (Ghost ghost : ghosts.values()) {
				if (ghost.lairTime == 0) {
					ghost.edibleTime = newEdibleTime;
				} else {
					ghost.edibleTime = 0;
				}
			}

			powerPillWasEaten = true;
		}
	}

	private void dontEatPowerPill() {
		powerPillWasEaten = false;

		int powerPillIndex = currentMaze.graph[internalPacman.currentNodeIndex].powerPillIndex;

		if (powerPillIndex >= 0 && powerPills.get(powerPillIndex)) {
			score += POWER_PILL;
			ghostEatMultiplier = 1;
			powerPills.clear(powerPillIndex);
		}
	}

	private boolean reverseGhosts(Map<GHOST, MOVE> moves, boolean force) {
		boolean reversed = false;
		boolean globalReverse = false;

		if (rnd.nextDouble() < GHOST_REVERSAL) {
			globalReverse = true;
		}

		for (Entry<GHOST, MOVE> entry : moves.entrySet()) {
			Ghost ghost = ghosts.get(entry.getKey());

			if (currentLevelTime > 1 && ghost.lairTime == 0 && ghost.lastMoveMade != MOVE.NEUTRAL) {
				if (force || (powerPillWasEaten || globalReverse)) {
					ghost.lastMoveMade = ghost.lastMoveMade.opposite();
					ghost.currentNodeIndex = currentMaze.graph[ghost.currentNodeIndex].neighbourhood
							.get(ghost.lastMoveMade);
					reversed = true;
					timeOfLastGlobalReversal = totalTime;
				}
			}
		}

		return reversed;
	}

	/**
	 * feast.
	 */
	/*
	 * //FEAST WHERE GHOSTS DOESNT EXIT LAIR private void feast() { pacmanWasEaten =
	 * false;
	 * 
	 * for (GHOST ghost : ghosts.keySet()) { ghostsEaten.put(ghost, false); }
	 * 
	 * for (Ghost ghost : ghosts.values()) { int distance =
	 * getShortestPathDistance(internalPacman.currentNodeIndex,
	 * ghost.currentNodeIndex);
	 * 
	 * if (distance <= EAT_DISTANCE && distance != -1) { if (ghost.edibleTime > 0)
	 * //pac-man eats ghost { score += GHOST_EAT_SCORE * ghostEatMultiplier;
	 * ghostEatMultiplier *= 2; ghost.edibleTime = this.ghostDefaultEdibleTime;
	 * ghost.lairTime = Integer.MAX_VALUE; ghost.currentNodeIndex =
	 * currentMaze.lairNodeIndex; ghost.lastMoveMade = MOVE.NEUTRAL;
	 * 
	 * ghostsEaten.put(ghost.type, true); } else //ghost eats pac-man {
	 * internalPacman.numberOfLivesRemaining--; pacmanWasEaten = true;
	 * 
	 * if (internalPacman.numberOfLivesRemaining <= 0) { gameOver = true; } else {
	 * levelReset(); }
	 * 
	 * return; } } } for (Ghost ghost : ghosts.values()) { if (ghost.edibleTime > 0)
	 * { ghost.edibleTime--; } } }
	 */
	// FEAST WHERE GHOSTS EXIT LAIR

	private void feast() {
		pacmanWasEaten = false;

		for (GHOST ghost : ghosts.keySet()) {
			ghostsEaten.put(ghost, false);
		}

		for (Ghost ghost : ghosts.values()) {
			int distance = getShortestPathDistance(internalPacman.currentNodeIndex, ghost.currentNodeIndex);

			if (distance <= EAT_DISTANCE && distance != -1) {
				if (ghost.edibleTime > 0) // pac-man eats ghost
				{
					score += GHOST_EAT_SCORE * ghostEatMultiplier;
					ghostEatMultiplier *= 2;
					ghost.edibleTime = this.ghostDefaultEdibleTime;
					ghost.lairTime = (int) (COMMON_LAIR_TIME
							* (Math.pow(LAIR_REDUCTION, levelCount % LEVEL_RESET_REDUCTION)));
					ghost.currentNodeIndex = currentMaze.lairNodeIndex;
					ghost.lastMoveMade = MOVE.NEUTRAL;

					ghostsEaten.put(ghost.type, true);
				} else // ghost eats pac-man
				{
					internalPacman.numberOfLivesRemaining--;
					pacmanWasEaten = true;

					if (internalPacman.numberOfLivesRemaining <= 0) {
						gameOver = true;
					} else {
						levelReset();
					}

					return;
				}
			}
		}
		for (Ghost ghost : ghosts.values()) {
			if (ghost.edibleTime > 0) {
				ghost.edibleTime--;
			}
		}
	}

	/**
	 * _check level state.
	 */
	private void checkLevelState() {
		// put a cap on the total time a game can be played for
		if (totalTime + 1 > MAX_TIME) {
			gameOver = true;
			score += internalPacman.numberOfLivesRemaining * AWARD_LIFE_LEFT;
		}
		// if all pills have been eaten or the time is up...
		else if ((pills.isEmpty() && powerPills.isEmpty()) || currentLevelTime >= LEVEL_LIMIT) {
			newLevelReset();
		}
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////// Query Methods (return only) ///////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns whether internalPacman was eaten in the last time step
	 *
	 * @return whether Ms Pac-Man was eaten.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean wasPacManEaten() {
		return pacmanWasEaten;
	}

	/**
	 * Returns whether a ghost was eaten in the last time step
	 *
	 * @param ghost the ghost to check
	 * @return whether a ghost was eaten.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean wasGhostEaten(GHOST ghost) {
		return ghostsEaten.get(ghost);
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNumGhostsEaten() {
		int count = 0;
		for (GHOST ghost : ghosts.keySet()) {
			if (ghostsEaten.get(ghost)) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Returns whether a pill was eaten in the last time step
	 *
	 * @return whether a pill was eaten.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean wasPillEaten() {
		return pillWasEaten;
	}

	/**
	 * Returns whether a power pill was eaten in the last time step
	 *
	 * @return whether a power pill was eaten.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean wasPowerPillEaten() {
		return powerPillWasEaten;
	}

	/**
	 * Returns the time when the last global reversal event took place.
	 *
	 * @return time the last global reversal event took place (not including power
	 *         pill reversals)
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getTimeOfLastGlobalReversal() {
		return timeOfLastGlobalReversal;
	}

	/**
	 * Checks whether the game is over or not: all lives are lost or 16 levels have
	 * been played. The variable is set by the methods feast() and
	 * checkLevelState().
	 *
	 * @return true, if successful
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean gameOver() {
		return gameOver;
	}

	/**
	 * Returns the current maze of the game.
	 *
	 * @return The current maze.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Maze getCurrentMaze() {
		return currentMaze;
	}

	/**
	 * Returns the x coordinate of the specified node.
	 *
	 * @param nodeIndex the node index
	 * @return the node x cood
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNodeXCood(int nodeIndex) {
		return currentMaze.graph[nodeIndex].x;
	}

	/**
	 * Returns the y coordinate of the specified node.
	 *
	 * @param nodeIndex The node index
	 * @return The node's y coordinate
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNodeYCood(int nodeIndex) {
		return currentMaze.graph[nodeIndex].y;
	}

	/**
	 * Gets the index of the current maze.
	 *
	 * @return The maze index
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getMazeIndex() {
		return mazeIndex;
	}

	/**
	 * Returns the current level.
	 *
	 * @return The current level
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getCurrentLevel() {
		return levelCount;
	}

	/**
	 * Returns the number of nodes in the current maze.
	 *
	 * @return number of nodes in the current maze.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNumberOfNodes() {
		return currentMaze.graph.length;
	}

	/**
	 * Returns the current value awarded for eating a ghost.
	 *
	 * @return the current value awarded for eating a ghost.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getGhostCurrentEdibleScore() {
		return GHOST_EAT_SCORE * ghostEatMultiplier;
	}

	/**
	 * Returns the node index where ghosts start in the maze once leaving the lair.
	 *
	 * @return the node index where ghosts start after leaving the lair.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getGhostInitialNodeIndex() {
		return currentMaze.initialGhostNodeIndex;
	}

	/**
	 * Returns the node index where Ms. Pac-Man starts in the maze
	 *
	 * @return the node index where Ms. Pac-Man starts in the maze
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getPacManInitialNodeIndex() {
		// return currentMaze.initialPacManNodeIndex;
		return initialNode;
	}

	/**
	 * Whether the pill specified is still there or has been eaten.
	 *
	 * @param pillIndex The pill index
	 * @return true, if is pill still available
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Boolean isPillStillAvailable(int pillIndex) {
		if (po) {
			int pillLocation = currentMaze.pillIndices[pillIndex];
			if (!isNodeObservable(pillLocation)) {
				return null;
			}

		}
		return pills.get(pillIndex);
	}

	/**
	 * Whether the power pill specified is still there or has been eaten.
	 *
	 * @param powerPillIndex The power pill index
	 * @return true, if is power pill still available
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Boolean isPowerPillStillAvailable(int powerPillIndex) {
		if (po) {
			int pillLocation = currentMaze.powerPillIndices[powerPillIndex];
			if (!isNodeObservable(pillLocation)) {
				return null;
			}
		}
		return powerPills.get(powerPillIndex);
	}

	/**
	 * Returns the pill index of the node specified. This can be -1 if there is no
	 * pill at the specified node.
	 *
	 * @param nodeIndex The Index of the node.
	 * @return a number corresponding to the pill index (or -1 if node has no pill)
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getPillIndex(int nodeIndex) {
		return currentMaze.graph[nodeIndex].pillIndex;
	}

	/**
	 * Returns the power pill index of the node specified. This can be -1 if there
	 * is no power pill at the specified node.
	 *
	 * @param nodeIndex The Index of the node.
	 * @return a number corresponding to the power pill index (or -1 if node has no
	 *         pill)
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getPowerPillIndex(int nodeIndex) {
		return currentMaze.graph[nodeIndex].powerPillIndex;
	}

	/**
	 * Returns the array of node indices that are junctions (3 or more neighbours).
	 *
	 * @return the junction indices
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getJunctionIndices() {
		return Arrays.clone(currentMaze.junctionIndices);
	}

	/**
	 * Returns the indices to all the nodes that have pills.
	 *
	 * @return the pill indices
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getPillIndices() {
		return Arrays.clone(currentMaze.pillIndices);
	}

	/**
	 * Returns the indices to all the nodes that have power pills.
	 *
	 * @return the power pill indices
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getPowerPillIndices() {
		return Arrays.clone(currentMaze.powerPillIndices);
	}

	/**
	 * Current node index of Ms Pac-Man.
	 *
	 * @return the internalPacman current node index
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getPacmanCurrentNodeIndex() {
		if (po && !isNodeObservable(internalPacman.currentNodeIndex)) {
			return -1;
		}
		return internalPacman.currentNodeIndex;
	}

	/**
	 * Current node index of Ms Pac-Man.
	 *
	 * @return the internalPacman last move made
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getPacmanLastMoveMade() {
		if (po && !isNodeObservable(internalPacman.currentNodeIndex)) {
			return null;
		}
		return internalPacman.lastMoveMade;
	}

	/**
	 * Lives that remain for Ms Pac-Man.
	 *
	 * @return the number of lives remaining
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getPacmanNumberOfLivesRemaining() {
		return internalPacman.numberOfLivesRemaining;
	}

	/**
	 * Current node at which the specified ghost resides.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost current node index
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getGhostCurrentNodeIndex(GHOST ghostType) {
		if (po) {
			int index = ghosts.get(ghostType).currentNodeIndex;
			return isNodeObservable(index) ? index : -1;
		}
		return ghosts.get(ghostType).currentNodeIndex;
	}

	/**
	 * Current direction of the specified ghost.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost last move made
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getGhostLastMoveMade(GHOST ghostType) {
		if (po) {
			Ghost ghost = ghosts.get(ghostType);
			return isNodeObservable(ghost.currentNodeIndex) ? ghost.lastMoveMade : null;
		}
		return ghosts.get(ghostType).lastMoveMade;
	}

	/**
	 * Returns the edible time for the specified ghost.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost edible time
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getGhostEdibleTime(GHOST ghostType) {
		if (po) {
			Ghost ghost = ghosts.get(ghostType);
			return isNodeObservable(ghost.currentNodeIndex) ? ghost.edibleTime : -1;
		}
		return ghosts.get(ghostType).edibleTime;
	}

	/**
	 * Simpler check to see if a ghost is edible.
	 *
	 * @param ghostType the ghost type
	 * @return true, if is ghost edible
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Boolean isGhostEdible(GHOST ghostType) {
		if (po) {
			Ghost ghost = ghosts.get(ghostType);
			return (isNodeObservable(ghost.currentNodeIndex)) ? ghost.edibleTime > 0 : null;
		}
		return ghosts.get(ghostType).edibleTime > 0;
	}

	/**
	 * Returns the score of the game.
	 *
	 * @return the score
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getScore() {
		return score;
	}

	/**
	 * Returns the time of the current level (important with respect to
	 * LEVEL_LIMIT).
	 *
	 * @return the current level time
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getCurrentLevelTime() {
		return currentLevelTime;
	}

	/**
	 * Total time the game has been played for (at most LEVEL_LIMIT*MAX_LEVELS).
	 *
	 * @return the total time
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getTotalTime() {
		return totalTime;
	}

	/**
	 * Total number of pills in the mazes[gs.curMaze]
	 *
	 * @return the number of pills
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNumberOfPills() {
		return currentMaze.pillIndices.length;
	}

	/**
	 * Total number of power pills in the mazes[gs.curMaze]
	 *
	 * @return the number of power pills
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNumberOfPowerPills() {
		return currentMaze.powerPillIndices.length;
	}

	/**
	 * Total number of pills in the mazes[gs.curMaze]
	 *
	 * @return the number of active pills
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNumberOfActivePills() {
		return pills.cardinality();
	}

	/**
	 * Total number of power pills in the mazes[gs.curMaze]
	 *
	 * @return the number of active power pills
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNumberOfActivePowerPills() {
		return powerPills.cardinality();
	}

	/**
	 * Time left that the specified ghost will spend in the lair.
	 *
	 * @param ghostType the ghost type
	 * @return the ghost lair time
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getGhostLairTime(GHOST ghostType) {
		if (po) {
			Ghost ghost = ghosts.get(ghostType);
			return isNodeObservable(ghost.currentNodeIndex) ? ghost.lairTime : -1;
		}
		return ghosts.get(ghostType).lairTime;
	}

	/**
	 * Returns the indices of all active pills in the mazes[gs.curMaze]
	 *
	 * @return the active pills indices
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getActivePillsIndices() {
		int[] indices = new int[pills.cardinality()];

		int index = 0;

		for (int i = 0; i < currentMaze.pillIndices.length; i++) {
			if (!po || isNodeObservable(currentMaze.pillIndices[i])) {
				if (pills.get(i)) {
					indices[index++] = currentMaze.pillIndices[i];
				}
			}
		}
		if (index != indices.length) {
			int[] results = new int[index];
			System.arraycopy(indices, 0, results, 0, index);
			return results;
		}
		return indices;
	}

	/**
	 * Returns the indices of all active power pills in the mazes[gs.curMaze]
	 *
	 * @return the active power pills indices
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getActivePowerPillsIndices() {
		int[] indices = new int[powerPills.cardinality()];

		int index = 0;

		for (int i = 0; i < currentMaze.powerPillIndices.length; i++) {
			if (!po || isNodeObservable(currentMaze.powerPillIndices[i])) {
				if (powerPills.get(i)) {
					indices[index++] = currentMaze.powerPillIndices[i];
				}
			}
		}
		if (index != indices.length) {
			int[] results = new int[index];
			System.arraycopy(indices, 0, results, 0, index);
			return results;
		}
		return indices;
	}

	/**
	 * s If in lair (getLairTime(-) &gt; 0) or if not at junction.
	 *
	 * @param ghostType the ghost type
	 * @return true, if successful
	 */
	// @SuppressWarnings({"WeakerAccess", "unused"})
	public Boolean doesGhostRequireAction(GHOST ghostType) {
		// inlcude neutral here for the unique case where the ghost just left the lair
		/*
		 * if (!po || isNodeObservable(ghosts.get(ghostType).currentNodeIndex)) { return
		 * ((isJunction(ghosts.get(ghostType).currentNodeIndex) ||
		 * (ghosts.get(ghostType).lastMoveMade == MOVE.NEUTRAL) &&
		 * ghosts.get(ghostType).currentNodeIndex == currentMaze.initialGhostNodeIndex)
		 * && (ghosts.get(ghostType).edibleTime == 0 || ghosts.get(ghostType).edibleTime
		 * % GHOST_SPEED_REDUCTION != 0)); } else { return null; }
		 */
		return ((isJunction(ghosts.get(ghostType).currentNodeIndex)
				|| (ghosts.get(ghostType).lastMoveMade == MOVE.NEUTRAL)
						&& ghosts.get(ghostType).currentNodeIndex == currentMaze.initialGhostNodeIndex)
				&& (ghosts.get(ghostType).edibleTime == 0
						|| ghosts.get(ghostType).edibleTime % GHOST_SPEED_REDUCTION != 0));
	}

	/**
	 * Checks if the node specified by the nodeIndex is a junction.
	 *
	 * @param nodeIndex the node index
	 * @return true, if is junction
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean isJunction(int nodeIndex) {
		return currentMaze.graph[nodeIndex].numNeighbouringNodes > 2;
	}

	/**
	 * Gets the possible moves from the node index specified.
	 *
	 * @param nodeIndex The current node index
	 * @return The set of possible moves
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE[] getPossibleMoves(int nodeIndex) {
		return currentMaze.graph[nodeIndex].allPossibleMoves.get(MOVE.NEUTRAL).clone();
	}

	/**
	 * Gets the possible moves except the one that corresponds to the reverse of the
	 * move supplied.
	 *
	 * @param nodeIndex    The current node index
	 * @param lastModeMade The last mode made (possible moves will exclude the
	 *                     reverse)
	 * @return The set of possible moves
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE[] getPossibleMoves(int nodeIndex, MOVE lastModeMade) {
		MOVE[] moves = currentMaze.graph[nodeIndex].allPossibleMoves.get(lastModeMade);
		if (moves == null)
			moves = new MOVE[0];
		else
			moves = moves.clone();
		return moves;
	}

	/**
	 * Gets the neighbouring nodes from the current node index.
	 *
	 * @param nodeIndex The current node index
	 * @return The set of neighbouring nodes
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public final int[] getNeighbouringNodes(int nodeIndex) {
		return Arrays.clone(currentMaze.graph[nodeIndex].allNeighbouringNodes.get(MOVE.NEUTRAL));
	}

	/**
	 * Gets the neighbouring nodes from the current node index excluding the node
	 * that corresponds to the opposite of the last move made which is given as an
	 * argument.
	 *
	 * @param nodeIndex    The current node index
	 * @param lastModeMade The last mode made
	 * @return The set of neighbouring nodes except the one that is opposite of the
	 *         last move made
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public final int[] getNeighbouringNodes(int nodeIndex, MOVE lastModeMade) {
		try {
			return Arrays.clone(currentMaze.graph[nodeIndex].allNeighbouringNodes.get(lastModeMade));
		} catch (Exception e) {
			System.err.println(String.format("Error getNeighbouringNodes(nodeIndex: %s, lastMoveMade: %s)", nodeIndex,
					lastModeMade));// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new int[0];
	}

	/**
	 * Given a node index and a move to be made, it returns the node index the move
	 * takes one to. If there is no neighbour in that direction, the method returns
	 * -1.
	 *
	 * @param nodeIndex    The current node index
	 * @param moveToBeMade The move to be made
	 * @return The node index of the node the move takes one to
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getNeighbour(int nodeIndex, MOVE moveToBeMade) {
		Integer neighbour = currentMaze.graph[nodeIndex].neighbourhood.get(moveToBeMade);

		return neighbour == null ? -1 : neighbour;
	}

	/**
	 * Method that returns the direction to take given a node index and an index of
	 * a neighbouring node. Returns null if the neighbour is invalid.
	 *
	 * @param currentNodeIndex   The current node index.
	 * @param neighbourNodeIndex The direct neighbour (node index) of the current
	 *                           node.
	 * @return the move to make to reach direct neighbour
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getMoveToMakeToReachDirectNeighbour(int currentNodeIndex, int neighbourNodeIndex) {
		for (MOVE move : MOVE.values()) {
			if (currentMaze.graph[currentNodeIndex].neighbourhood.containsKey(move)
					&& currentMaze.graph[currentNodeIndex].neighbourhood.get(move) == neighbourNodeIndex) {
				return move;
			}
		}

		return null;
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////// Helper Methods (computational) ////////////////////////
	/////////////////////////////////////////////////////////////////////////////

	/**
	 * Returns the PATH distance from any node to any other node.
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex   the to node index
	 * @return the shortest path distance
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getShortestPathDistance(int fromNodeIndex, int toNodeIndex) {

		try {
			if (toNodeIndex == -1)
				return 0;
			if (fromNodeIndex == toNodeIndex) {
				return 0;
			} else if (fromNodeIndex < toNodeIndex) {
				return currentMaze.shortestPathDistances[((toNodeIndex * (toNodeIndex + 1)) / 2) + fromNodeIndex];
			} else {
				return currentMaze.shortestPathDistances[((fromNodeIndex * (fromNodeIndex + 1)) / 2) + toNodeIndex];
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Returns the EUCLIDEAN distance between two nodes in the current
	 * mazes[gs.curMaze].
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex   the to node index
	 * @return the euclidean distance
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public double getEuclideanDistance(int fromNodeIndex, int toNodeIndex) {
		try {
			return Math.sqrt(Math.pow(currentMaze.graph[fromNodeIndex].x - currentMaze.graph[toNodeIndex].x, 2)
					+ Math.pow(currentMaze.graph[fromNodeIndex].y - currentMaze.graph[toNodeIndex].y, 2));
		} catch (Exception e) {
			System.err.println(e);
		}
		return 0;
	}

	/**
	 * Returns the MANHATTAN distance between two nodes in the current
	 * mazes[gs.curMaze].
	 *
	 * @param fromNodeIndex the from node index
	 * @param toNodeIndex   the to node index
	 * @return the manhattan distance
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getManhattanDistance(int fromNodeIndex, int toNodeIndex) {
		return (Math.abs(currentMaze.graph[fromNodeIndex].x - currentMaze.graph[toNodeIndex].x)
				+ Math.abs(currentMaze.graph[fromNodeIndex].y - currentMaze.graph[toNodeIndex].y));
	}

	/**
	 * Gets the distance.
	 *
	 * @param fromNodeIndex   the from node index
	 * @param toNodeIndex     the to node index
	 * @param distanceMeasure the distance measure
	 * @return the distance
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public double getDistance(int fromNodeIndex, int toNodeIndex, DM distanceMeasure) {
		/*
		 * if(fromNodeIndex >= this.getCurrentMaze().graph.length)
		 * 
		 * { System.err.println("Error. fromNodeIndex > graph.length"); return 0; }
		 */
		if (toNodeIndex >= this.getCurrentMaze().graph.length) {
			System.err.println("Error. toNodeIndex > graph.length");
			return 0;
		}

		switch (distanceMeasure) {
		case PATH:
			return getShortestPathDistance(fromNodeIndex, toNodeIndex);
		case EUCLID:
			return getEuclideanDistance(fromNodeIndex, toNodeIndex);
		case MANHATTAN:
			return getManhattanDistance(fromNodeIndex, toNodeIndex);
		}

		return -1;
	}

	/**
	 * Returns the distance between two nodes taking reversals into account.
	 *
	 * @param fromNodeIndex   the index of the originating node
	 * @param toNodeIndex     the index of the target node
	 * @param lastMoveMade    the last move made
	 * @param distanceMeasure the distance measure to be used
	 * @return the distance between two nodes.
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public double getDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure) {
		switch (distanceMeasure) {
		case PATH:
			return getApproximateShortestPathDistance(fromNodeIndex, toNodeIndex, lastMoveMade);
		case EUCLID:
			return getEuclideanDistance(fromNodeIndex, toNodeIndex);
		case MANHATTAN:
			return getManhattanDistance(fromNodeIndex, toNodeIndex);
		}

		return -1;
	}

	/**
	 * Gets the closest node index from node index.
	 *
	 * @param fromNodeIndex     the from node index
	 * @param targetNodeIndices the target node indices
	 * @param distanceMeasure   the distance measure
	 * @return the closest node index from node index
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getClosestNodeIndexFromNodeIndex(int fromNodeIndex, int[] targetNodeIndices, DM distanceMeasure) {
		double minDistance = Integer.MAX_VALUE;
		int target = -1;

		for (int i = 0; i < targetNodeIndices.length; i++) {
			double distance = 0;

			distance = getDistance(targetNodeIndices[i], fromNodeIndex, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				target = targetNodeIndices[i];
			}
		}

		return target;
	}

	/**
	 * Gets the farthest node index from node index.
	 *
	 * @param fromNodeIndex     the from node index
	 * @param targetNodeIndices the target node indices
	 * @param distanceMeasure   the distance measure
	 * @return the farthest node index from node index
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getFarthestNodeIndexFromNodeIndex(int fromNodeIndex, int[] targetNodeIndices, DM distanceMeasure) {
		double maxDistance = Integer.MIN_VALUE;
		int target = -1;

		for (int i = 0; i < targetNodeIndices.length; i++) {
			double distance = 0;

			distance = getDistance(targetNodeIndices[i], fromNodeIndex, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				target = targetNodeIndices[i];
			}
		}

		return target;
	}

	/**
	 * Gets the next move towards target.
	 *
	 * @param fromNodeIndex   the from node index
	 * @param toNodeIndex     the to node index
	 * @param distanceMeasure the distance measure
	 * @return the next move towards target
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, DM distanceMeasure) {
		MOVE move = null;

		double minDistance = Integer.MAX_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	/**
	 * Gets the next move away from target.
	 *
	 * @param fromNodeIndex   the from node index
	 * @param toNodeIndex     the to node index
	 * @param distanceMeasure the distance measure
	 * @return the next move away from target
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, DM distanceMeasure) {
		MOVE move = null;

		double maxDistance = Integer.MIN_VALUE;
		if ((fromNodeIndex == -1) || (toNodeIndex == -1))
			return MOVE.NEUTRAL;
		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].neighbourhood.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	/**
	 * Gets the approximate next move towards target not considering directions
	 * opposing the last move made.
	 *
	 * @param fromNodeIndex   The node index from which to move (i.e., current
	 *                        position)
	 * @param toNodeIndex     The target node index
	 * @param lastMoveMade    The last move made
	 * @param distanceMeasure The distance measure required (Manhattan, Euclidean or
	 *                        Straight line)
	 * @return The approximate next move towards target (chosen greedily)
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getApproximateNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade,
			DM distanceMeasure) {
		MOVE move = MOVE.NEUTRAL;
		if ((toNodeIndex == -1) || (fromNodeIndex == -1))
			return MOVE.NEUTRAL;

		try {
			double minDistance = Integer.MAX_VALUE;
			for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
					.entrySet()) {
				double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

				if (distance < minDistance) {
					minDistance = distance;
					move = entry.getKey();
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		return move;
	}

	/**
	 * Gets the approximate next move away from a target not considering directions
	 * opposing the last move made.
	 *
	 * @param fromNodeIndex   The node index from which to move (i.e., current
	 *                        position)
	 * @param toNodeIndex     The target node index
	 * @param lastMoveMade    The last move made
	 * @param distanceMeasure The distance measure required (Manhattan, Euclidean or
	 *                        Straight line)
	 * @return The approximate next move towards target (chosen greedily)
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getApproximateNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade,
			DM distanceMeasure) {
		MOVE move = MOVE.NEUTRAL;

		try {
			double maxDistance = Integer.MIN_VALUE;

			for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
					.entrySet()) {
				double distance = getDistance(entry.getValue(), toNodeIndex, distanceMeasure);

				if (distance > maxDistance) {
					maxDistance = distance;
					move = entry.getKey();
				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		return move;
	}

	/**
	 * Gets the exact next move towards target taking into account reversals. This
	 * uses the pre-computed paths.
	 *
	 * @param fromNodeIndex   The node index from which to move (i.e., current
	 *                        position)
	 * @param toNodeIndex     The target node index
	 * @param lastMoveMade    The last move made
	 * @param distanceMeasure the distance measure to be used
	 * @return the next move towards target
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getNextMoveTowardsTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure) {
		MOVE move = null;

		double minDistance = Integer.MAX_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
				.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, lastMoveMade, distanceMeasure);

			if (distance < minDistance) {
				minDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	/**
	 * Gets the exact next move away from target taking into account reversals. This
	 * uses the pre-computed paths.
	 *
	 * @param fromNodeIndex   The node index from which to move (i.e., current
	 *                        position)
	 * @param toNodeIndex     The target node index
	 * @param lastMoveMade    The last move made
	 * @param distanceMeasure the distance measure to be used
	 * @return the next move away from target
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public MOVE getNextMoveAwayFromTarget(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade, DM distanceMeasure) {
		MOVE move = null;

		double maxDistance = Integer.MIN_VALUE;

		for (Entry<MOVE, Integer> entry : currentMaze.graph[fromNodeIndex].allNeighbourhoods.get(lastMoveMade)
				.entrySet()) {
			double distance = getDistance(entry.getValue(), toNodeIndex, lastMoveMade, distanceMeasure);

			if (distance > maxDistance) {
				maxDistance = distance;
				move = entry.getKey();
			}
		}

		return move;
	}

	/**
	 * Gets the A* path considering previous moves made (i.e., opposing actions are
	 * ignored)
	 *
	 * @param fromNodeIndex The node index from which to move (i.e., current
	 *                      position)
	 * @param toNodeIndex   The target node index
	 * @param lastMoveMade  The last move made
	 * @return The A* path
	 * @deprecated use getShortestPath() instead.
	 */
	@Deprecated
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getAStarPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		return getShortestPath(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	/**
	 * Gets the shortest path from node A to node B as specified by their indices.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current
	 *                      position)
	 * @param toNodeIndex   The target node index
	 * @return the shortest path from start to target
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getShortestPath(int fromNodeIndex, int toNodeIndex) {
		return caches[mazeIndex].getPathFromA2B(fromNodeIndex, toNodeIndex);
	}

	/**
	 * Gets the shortest path taking into account the last move made (i.e., no
	 * reversals). This is approximate only as the path is computed greedily. A more
	 * accurate path can be obtained using A* which is slightly more costly.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current
	 *                      position)
	 * @param toNodeIndex   The target node index
	 * @param lastMoveMade  The last move made
	 * @return the shortest path from start to target
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getShortestPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		if (currentMaze.graph[fromNodeIndex].neighbourhood.size() == 0)// lair
		{
			return new int[0];
		}

		return caches[mazeIndex].getPathFromA2B(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	/**
	 * Gets the approximate shortest path taking into account the last move made
	 * (i.e., no reversals). This is approximate only as the path is computed
	 * greedily. A more accurate path can be obtained using A* which is slightly
	 * more costly.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current
	 *                      position)
	 * @param toNodeIndex   The target node index
	 * @param lastMoveMade  The last move made
	 * @return the shortest path from start to target
	 * @deprecated use getShortestPath() instead.
	 */
	@Deprecated
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int[] getApproximateShortestPath(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		return getShortestPath(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	/**
	 * Similar to getApproximateShortestPath but returns the distance of the path
	 * only. It is slightly more efficient.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current
	 *                      position)
	 * @param toNodeIndex   The target node index
	 * @param lastMoveMade  The last move made
	 * @return the exact distance of the path
	 * @deprecated use getShortestPathDistance() instead.
	 */
	@Deprecated
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getApproximateShortestPathDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		return getShortestPathDistance(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	/**
	 * Similar to getShortestPath but returns the distance of the path only. It is
	 * slightly more efficient.
	 *
	 * @param fromNodeIndex The node index from where to start (i.e., current
	 *                      position)
	 * @param toNodeIndex   The target node index
	 * @param lastMoveMade  The last move made
	 * @return the exact distance of the path
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public int getShortestPathDistance(int fromNodeIndex, int toNodeIndex, MOVE lastMoveMade) {
		if (currentMaze.graph[fromNodeIndex].neighbourhood.size() == 0)// lair
		{
			return 0;
		}

		return caches[mazeIndex].getPathDistanceFromA2B(fromNodeIndex, toNodeIndex, lastMoveMade);
	}

	/**
	 * Can be used to query if the game contains Messaging
	 * <p>
	 * Will be true if messaging is active and the copy of the game is owned by a
	 * Ghost
	 * <p>
	 * Pacman can't access the Messenger
	 *
	 * @return True if messaging is available, false otherwise
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean hasMessaging() {
		return messenger != null && agent < GHOST.values().length;
	}

	/**
	 * Gets the messenger or null if it either doesn't exist or you don't have
	 * access to it
	 *
	 * @return The messenger
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Messenger getMessenger() {
		return (hasMessaging() ? messenger : null);
	}

	/**
	 * Gets a data structure that can be modified and then used to construct a
	 * forward model
	 *
	 * @return The GameInfo object for use in making the Game
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public GameInfo getBlankGameInfo() {
		return new GameInfo(pills.length());
	}

	/**
	 * Gets a GameInfo object that is populated with present data that you can see
	 *
	 * @return The GameInfo object for use in making the Game
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public GameInfo getPopulatedGameInfo() {
		GameInfo info = getBlankGameInfo();

		if (getPacmanCurrentNodeIndex() != -1) {
			info.setPacman(new PacMan(getPacmanCurrentNodeIndex(), getPacmanLastMoveMade(),
					getPacmanNumberOfLivesRemaining(), false));
		}
		EnumMap<Constants.GHOST, Ghost> copyGhosts = info.getGhosts();
		for (Constants.GHOST ghost : GHOST.values()) {
			if (getGhostCurrentNodeIndex(ghost) != -1) {
				copyGhosts.put(ghost, new Ghost(ghost, getGhostCurrentNodeIndex(ghost), getGhostEdibleTime(ghost),
						getGhostLairTime(ghost), getGhostLastMoveMade(ghost)));
			}
		}

		for (int index : getActivePillsIndices()) {
			info.setPillAtIndex(getPillIndex(index), true);
		}

		for (int index : getActivePowerPillsIndices()) {
			info.setPowerPillAtIndex(getPowerPillIndex(index), true);
		}

		return info;
	}

	/**
	 * Gets a copy of the game that is populated with the data contained within info
	 *
	 * @param info The data you wish the game to be supplied with
	 * @return The resultant game
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public Game getGameFromInfo(GameInfo info) {
		Game game = copy(false);
		// Destroy the messenger reference - can't allow communication in the playouts
		// Can we? start a fresh one?
		game.messenger = null;
		game.pills = info.getPills();
		game.powerPills = info.getPowerPills();
		// Etc
		game.internalPacman = info.getPacman();

		game.ghosts = info.getGhosts();

		game.beenBlanked = true;
		game.po = false;

		return game;
	}

	/**
	 * Is this game Partially Observable?
	 *
	 * @return The boolean answer to the question
	 */
	@SuppressWarnings({ "WeakerAccess", "unused" })
	public boolean isGamePo() {
		return po;
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void setGhostsPresent(boolean ghostsPresent) {
		this.ghostsPresent = ghostsPresent;
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void setPillsPresent(boolean pillsPresent) {
		this.pillsPresent = pillsPresent;
	}

	@SuppressWarnings({ "WeakerAccess", "unused" })
	public void setPowerPillsPresent(boolean powerPillsPresent) {
		this.powerPillsPresent = powerPillsPresent;
	}

	String getPacManControllerName() {
		return this.PacManControllerName;
	}

	String getGhostsControllerName() {
		return this.GhostsControllerName;
	}
}