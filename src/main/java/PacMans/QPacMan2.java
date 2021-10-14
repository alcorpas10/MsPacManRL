package PacMans;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.github.chen0040.rl.learning.qlearn.QLearner;

import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class QPacMan2 {
    protected Game game;
    private final QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;


    protected static Random random = new Random(42);

    protected final int[] REWARD = {-100, -10000};

    public QPacMan2(QLearner learner) {
		this.agent = learner;
    }
    
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	this.lastJunctionState = game.getNumberOfActivePills();
    }

    public MOVE act() {
    	
    	if(game.isJunction(game.getPacmanCurrentNodeIndex())) {
    		int maxDistancePill=0;
    		int maxDistanceGhost=0;
    		int distancePill=0;
    		int distanceGhost=0; 
    		
    		if(distanceGhost== -1 ) {
    			if (distancePill ==-1) {
    				this.lastJunctionState = (maxDistanceGhost+1) * (maxDistancePill+1) +(maxDistancePill +1) ;
    			} 
    			else {
    				this.lastJunctionState = (maxDistanceGhost+1) * (maxDistancePill+1) + distancePill ;
    			}
    		}
    		else if(distancePill == -1) {
        		this.lastJunctionState = distanceGhost * (maxDistancePill +1)+ (maxDistancePill +1);

    		}
    		else {
        		this.lastJunctionState = distanceGhost * (maxDistancePill+1) + distancePill;

    		}
	    	//1 pill-2m 
	    	//2pill-1 m 
	
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
    public void updateStrategy() {
    	int reward = (game.wasPillEaten())? REWARD[0] : REWARD[1];
    	agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), game.getNumberOfActivePills(), reward);
    }
  //Buscar los ghost no comibles problematicos
  	private GHOST getNearestChasingGhosts(Game game, int limit) {
  		int mspacman = game.getPacmanCurrentNodeIndex();
  		int d =Integer.MAX_VALUE;
  		GHOST gh = null;
  		for(Constants.GHOST g: Constants.GHOST.values()) { //miramos entre todos los ghosts
  			int ghost = game.getGhostCurrentNodeIndex(g);
  			int di = game.getShortestPathDistance(mspacman, ghost);
  			if(di<d) {
  				d=di;
  				gh=g;
  			}
  		}
  		
  		return gh;
  	}

}

