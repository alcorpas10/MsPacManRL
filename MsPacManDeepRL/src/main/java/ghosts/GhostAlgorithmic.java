package ghosts;

import java.util.EnumMap;

import pacman.controllers.GhostController;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;


/**
 * Ghosts whose behavior is algorithmic.
 *
 */
public class GhostAlgorithmic extends GhostController {
	// Distance the ghosts spread out while chasing Pacman
	public final int CHASE_PROXIM_LIMIT = 30; 
	// Distance the ghosts are separated while fleeing from Pacman
	public final int FLEE_PROXIM_LIMIT = 30;
	// Distance between Pacman and a Power Pill that ghosts flee to
	public final int PPILL_FLEE_DIST = 25;
	private EnumMap<GHOST, MOVE> moves = new EnumMap<GHOST, MOVE>(GHOST.class);
	private GHOST other = null;

	@Override
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
		int msPacManNode = game.getPacmanCurrentNodeIndex();
		moves.clear();
		
		for (GHOST ghost : GHOST.values()) {
			if (game.doesGhostRequireAction(ghost)) {
				if(game.isGhostEdible(ghost) || pacManCloseToPPill(game, PPILL_FLEE_DIST, msPacManNode)) {
					// FLEE FROM MSPACMAN
					moves.put(ghost, getGhostFleeMove(game, ghost, msPacManNode));
					
				} else {
					// CHASE MSPACMAN
					moves.put(ghost, getGhostChaseMove(game, ghost, msPacManNode));
				}
			}
		}
		return moves;
	}

	// Returns the movement to be made by ghost to flee from MsPacman
	private MOVE getGhostFleeMove(Game game, GHOST ghost, int msPacManNode) {
		MOVE move;
		other = isNearOtherGhost(game, FLEE_PROXIM_LIMIT, ghost);

		// If there is a ghost nearby, avoid running the same way
		// If not, flee from mspacman
		if (other != null)
			move = game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
					game.getGhostCurrentNodeIndex(other), game.getGhostLastMoveMade(ghost), DM.EUCLID);
		else
			move = game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
					msPacManNode, game.getGhostLastMoveMade(ghost), DM.EUCLID); 
		
		return move;
	}
	
	// Returns the movement to be made by ghost to flee from MsPacman
	private MOVE getGhostChaseMove(Game game, GHOST ghost, int msPacManNode) {
		MOVE move = null;
		other = isNearOtherGhost(game, CHASE_PROXIM_LIMIT, ghost);
		int ppillCloserToGhost = ghostCloserToPPill(game, PPILL_FLEE_DIST, msPacManNode, ghost );
		
		if (ppillCloserToGhost >= 0) {
			move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
					ppillCloserToGhost, game.getGhostLastMoveMade(ghost), DM.EUCLID);
		} else if(other != null) {
			// If there is another ghost nearby, chase mspacman with prediction (getAmbushPosition)
			MOVE dir = game.getPacmanLastMoveMade();
			int pos = msPacManNode;

			pos = getAmbushPosition(game, dir, pos);
			move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
					pos, game.getGhostLastMoveMade(ghost), DM.EUCLID);
		} else {
			// Chase MsPacman by going after her
			move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
					msPacManNode, game.getGhostLastMoveMade(ghost), DM.EUCLID);
		}
		
		return move;
	}
	
	// If the ghost is closer to a powerpill than Pacman, it returns the node that powerpill is on
	private int ghostCloserToPPill(Game game, int limit, int msPacManNode, GHOST ghost) {
		if(game.isGhostEdible(ghost)) return -1;
		
		int[] pPillsArray = game.getActivePowerPillsIndices();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
		int ghostNode = game.getGhostCurrentNodeIndex(ghost);
		MOVE ghostMove = game.getGhostLastMoveMade(ghost);
		for (int pPill : pPillsArray) {
			double dist = game.getDistance(msPacManNode, pPill, msPacManMove, DM.EUCLID);
			if (dist <= limit && dist > game.getDistance(ghostNode, pPill, ghostMove, DM.EUCLID))
				return pPill;
		}
		return -1;
	}
	
	private boolean pacManCloseToPPill(Game game, int limit, int msPacManNode) {
		int[] pPillsArray = game.getActivePowerPillsIndices();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
		for (int pPill : pPillsArray)
			if (game.getDistance(msPacManNode, pPill, msPacManMove, DM.EUCLID) <= limit)
				return true;
		return false;
	}

	// If ghost is close to another ghost (within limit), return the other ghost
	private GHOST isNearOtherGhost(Game game, int limit, GHOST ghost) {
		for (GHOST ghostType : GHOST.values())
			if (ghost != ghostType && game.getDistance(game.getGhostCurrentNodeIndex(ghost),
					game.getGhostCurrentNodeIndex(ghostType), DM.EUCLID) < limit)
				return ghostType;
		return null;
	}

	// Returns the next intersection from pos at direction dir
	private int getAmbushPosition(Game game, MOVE dir, int pos) {
		int aux = game.getNeighbour(pos, dir);
		while (aux != -1 && !game.isJunction(pos)) {
			pos = aux;
			aux = game.getNeighbour(pos, dir);
		}
		return pos;
	}
}