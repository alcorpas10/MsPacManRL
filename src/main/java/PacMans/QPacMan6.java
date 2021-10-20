package PacMans;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.github.chen0040.rl.learning.qlearn.QLearner;

import Utils.Pair;
import Utils.QConstants;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class QPacMan6 {
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int nextState;

    private final int[] REWARD = {-1000, -10000, -1000000, -10};


    public QPacMan6(QLearner learner) {
		this.agent = learner;
    }
    
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
  		
		int pillNode = getNearestPill(msPacManNode, msPacManMove);
		int distancePill = game.getShortestPathDistance(msPacManNode, pillNode);
		GHOST ghost = getNearestGhost(msPacManNode, msPacManMove);
		boolean edible = false;
		int distanceGhost = 4;
		MOVE directionGhost = MOVE.UP, directionPill = game.getNextMoveTowardsTarget(msPacManNode, pillNode, msPacManMove, DM.PATH);;
		
		
		if(ghost != null) {
			edible = game.isGhostEdible(ghost);
			
			int ghostNode = game.getGhostCurrentNodeIndex(ghost);
			
			if(edible) {
				distanceGhost = game.getShortestPathDistance(msPacManNode, ghostNode, msPacManMove);
				directionGhost = game.getNextMoveTowardsTarget(msPacManNode, ghostNode, msPacManMove, DM.PATH);
			}
			else {
				distanceGhost = game.getShortestPathDistance(ghostNode, msPacManNode, game.getGhostLastMoveMade(ghost));
				directionGhost = game.getNextMoveTowardsTarget(ghostNode, msPacManNode, game.getGhostLastMoveMade(ghost), DM.PATH);
			}
			
			if(distanceGhost <= 20 )
				distanceGhost = 0;
			else if(distanceGhost <= 50)
				distanceGhost = 1;
			else if(distanceGhost <= 90)
				distanceGhost = 2;
			else
				distanceGhost = 3;
		}
		
		if(distancePill <= 20 && distancePill >= 0)
			distancePill = 0;
		else if(distancePill <= 50)
			distancePill = 1;
		else if(distancePill <= 90)
			distancePill = 2;
		else
			distancePill = 3;
		
		
    	calculateState(distanceGhost, distancePill, edible, directionGhost, directionPill);

    }

    public MOVE act() {
    	if(game.isJunction(game.getPacmanCurrentNodeIndex())) {
     
	        MOVE[] possibleActions = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
	        
	        Set<Integer> possibleActionsSet = new HashSet<>();
	        
	        for(MOVE a: possibleActions)
	        	possibleActionsSet.add(a.ordinal());
	
	        if(!possibleActionsSet.isEmpty()) {
	        	int action = agent.selectAction(lastJunctionState, possibleActionsSet).getIndex();
	            switch(action) {
	    		case 0:
	    			this.lastJunctionMove = MOVE.UP;
	    			break;
	    		case 1:
	    			this.lastJunctionMove = MOVE.RIGHT;
	    			break;
	    		case 2:
	    			this.lastJunctionMove = MOVE.DOWN;
	    			break;
	    		case 3:
	    			this.lastJunctionMove = MOVE.LEFT;
	    			break;
	    		default:
	    			this.lastJunctionMove = MOVE.NEUTRAL;
	            }     
	        }
	        return this.lastJunctionMove;
    	}
    	return MOVE.NEUTRAL;
		
    }
    
    private void calculateState(int distanceGhost, int distancePill, boolean edible, MOVE directionGhost, MOVE directionPill) {
		
    	int edibleInt =(edible)? 1:0;
    	
    	this.nextState = edibleInt*10000 + directionGhost.ordinal()*1000 + directionPill.ordinal()*100 + distanceGhost*10 + distancePill;
    }
    
    /**
     * Basic strategy 
     */
    public void updateStrategy() {
    	int reward;
    	boolean eatenGhost = false;
    	for (GHOST g: GHOST.values()) {
    		if (game.wasGhostEaten(g)) {
    			eatenGhost = true;
    			break;
    		}
    	}
    	
    	if (game.wasPacManEaten())
    		reward = REWARD[2];
    	else if (eatenGhost)
    		reward = REWARD[3];
    	else if (game.wasPillEaten())
    		reward = REWARD[0];
    	else
    		reward = REWARD[1];
    	
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
  		
		int pillNode = getNearestPill(msPacManNode, msPacManMove);
		int distancePill = game.getShortestPathDistance(msPacManNode, pillNode);
		GHOST ghost = getNearestGhost(msPacManNode, msPacManMove);
		boolean edible = false;
		int distanceGhost = 4;
		MOVE directionGhost = MOVE.UP, directionPill = game.getNextMoveTowardsTarget(msPacManNode, pillNode, msPacManMove, DM.PATH);;
		
		
		if(ghost != null) {
			edible = game.isGhostEdible(ghost);
			
			int ghostNode = game.getGhostCurrentNodeIndex(ghost);
			
			if(edible) {
				distanceGhost = game.getShortestPathDistance(msPacManNode, ghostNode, msPacManMove);
				directionGhost = game.getNextMoveTowardsTarget(msPacManNode, ghostNode, msPacManMove, DM.PATH);
			}
			else {
				distanceGhost = game.getShortestPathDistance(ghostNode, msPacManNode, game.getGhostLastMoveMade(ghost));
				directionGhost = game.getNextMoveTowardsTarget(ghostNode, msPacManNode, game.getGhostLastMoveMade(ghost), DM.PATH);
			}
			
			if(distanceGhost <= 20 )
				distanceGhost = 0;
			else if(distanceGhost <= 50)
				distanceGhost = 1;
			else if(distanceGhost <= 90)
				distanceGhost = 2;
			else
				distanceGhost = 3;
		}
		
		if(distancePill <= 20 && distancePill >= 0)
			distancePill = 0;
		else if(distancePill <= 50)
			distancePill = 1;
		else if(distancePill <= 90)
			distancePill = 2;
		else
			distancePill = 3;
		
		if(game.isJunction(game.getPacmanCurrentNodeIndex()))
			this.lastJunctionState = this.nextState;
		
    	calculateState(distanceGhost, distancePill, edible, directionGhost, directionPill);
    	
    	agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), this.nextState, reward);
    }
    
    //Buscar los ghost no comibles problematicos
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
  	
  	//Metodo que obtiene la pill mas cercana y en caso de haber varias a la misma distancia obtiene la direccion a una
  	//de ellas de forma pseudoaleatoria
  	private int getNearestPill(int msPacManNode, MOVE msPacManMove) {
  		int[] pillsArray = game.getActivePillsIndices();
  		List<Integer> nearestPills = new ArrayList<Integer>();
  		double distance, minDistance = Double.MAX_VALUE;
  		for (int pill : pillsArray) {
  			distance = game.getShortestPathDistance(msPacManNode, pill, msPacManMove);
  			if(distance == minDistance)
  				nearestPills.add(pill);
  			else if( distance < minDistance) {
  				minDistance = distance;
  				nearestPills.clear();
  				nearestPills.add(pill);
  			}
  		}
  		if (nearestPills.isEmpty())
  			return -1;
  		else {
  			Random rnd = new Random();
  			return nearestPills.get(rnd.nextInt(nearestPills.size()));
  		}
  	}
}

