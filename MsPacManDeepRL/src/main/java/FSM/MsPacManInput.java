package FSM;

import engine.es.ucm.fdi.ici.Input;
import engine.pacman.game.Game;
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;

public class MsPacManInput extends Input {

	private boolean chase;
	private boolean flee;
	
	
	public MsPacManInput(Game game) {
		super(game);
		chase = false;
		flee = false;
		
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
