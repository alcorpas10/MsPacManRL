package pacman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import engine.es.ucm.fdi.ici.Action;
import engine.pacman.controllers.PacmanController;
import engine.pacman.game.Constants;
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;

import engine.pacman.game.Game;
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
	 * @param type 0: General; 1: Not Edible; 2: Edible
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
			//List<Integer> edibleTimeGhosts = new ArrayList<>();
			int i = 0;
			for (GHOST g : lGhost) {
				if (g != null) {
					if (game.getGhostEdibleTime(g) == 0)
						distGhosts.set(i, (int) 0.5*distGhosts.get(i)+150);
				}
				i++;
			}
			output = distPills + "/" + distPowerPills + "/" + distGhosts + ";"; // + "/" + edibleTimeGhosts
		}
		// Not Edible state
		else if (type == 1 || type == 2) {
			List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
			List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);

			output = distPills + "/" + distPowerPills + "/" + distGhosts + ";";
		}
		// Edible state
		else {
			output = distGhosts + ";";
		}
		toServer.print(output + calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
	}

	// Unified method for every type of reward
	private int calculateReward() {
		
		int reward = 0;

		int currentGhosts = game.getNumberOfGhostsEaten();
		int currentLives = game.getPacmanNumberOfLivesRemaining();
		//int currentLevel = game.getCurrentLevel();

		// General reward
		if (type == 0) {
			int currentPills = game.getNumberOfActivePills();
			int aux = (lastPills - currentPills) * 10;
			int rewardForPills = (aux > 0) ? aux : 0;
			lastPills = currentPills;

			int currentPPills = game.getNumberOfActivePowerPills();
			int aux2 = (lastPPills - currentPPills) * 50;
			int rewardForPPills = (aux2 > 0) ? aux2 : 0;
			lastPPills = currentPPills;
			
			int rewardForGhosts = (currentGhosts - lastGhosts) * Constants.GHOST_EAT_SCORE;
			lastGhosts = currentGhosts;
			
			reward = rewardForPills + rewardForPPills + rewardForGhosts;
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
			/*Pair<List<GHOST>, List<Integer>> pairGhostDist = getNearestGhosts(this.game.getPacmanCurrentNodeIndex());
			List<Integer> distGhosts = pairGhostDist.getSecond();
			
			int distanceToGhost = maxValue;
			for(int dist: distGhosts) {
				if(dist < distanceToGhost)
					distanceToGhost = dist;
			}
			
			reward = (this.lastDistanceToGhost != maxValue && distanceToGhost != maxValue && currentGhosts == lastGhosts) ?
					(this.lastDistanceToGhost - distanceToGhost) : 0;
			this.lastDistanceToGhost = distanceToGhost;*/
			
			/*int node = game.getPacmanCurrentNodeIndex();
			int currentDistance = (lastNearestGhost != null) ? 
					(int) game.getDistance(node, game.getGhostCurrentNodeIndex(lastNearestGhost), DM.PATH)
					: maxValue;
			int rewardForCloser = (lastDistance != maxValue && currentDistance != maxValue) ?
					(lastDistance - currentDistance) : 0;
			
			GHOST g = getNearestGhost(node);
			lastDistance = (g != null) ? 
					(int) game.getDistance(node, game.getGhostCurrentNodeIndex(g), DM.PATH)	: maxValue;
			lastNearestGhost = g;

			reward = rewardForPills + rewardForCloser;*/
			
			int currentPills = game.getNumberOfActivePills();
			int aux = (lastPills - currentPills);
			int rewardForPills = (aux > 0) ? aux : 0;
			lastPills = currentPills;
			
			int rewardForGhosts = (currentGhosts - lastGhosts) * 250;
			lastGhosts = currentGhosts;
			
			reward = rewardForPills + rewardForGhosts;
			//return rewardForGhosts;
		}
		
		/*
		int currentTime = game.getTotalTime();
		
		int rewardForTime = (lastTime - currentTime);
		lastTime = currentTime;*/

		/*int rewardForGhosts = (currentGhosts - lastGhosts) * Constants.GHOST_EAT_SCORE;
		lastGhosts = currentGhosts;*/

		int rewardForLives = (currentLives < lastLives) ? -500 : 0;
		lastLives = currentLives;

		/*int rewardForLevel = (currentLevel > lastLevel) ? 1000 : 0;
		lastLevel = currentLevel;*/

		return reward + rewardForLives; //+ rewardForGhosts + rewardForLevel + rewardForTime;
	}

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

	private List<Integer> getDistanceToNearestPills(int msPacManNode) {

		List<Integer> l = Arrays.asList(new Integer[] { maxValue, maxValue, maxValue, maxValue });

		for (MOVE m : game.getPossibleMoves(msPacManNode)) {
			l.set(m.ordinal(), getDistanceToNearestPill(m, msPacManNode));
		}

		return l;

	}

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

	private List<Integer> getDistanceToNearestPowerPills(int msPacManNode) {

		List<Integer> l = Arrays.asList(new Integer[] { maxValue, maxValue, maxValue, maxValue });

		for (MOVE m : game.getPossibleMoves(msPacManNode)) {
			l.set(m.ordinal(), getDistanceToNearestPowerPill(m, msPacManNode));
		}

		return l;

	}

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

	public void gameOver(Game game) {
		this.game = game;
		toServer.print("gameOver;" + calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
		this.lastPills = 0;
	}

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
