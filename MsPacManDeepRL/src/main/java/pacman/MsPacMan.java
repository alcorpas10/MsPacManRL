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

public class MsPacMan extends PacmanController implements Action {
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private Game game;
	private static int maxValue = 500;
	private MOVE lastMoveMade;
	private int lastPills;
	private int lastLives;
	private int lastLevel;
	private int lastTime;
	private int lastGhosts;
	private int lastPPills;
	private int lastScore;
	private String name;

	public MsPacMan(Socket socket, String name) {
		this.name = name;
		this.lastMoveMade = MOVE.UP;
		this.lastTime = 0;
		this.lastGhosts = 0;
		this.lastScore=0;

		try {
			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			toServer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public void init(Game game) {
		this.lastPills=game.getNumberOfPills();
		this.lastPPills= game.getNumberOfPowerPills();
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

 	//SendState General
  	/*private void sendState(int msPacManNode) {
		List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
		List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);
		List<GHOST> lGhost = getNearestGhosts(msPacManNode);
		List<Integer> distGhosts = new ArrayList<>();
		List<Integer> edibleTimeGhosts = new ArrayList<>();
		for (GHOST g : lGhost) {
			if (g == null) {
				distGhosts.add(maxValue);
				edibleTimeGhosts.add(maxValue);
			} else {
				distGhosts.add((int) game.getDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), DM.PATH));
				edibleTimeGhosts.add(game.getGhostEdibleTime(g));
			}

		}
		toServer.print(distPills + "/" + distPowerPills + "/" + distGhosts + "/" + edibleTimeGhosts + ";"
				+ calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
	}*/
	
	//SendState Not Edible
	/*private void sendState(int msPacManNode) {
		List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
		List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);
		List<GHOST> lGhost = getNearestGhosts(msPacManNode);
		List<Integer> distGhosts = new ArrayList<>();
		
		for (GHOST g : lGhost) {
			if (g == null) {
				distGhosts.add(maxValue);
			} else {
				distGhosts.add((int) game.getDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), DM.PATH));
			}

		}
		toServer.print(distPills + "/" + distPowerPills + "/" + distGhosts  + ";"
				+ calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
	}*/
	
	//SendState Edible
	private void sendState(int msPacManNode) {
		List<GHOST> lGhost = getNearestGhosts(msPacManNode);
		List<Integer> distGhosts = new ArrayList<>();
		
		for (GHOST g : lGhost) {
			if (g == null) {
				distGhosts.add(maxValue);
			} else {
				distGhosts.add((int) game.getDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), DM.PATH));
			}
		}
		toServer.print(distGhosts + ";" + calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
	}
	
	//Reward NotEdible Ghosts
	/*private int calculateReward() {
		int currentScore = game.getScore(); 
		int rewardForScore = currentScore - lastScore; 
		lastScore = currentScore;

		int currentLives = game.getPacmanNumberOfLivesRemaining();
		int rewardForLives = (currentLives < lastLives) ? -100 : 0;
		lastLives = currentLives;

		int currentTime = game.getTotalTime();
		int rewardForTime = (lastTime - currentTime);
		lastTime = currentTime;

		int currentLevel = game.getCurrentLevel();
		int rewardForLevel = (currentLevel > lastLevel) ? 1000 : 0;
		lastLevel = currentLevel;

		return rewardForScore + rewardForLives + rewardForTime + rewardForLevel;
	}*/
	
	//Reward Edible Ghosts 
	private int calculateReward() {
		int currentGhosts = game.getNumberOfGhostsEaten();
		int rewardForGhosts = (currentGhosts - lastGhosts) * Constants.GHOST_EAT_SCORE;
		lastGhosts = currentGhosts;

		int currentTime = game.getTotalTime();
		int rewardForTime = (lastTime - currentTime) * 3;
		lastTime = currentTime;

		return rewardForGhosts + rewardForTime;
	}
	
	//CalculateReward General 
	/*private int calculateReward() {
		int currentPills = game.getNumberOfActivePills();
		int aux = (lastPills - currentPills) * 10;
		int rewardForPills= (aux>0) ? aux : 0;
		lastPills = currentPills;
		
		int currentPPills = game.getNumberOfActivePowerPills();
		int aux2 = (lastPPills - currentPPills ) * 20;
		int rewardForPPills= (aux2>0) ? aux2 : 0;
		lastPPills = currentPPills;

		int currentGhosts = game.getNumberOfGhostsEaten();
		int rewardForGhosts = (currentGhosts - lastGhosts) * Constants.GHOST_EAT_SCORE;
		lastGhosts = currentGhosts;

		int currentLives = game.getPacmanNumberOfLivesRemaining();
		int rewardForLives = (currentLives < lastLives) ? -500 : 0;
		lastLives = currentLives;

		int currentTime = game.getTotalTime();
		int rewardForTime = (lastTime - currentTime);
		lastTime = currentTime;

		int currentLevel = game.getCurrentLevel();
		int rewardForLevel = (currentLevel > lastLevel) ? 1000 : 0;
		lastLevel = currentLevel;

		return rewardForPills + rewardForGhosts + rewardForLives + rewardForTime + rewardForLevel+ rewardForPPills;
	}*/
	
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
		int distance, minDistance = Integer.MAX_VALUE;
		for (int pill : pillsArray) {
			distance = (int) game.getDistance(msPacManNode, pill, move, DM.PATH);
			if (distance < minDistance)
				minDistance = distance;
		}
		if (minDistance == Integer.MAX_VALUE)
			minDistance = -1;
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
		int distance, minDistance = Integer.MAX_VALUE;
		for (int ppill : powerPillsArray) {
			distance = (int) game.getDistance(msPacManNode, ppill, move, DM.PATH);
			if (distance < minDistance)
				minDistance = distance;
		}
		if (minDistance == Integer.MAX_VALUE)
			minDistance = maxValue;
		return minDistance;
	}

	private List<GHOST> getNearestGhosts(int msPacManNode) {

		List<GHOST> l = Arrays.asList(new GHOST[] { null, null, null, null });

		for (MOVE m : game.getPossibleMoves(msPacManNode)) {
			l.set(m.ordinal(), getNearestGhost(m, msPacManNode));
		}

		return l;

	}

	private GHOST getNearestGhost(MOVE move, int msPacManNode) {
		int distance, minDistance = Integer.MAX_VALUE, pos;
		GHOST nearestGhost = null;
		for (GHOST ghost : GHOST.values()) {
			pos = game.getGhostCurrentNodeIndex(ghost);
			if (pos != game.getCurrentMaze().lairNodeIndex)
				distance = (int) game.getDistance(msPacManNode, pos, move, DM.PATH);
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

	public void gameOver() {
		toServer.print("gameOver;" + calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
		this.lastPills = 0;
	}

	public void getOk() {
		try {
			String data = fromServer.readLine(); // Waits OK
		} catch (IOException e) {
			// TODO Auto-generated catch block
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
