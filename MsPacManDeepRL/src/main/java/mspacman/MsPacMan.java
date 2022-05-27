package mspacman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import es.ucm.fdi.ici.Action;
import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

import pacman.game.Game;
import utils.Pair;

public class MsPacMan extends PacmanController implements Action {
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private static int maxValue = 300;
	private String name;
	private int type;
	private Game game;
	private MOVE lastMoveMade;
	private int lastTime;
	private int lastGhosts;
	private int lastScore;
	private GHOST lastNearestGhost;
	private int lastDistanceToGhost;
	private int lastDistance;
	private int lastPills;
	private int lastPPills;
	private int lastLevel;
	private int lastLives;

	/**
	 * @param type 0: General; 1: Not Edible & Edible Long state; 2: Edible Short State
	 */
	public MsPacMan(Socket socket, String name, int type) {
		this.name = name;
		this.type = type;
		try {
			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			toServer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Initializes the msPacMan
	 */
	public void init(Game game) {
		this.game = game;
		this.lastMoveMade = MOVE.UP;
		this.lastTime = 0;
		this.lastGhosts = 0;
		this.lastScore = 0;
		this.lastNearestGhost = null;
		this.lastDistanceToGhost = maxValue;
		this.lastDistance = maxValue;
		this.lastPills = game.getNumberOfPills();
		this.lastPPills = game.getNumberOfPowerPills();
		this.lastLevel = game.getCurrentLevel();
		this.lastLives = game.getPacmanNumberOfLivesRemaining();
	}
	/**
	 * Sends the state to python and gets the move that has to be made
	 */
	@Override
	public MOVE getMove(Game game, long timeDue) {
		int msPacManNode = game.getPacmanCurrentNodeIndex();
		if (game.isJunction(msPacManNode)) {
			this.game = game;
			sendState(msPacManNode);
			return recieveAction(msPacManNode);
		}
		return MOVE.NEUTRAL;
	}
	/**
	 * Sends the corresponding state to python.
	 */
	// Unified method for every type of state
	private void sendState(int msPacManNode) {
		Pair<List<GHOST>, List<Integer>> pairGhostDist = getNearestGhosts(msPacManNode);
		List<GHOST> lGhost = pairGhostDist.getFirst();
		List<Integer> distGhosts = pairGhostDist.getSecond();
		
		String output = null;

		// General state
		if (type == 0) {
			List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
			List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);
			List<Integer> edibleGhosts = new ArrayList<>();
			for (GHOST g : lGhost) {
				if (g != null)
					edibleGhosts.add((game.getGhostEdibleTime(g) > 0) ? maxValue/2 : 0);
				else
					edibleGhosts.add(maxValue);
			}
			output = distPills + "/" + distPowerPills + "/" + distGhosts + "/" + edibleGhosts + ";"; 
		}
		// Not Edible state and long Edible state
		else if (type == 1) {
			List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
			List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);

			output = distPills + "/" + distPowerPills + "/" + distGhosts + ";";
		}
		// Short Edible state
		else {
			output = distGhosts + ";";
		}
		toServer.print(output + calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
	}
	
	/**
	 * Calculates the corresponding reward that has to be sent to python.
	 */
	// Unified method for every type of reward
	private int calculateReward() {
		
		int reward = 0;

		int currentGhosts = game.getNumberOfGhostsEaten();
		int currentLives = game.getPacmanNumberOfLivesRemaining();

		// General reward
		if (type == 0) {
			int currentPills = game.getNumberOfActivePills();
			int aux = (lastPills - currentPills) * 1;
			int rewardForPills = (aux > 0) ? aux : 0;
			lastPills = currentPills;

			int currentPPills = game.getNumberOfActivePowerPills();
			int aux2 = (lastPPills - currentPPills) * 5;
			int rewardForPPills = (aux2 > 0) ? aux2 : 0;
			lastPPills = currentPPills;
			
			int rewardForGhosts = (currentGhosts - lastGhosts) * 250;
			lastGhosts = currentGhosts;
			
			int currentLevel = game.getCurrentLevel();
			int rewardForLevel = (currentLevel > lastLevel) ? 2000 : 0;
			lastLevel = currentLevel;
			
			int currentTime = game.getTotalTime();
			int rewardForTime = currentTime - lastTime;
			lastTime = currentTime;
			
			reward = rewardForPills + rewardForPPills + rewardForGhosts + rewardForLevel + rewardForTime;
		}
		// Not Edible reward
		else if (type == 1) {
			int currentScore = game.getScore();
			int rewardForScore = currentScore - lastScore;
			lastScore = currentScore;

			reward = rewardForScore;
		}
		// Edible reward
		else {
			int currentPills = game.getNumberOfActivePills();
			int aux = (lastPills - currentPills);
			int rewardForPills = (aux > 0) ? aux : 0;
			lastPills = currentPills;
			
			int rewardForGhosts = (currentGhosts - lastGhosts) * 250;
			lastGhosts = currentGhosts;
			
			reward = rewardForPills + rewardForGhosts; // For short state remove rewardForPills
		}

		int rewardForLives = (currentLives < lastLives) ? -500 : 0;
		lastLives = currentLives;

		return reward + rewardForLives;
	}
	
	/**
	 * Receives from the python server the two best actions and does the action that is possible to be made.

	 */
	private MOVE recieveAction(int msPacManNode) {
		try {
			String data = fromServer.readLine();
			String moves[] = data.split(";");
			MOVE[] movements = game.getPossibleMoves(msPacManNode);
			List<MOVE> l = Arrays.asList(movements);
			MOVE m1 = MOVE.values()[Integer.parseInt(moves[0])];
			MOVE m2 = MOVE.values()[Integer.parseInt(moves[1])];
			if (l.contains(m1)) {
				lastMoveMade = m1;
				return m1;
			} else {

				lastMoveMade = m2;
				return m2;
			}
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			return MOVE.NEUTRAL;
		}
	}
	
	/**
	 * Method that gets the nearest pill in every direction of the mspacman
	 */
	private List<Integer> getDistanceToNearestPills(int msPacManNode) {

		List<Integer> l = Arrays.asList(new Integer[] { maxValue, maxValue, maxValue, maxValue });

		for (MOVE m : game.getPossibleMoves(msPacManNode)) {
			l.set(m.ordinal(), getDistanceToNearestPill(m, msPacManNode));
		}

		return l;

	}
	/**
	 * Method that gets the nearest pill in the direction given from mspacman
	 */
	private int getDistanceToNearestPill(MOVE move, int msPacManNode) {
		int[] pillsArray = game.getActivePillsIndices();
		int distance, minDistance = maxValue, neigh = game.getNeighbour(msPacManNode, move);
		for (int pill : pillsArray) {
			distance = (int) game.getDistance(neigh, pill, move, DM.PATH);
			if (distance < minDistance)
				minDistance = distance;
		}
		return minDistance;
	}
	/**
	 * Method that gets the nearest powerpill in every direction of the mspacman
	 */
	private List<Integer> getDistanceToNearestPowerPills(int msPacManNode) {

		List<Integer> l = Arrays.asList(new Integer[] { maxValue, maxValue, maxValue, maxValue });

		for (MOVE m : game.getPossibleMoves(msPacManNode)) {
			l.set(m.ordinal(), getDistanceToNearestPowerPill(m, msPacManNode));
		}

		return l;

	}
	/**
	 * Method that gets the nearest power pill in the direction given from mspacman
	 */
	private int getDistanceToNearestPowerPill(MOVE move, int msPacManNode) {
		int[] powerPillsArray = game.getActivePowerPillsIndices();
		int distance, minDistance = maxValue, neigh = game.getNeighbour(msPacManNode, move);
		
		for (int ppill : powerPillsArray) {
			distance = (int) game.getDistance(neigh, ppill, move, DM.PATH);
			if (distance < minDistance)
				minDistance = distance;
		}
		return minDistance;
	}
	/**
	 * Method that gets the nearest ghost in every direction of the mspacman
	 */
	private Pair<List<GHOST>, List<Integer>> getNearestGhosts(int msPacManNode) {

		List<GHOST> lGhost = Arrays.asList(new GHOST[] { null, null, null, null });
		List<Integer> lDistance = Arrays.asList(new Integer[] { maxValue, maxValue, maxValue, maxValue });

		for (MOVE m : game.getPossibleMoves(msPacManNode)) {
			Pair<GHOST, Integer> pair= getNearestGhost(m, msPacManNode);
			lGhost.set(m.ordinal(), pair.getFirst());
			lDistance.set(m.ordinal(), pair.getSecond());
		}

		return new Pair<List<GHOST>, List<Integer>>(lGhost, lDistance);

	}
	/**
	 * Method that gets the nearest ghost in the direction given from mspacman
	 */
	private Pair<GHOST, Integer> getNearestGhost(MOVE move, int msPacManNode) {
		int distance, minDistance = maxValue, pos, neigh = game.getNeighbour(msPacManNode, move);
		GHOST nearestGhost = null;
		for (GHOST ghost : GHOST.values()) {
			pos = game.getGhostCurrentNodeIndex(ghost);
			if (pos != game.getCurrentMaze().lairNodeIndex) {
				if(neigh != -1)
					distance = (int) game.getDistance(neigh, pos, move, DM.PATH);
				else
					distance = maxValue;
			}				
			else
				distance = maxValue;
			if (distance < minDistance) {
				minDistance = distance;
				nearestGhost = ghost;
			}
		}
		return new Pair<GHOST, Integer>(nearestGhost, minDistance);
	}
	
	/**
	 * Method that gets the nearest ghost from mspacman
	 */
	private GHOST getNearestGhost(int msPacManNode) {
		int distance, minDistance = Integer.MAX_VALUE, pos;
		GHOST nearestGhost = null;
		for (GHOST ghost : GHOST.values()) {
			pos = game.getGhostCurrentNodeIndex(ghost);
			if (pos != game.getCurrentMaze().lairNodeIndex)
				distance = (int) game.getDistance(msPacManNode, pos, DM.PATH);
			else
				distance = Integer.MAX_VALUE;
			if (distance < minDistance) {
				minDistance = distance;
				nearestGhost = ghost;
			}
		}
		return nearestGhost;
	}

	public int getEpisodes() {
		try {
			return Integer.parseInt(fromServer.readLine());
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			return 0;
		}
	}
	/**
	 * Sends to python that the game is over, the last reward and the last move made.
	 * @param game
	 */
	public void gameOver(Game game) {
		this.game = game;
		toServer.print("gameOver;" + calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
		this.lastPills = 0;
	}
	
	/**
	 * Waits for the ok from the python server
	 */
	public void getOk() {
		try {
			fromServer.readLine(); // Waits OK
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getActionId() {
		return this.name;
	}
	
	/**
	 * Method that executes the game in fsm
	 */
	@Override
	public MOVE execute(Game game) {
		int msPacManNode = game.getPacmanCurrentNodeIndex();
		if (game.isJunction(msPacManNode)) {
			this.game = game;
			sendState(msPacManNode);
			return recieveAction(msPacManNode);
		}
		return MOVE.NEUTRAL;
	}
}
