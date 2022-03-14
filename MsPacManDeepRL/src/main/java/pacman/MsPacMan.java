package pacman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import engine.pacman.controllers.PacmanController;
import engine.pacman.game.Constants;
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;

import engine.pacman.game.Game;

public class MsPacMan extends PacmanController {
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private Game game;
	private int lastScore;
	private static int maxDistance = 250;

	public MsPacMan(Socket socket) {
		this.lastScore = 0;
		try {

			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			toServer = new PrintWriter(socket.getOutputStream(), true);

		} catch (IOException e) {
			e.printStackTrace();
		}
		;

	}

	@Override
	public MOVE getMove(Game game, long timeDue) {
		int msPacManNode = game.getPacmanCurrentNodeIndex();
		if (game.isJunction(msPacManNode)) {
			this.game = game;
			sendState(msPacManNode);
			return recieveAction();
		}
		return MOVE.NEUTRAL;
	}

	private void sendState(int msPacManNode) {
		List<Integer> distPills = getDistanceToNearestPills(msPacManNode);
		List<Integer> distPowerPills = getDistanceToNearestPowerPills(msPacManNode);
		int numPills = game.getNumberOfActivePills();
		int numPowerPills = game.getNumberOfActivePowerPills();
		int currentTime = game.getCurrentLevelTime();
		List<GHOST> lGhost = getNearestGhosts(msPacManNode);
		MOVE msPacManLastMove = game.getPacmanLastMoveMade();
		List<Integer> distGhosts = new ArrayList<>();
		List<Integer> dirGhosts = new ArrayList<>();
		List<Integer> edibleTimeGhosts = new ArrayList<>();
		int lairTime = Constants.COMMON_LAIR_TIME + 10;
		for (GHOST g : lGhost) {
			if (g == null) {
				distGhosts.add(maxDistance);
				dirGhosts.add(MOVE.NEUTRAL.ordinal());
				edibleTimeGhosts.add(lairTime);
			} else {

				distGhosts.add((int) game.getDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), msPacManLastMove,
						DM.PATH));
				dirGhosts.add(game.getGhostLastMoveMade(g).ordinal());
				edibleTimeGhosts.add(game.getGhostEdibleTime(g));
			}
		}

		int aux;
		for (GHOST gh : GHOST.values()) {
			aux = game.getGhostLairTime(gh);
			if (aux < lairTime && aux != 0) {
				lairTime = aux;
			}
		}

		int currentScore = game.getScore();
		int reward = currentScore - lastScore;
		lastScore = currentScore;
		toServer.print(distPills + "/" + distPowerPills + "/" + numPills + "/" + numPowerPills + "/" + currentTime + "/"
				+ distGhosts + "/" + dirGhosts + "/" + edibleTimeGhosts + "/" + lairTime + ";" + reward);
		toServer.flush();
	}

	private MOVE recieveAction() {
		try {
			int m = Integer.parseInt(fromServer.readLine());
			return MOVE.values()[m];
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
			return MOVE.NEUTRAL;
		}
	}

	private List<Integer> getDistanceToNearestPills(int msPacManNode) {

		List<Integer> l = Arrays.asList(new Integer[] { maxDistance, maxDistance, maxDistance, maxDistance });

		for (MOVE m : game.getPossibleMoves(msPacManNode, game.getPacmanLastMoveMade())) {
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

		for (MOVE m : game.getPossibleMoves(msPacManNode, game.getPacmanLastMoveMade())) {
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

		for (MOVE m : game.getPossibleMoves(msPacManNode, game.getPacmanLastMoveMade())) {
			l.set(m.ordinal(), getNearestGhost(m, msPacManNode));
		}

		return l;

	}

	private GHOST getNearestGhost(MOVE move, int msPacManNode) {
		int distance, minDistance = Integer.MAX_VALUE, pos;
		GHOST nearestGhost = null;
		for (GHOST ghost : GHOST.values()) {
			pos = game.getGhostCurrentNodeIndex(ghost);
			// Meter un -1 en la distancia estropearia todo el proceso ya que es mayor que
			// cualquier distancia
			if (pos != game.getCurrentMaze().lairNodeIndex)
				distance = (int) game.getDistance(msPacManNode, pos, move, DM.PATH);
			else
				distance = Integer.MAX_VALUE;
			if (distance < minDistance) {
				minDistance = distance;
				nearestGhost = ghost;
			}
		}
		/*
		 * if (minDistance == Integer.MAX_VALUE) minDistance = -1;
		 */
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
		toServer.print("gameOver;" + (game.getScore() - lastScore));
		toServer.flush();
		this.lastScore = 0;
	}
}
