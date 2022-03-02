package ghosts;

import java.util.EnumMap;

import engine.pacman.controllers.GhostController;
import engine.pacman.game.Constants.DM;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;
import engine.pacman.game.Game;

public class GhostAlgorithmic extends GhostController {
	// Distancia a la que se separan los fantasmas mientras persiguen a Pacman
	public final int CHASE_PROXIM_LIMIT = 30; 
	// Distancia a la que se separan los fantasmas mientras huyen de Pacman
	public final int FLEE_PROXIM_LIMIT = 30;
	// Distancia entre Pacman y una Power Pill a la que huyen los fantasmas
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
					// HUIDA DE PACMAN
					moves.put(ghost, getGhostFleeMove(game, ghost, msPacManNode));
					
				} else {
					// PERSEGUIR A PACMAN
					moves.put(ghost, getGhostChaseMove(game, ghost, msPacManNode));
				}
			}
		}
		return moves;
	}

	// Devuelve el movimiento a realizar por ghost para huir de MsPacman
	private MOVE getGhostFleeMove(Game game, GHOST ghost, int msPacManNode) {
		MOVE move;
		other = isNearOtherGhost(game, FLEE_PROXIM_LIMIT, ghost);

		// Si hay un fantasma cercano, evita huir por el mismo camino
		// Si no, huye de pacman
		if (other != null)
			move = game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
					game.getGhostCurrentNodeIndex(other), game.getGhostLastMoveMade(ghost), DM.EUCLID);
		else
			move = game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
					msPacManNode, game.getGhostLastMoveMade(ghost), DM.EUCLID); 
		
		return move;
	}
	
	// Devuelve el movimiento a realizar por ghost para huir de MsPacman
	private MOVE getGhostChaseMove(Game game, GHOST ghost, int msPacManNode) {
		MOVE move = null;
		other = isNearOtherGhost(game, CHASE_PROXIM_LIMIT, ghost);
		int ppillCloserToGhost = ghostCloserToPPill(game, PPILL_FLEE_DIST, msPacManNode, ghost );
		
		if (ppillCloserToGhost >= 0) {
			move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
					ppillCloserToGhost, game.getGhostLastMoveMade(ghost), DM.EUCLID);
		} else if(other != null) {
			// Si hay otro fantasma cercano, persigue a Pacman con prediccion (getAmbushPosition)
			MOVE dir = game.getPacmanLastMoveMade();
			int pos = msPacManNode;

			pos = getAmbushPosition(game, dir, pos);
			move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
					pos, game.getGhostLastMoveMade(ghost), DM.EUCLID);
		} else {
			// Persigue a Pacman yendo detras de el
			move = game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
					msPacManNode, game.getGhostLastMoveMade(ghost), DM.EUCLID);
		}
		
		return move;
	}
	
	// Si el fantasma se encuentra mas cercano a una powerpill que Pacman, devuelve el nodo en el que se encuentra dicha ppill
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

	// Si ghost esta cerca de otro fantasma (dentro de limit), devuelve el otro fantasma
	private GHOST isNearOtherGhost(Game game, int limit, GHOST ghost) {
		for (GHOST ghostType : GHOST.values())
			if (ghost != ghostType && game.getDistance(game.getGhostCurrentNodeIndex(ghost),
					game.getGhostCurrentNodeIndex(ghostType), DM.EUCLID) < limit)
				return ghostType;
		return null;
	}

	// Devuelve la siguiente interseccion desde pos en la direccion dir
	private int getAmbushPosition(Game game, MOVE dir, int pos) {
		int aux = game.getNeighbour(pos, dir);
		while (aux != -1 && !game.isJunction(pos)) {
			pos = aux;
			aux = game.getNeighbour(pos, dir);
		}
		return pos;
	}
}