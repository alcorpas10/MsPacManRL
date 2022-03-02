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
	private boolean wasJunction;
	private int lastScore;
	
	public MsPacMan(Socket socket) {

		try {

			fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			toServer = new PrintWriter(socket.getOutputStream(), true);
			/*
			 * while(true){ System.out.println("Trying to read...");
			 *  String in =fromServer.readLine();
			 *   System.out.println(in);
			 *   toServer.print("Try"+"\r\n");
			 * toServer.flush(); 
			 * System.out.println("Message sent"); }
			 */

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
			int m=4;
			List<Integer> distPills=getDistanceToNearestPills(msPacManNode);
			
			wasJunction=true;
			lastScore=game.getScore();
			try {
				toServer.print(distPills);
				toServer.flush();
				m =Integer.parseInt(fromServer.readLine());
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			return MOVE.values()[m];  
		}
		return MOVE.NEUTRAL;  
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
	
	public void nextStep(boolean done) {
		if(wasJunction) {
			int msPacManNode=game.getPacmanCurrentNodeIndex();
			List<Integer> distPills=getDistanceToNearestPills(msPacManNode);
			int reward=game.getScore()-lastScore;
			
			toServer.print(distPills+";"+reward+";"+done);
			toServer.flush();
			wasJunction=false;
		}
	}
}
