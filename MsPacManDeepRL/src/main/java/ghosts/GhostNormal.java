package ghosts;


import java.util.EnumMap;

import pacman.controllers.GhostController;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Ghosts whose behavior is  going to eat mspacman if they are not edible and fleeing from it fi they are edible
 *
 */
public class GhostNormal extends GhostController {
	private EnumMap<GHOST, MOVE> moves = new EnumMap<GHOST, MOVE>(GHOST.class);
	
	@Override
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
		moves.clear();
		for (GHOST ghostType : GHOST.values()) {
			if (game.doesGhostRequireAction(ghostType)) {
				if (!game.isGhostEdible(ghostType))
					moves.put(ghostType, game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghostType), 
																			game.getPacmanCurrentNodeIndex(), 
																			game.getGhostLastMoveMade(ghostType),
																			DM.PATH));
				else
					moves.put(ghostType, game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghostType), 
																			game.getPacmanCurrentNodeIndex(), 
																			game.getGhostLastMoveMade(ghostType),
																			DM.PATH));
			}
		}
		return moves;
	}
}
