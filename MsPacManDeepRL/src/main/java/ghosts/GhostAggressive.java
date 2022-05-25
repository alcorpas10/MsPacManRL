package ghosts;


import java.util.EnumMap;

import pacman.controllers.GhostController;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 *Ghosts whose behavior is always going to eat mspacman even if they are edible
 */
public class GhostAggressive extends GhostController {
	private EnumMap<GHOST, MOVE> moves = new EnumMap<GHOST, MOVE>(GHOST.class);
	
	@Override
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
		moves.clear();
		for (GHOST ghostType : GHOST.values()) {
			if (game.doesGhostRequireAction(ghostType)) {
				moves.put(ghostType, game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), 
																			game.getPacmanCurrentNodeIndex(), 
																			game.getGhostLastMoveMade(ghostType),
																			DM.EUCLID));
			}
		}
		return moves;
	}

}
