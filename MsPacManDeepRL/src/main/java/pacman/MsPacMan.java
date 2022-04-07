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
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;

import engine.pacman.game.Game;

public class MsPacMan extends PacmanController implements Action {
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private Game game;
	private static int maxDistance = 500;
	private MOVE lastMoveMade;
	private int lastScore;
	private int lastLives;
	private int lastLevel;
	private int lastTime;
	private String name;

	public MsPacMan(Socket socket, String name) {
		this.name = name;
		this.lastMoveMade = MOVE.UP;
		this.lastScore = 0;
		this.lastLives = 3;
		this.lastLevel = 3;
		this.lastTime = 0;
		try {
			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			toServer = new PrintWriter(socket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	private void sendState(int msPacManNode) {
		List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
		List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);
		List<GHOST> lGhost = getNearestGhosts(msPacManNode);
		List<Integer> distGhosts = new ArrayList<>();
		for (GHOST g : lGhost) {
			if (g == null)
				distGhosts.add(maxDistance);
			else
				distGhosts.add((int) game.getDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), DM.PATH));
		}
		toServer.print(distPills + "/" + distPowerPills + "/" + distGhosts + ";"
				+ calculateReward() + ";" + lastMoveMade.ordinal());
		toServer.flush();
	}

	private int calculateReward() {
		int currentScore = game.getScore();
		int rewardForPills = currentScore - lastScore;
		lastScore = currentScore;
		
		int currentLives = game.getPacmanNumberOfLivesRemaining();
		int rewardForLives = (currentLives < lastLives) ? -100 : 0;
		lastLives = currentLives;
		
		int currentTime = game.getTotalTime();
		int rewardForTime = lastTime - currentTime;
		lastTime = currentTime;
		
		int currentLevel = game.getCurrentLevel();
		int rewardForLevel = (currentLevel > lastLevel) ? 100 : 0;
		lastLevel = currentLevel;
		
		return rewardForPills + rewardForLives + rewardForTime + rewardForLevel;
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

		List<Integer> l = Arrays.asList(new Integer[] { maxDistance, maxDistance, maxDistance, maxDistance });

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

		List<Integer> l = Arrays.asList(new Integer[] { maxDistance, maxDistance, maxDistance, maxDistance });

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
			minDistance = maxDistance;
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
		this.lastScore = 0;
	}
	public void getOk() {
		try {
			String data = fromServer.readLine();  //Waits OK
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
