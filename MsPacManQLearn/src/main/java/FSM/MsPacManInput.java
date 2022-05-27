package FSM;

import es.ucm.fdi.ici.Input;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Class that gives the input to the fsm with the information needed from the game
 */
public class MsPacManInput extends Input {

	private boolean pills;
	private boolean chase;
	private boolean flee;
	
	
	public MsPacManInput(Game game) {
		super(game);
		pills=false;
		chase=false;
		flee=false;

		//Looks to the nearest ghost and updates the variables
		GHOST g = getNearestGhost(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
		if(g==null) {  //if the ghost is in the lair
			pills=true;
		}
		else if(game.isGhostEdible(g)) {  //if ghost is edible
			chase=true;
		}
		else if(game.getDistance(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(g), game.getPacmanLastMoveMade(), DM.PATH)>65) {  //if the distance is more than 65
			pills=true;
		}
		else {
			flee=true;
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
  		for(GHOST g: GHOST.values()) { //miramos entre todos los ghosts
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
	public void parseInput() {
		// does nothing.

		
		
	}
	
	
	public boolean pills() {
		return pills;
	}
	public boolean chase() {
		return chase;
	}
	public boolean flee() {
		return flee;
	}
	
}
