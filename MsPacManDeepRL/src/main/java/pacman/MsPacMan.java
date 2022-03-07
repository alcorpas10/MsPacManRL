package pacman;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import engine.pacman.controllers.PacmanController;
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.MOVE;
import engine.pacman.game.Game;

public class MsPacMan extends PacmanController {
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private Game game;
	private int lastScore;
	
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
		int msPacManNode=game.getPacmanCurrentNodeIndex();
		if(game.isJunction(msPacManNode)) {
			this.game = game;
			sendState(msPacManNode);
			return recieveAction();
		}
		return MOVE.NEUTRAL;  
	}
	
	private void sendState(int msPacManNode) {
		List<Integer> distPills=getDistanceToNearestPills(msPacManNode);
		int currentScore = game.getScore();
		int reward = currentScore - lastScore;
		lastScore = currentScore;
		toServer.print(distPills + ";" + reward);
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

		List<Integer> l = Arrays.asList(new Integer[] { 250, 250, 250, 250 });

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
	
	
	public int getEpisodes() {
		try {
			return Integer.parseInt(fromServer.readLine());
		} catch (NumberFormatException|IOException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public void gameOver() {
		toServer.print("gameOver;" +(game.getScore() - lastScore));
		toServer.flush();
		this.lastScore = 0;
	}
}
