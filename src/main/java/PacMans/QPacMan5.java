package PacMans;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.chen0040.rl.learning.qlearn.QLearner;

import Utils.QConstants;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class QPacMan5 {
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int lastScore;
    private int lastTime;


    public QPacMan5(QLearner learner) {
		this.agent = learner;
    }
    
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	this.lastJunctionState = game.getNumberOfActivePills();
    }

    public MOVE act() {
    	if(game.isJunction(game.getPacmanCurrentNodeIndex())) {
      		int msPacManNode = game.getPacmanCurrentNodeIndex();
    		MOVE msPacManMove = game.getPacmanLastMoveMade();
      		
    		int maxDistance = QConstants.maxDistance;
    		int distancePill = getDistanceNearestPill(msPacManNode, msPacManMove);
    		
    		int distanceChasingGhost = getDistanceNearestChasingGhost(msPacManNode, msPacManMove);
       		int distanceEdibleGhost = getDistanceNearestEdibleGhost(msPacManNode, msPacManMove);
    		
    		if (distanceChasingGhost == -1)
    			distanceChasingGhost = maxDistance+1;
	    	if(distanceEdibleGhost == -1)
	    		distanceEdibleGhost = maxDistance+1;
	    	if (distancePill == -1)
	    		distancePill = maxDistance+1;
	        this.lastJunctionState = distanceChasingGhost * ((maxDistance+2)*(maxDistance+2))
	        		+ (distanceEdibleGhost*(maxDistance+2)+distancePill);
	
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
    
    /**
     * Basic strategy 
     */
    public void updateStrategy() {
    	/*double oldTicks = oldDescription.getTicks(), currentTicks = newDescription.getTicks();
		double ticks = currentTicks - oldTicks;
		val = Math.round((score / ticks) * 100.0) / 100.0;
    	agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), game.getNumberOfActivePills(), reward);*/
    }
    
    //Buscar los ghost no comibles problematicos
  	private int getDistanceNearestChasingGhost(int msPacManNode, MOVE msPacManMove) {
  		int d = Integer.MAX_VALUE;
  		for(GHOST g: GHOST.values()) { //miramos entre todos los ghosts
  			if (!game.isGhostEdible(g)) {
	  			int di = game.getShortestPathDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), msPacManMove);
	  			if(di != -1 && di < d)
	  				d = di;
  			}
  		}
  		if (d == Integer.MAX_VALUE)
  			d = -1;
  		return d;
  	}
  	
  	private int getDistanceNearestEdibleGhost(int msPacManNode, MOVE msPacManMove) {
  		int d = Integer.MAX_VALUE;
  		for(GHOST g: GHOST.values()) { //miramos entre todos los ghosts
  			if (game.isGhostEdible(g)) {
	  			int di = game.getShortestPathDistance(msPacManNode, game.getGhostCurrentNodeIndex(g), msPacManMove);
	  			if(di != -1 && di < d)
	  				d = di;
  			}
  		}
  		if (d == Integer.MAX_VALUE)
  			d = -1;
  		return d;
  	}
  	
  	//Metodo que obtiene la pill mas cercana y en caso de haber varias a la misma distancia obtiene la direccion a una
  	//de ellas de forma pseudoaleatoria
  	private int getDistanceNearestPill(int msPacManNode, MOVE msPacManMove) {
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
  		return game.getShortestPathDistance(msPacManNode, nearestPills.get(0), msPacManMove);
  	}
}

