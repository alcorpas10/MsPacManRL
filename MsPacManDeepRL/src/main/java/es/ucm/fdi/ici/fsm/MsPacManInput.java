package es.ucm.fdi.ici.fsm;

import es.ucm.fdi.ici.Input;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Class that gives the input to the fsm with the information needed from the game
 *
 */
public class MsPacManInput extends Input {

	private boolean chase;
	private boolean flee;
	
	
	public MsPacManInput(Game game) {
		super(game);
		chase = false;
		flee = false;
		
		//Looks if the nearest ghost is edible or not
		GHOST g = getNearestGhost(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
		if(g==null) {
			flee = true;
		}
		else {
			if(game.isGhostEdible(g))
				chase = true;
			else
				flee = true;
		}
	}
	/**
	 * gets the nearest ghost from mspacman 
	 * @param msPacManNode node that mspacman is in the maze
	 * @param msPacManMove  last move made by mspacman
	 */
	private GHOST getNearestGhost(int msPacManNode, MOVE msPacManMove) {
  		int d = Integer.MAX_VALUE;
  		GHOST ghost = null;
  		for(GHOST g: GHOST.values()) {
  			int ghostNode = game.getGhostCurrentNodeIndex(g);
  			if (ghostNode != game.getCurrentMaze().lairNodeIndex) {
	  			int di = game.getShortestPathDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), msPacManMove);
	  			if(di != -1 && di < d) {
	  				d = di;
	  				ghost = g;
	  			}
  			}
  		}
  		if (d == Integer.MAX_VALUE)
  			return null;
  		return ghost;
  	}
   
	
	@Override
	public void parseInput() {}
	
	public boolean chase() {
		return chase;
	}
	public boolean flee() {
		return flee;
	}
}
