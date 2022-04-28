package ghosts;


import java.util.EnumMap;

import engine.pacman.controllers.GhostController;
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;
import engine.pacman.game.Game;

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
