package PacMans;
import java.util.HashSet;
import java.util.Set;

import Utils.QConstants;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Class that implements the chasing behaviour of mspacman with qlearn 
 */
public class QPacManChase extends QPacMan {
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int nextState;
    // -10:mspacman eaten, 1 ghost eaten
    private final int[] REWARD = {-10, 1};


    public QPacManChase(QLearner learner) {
		this.agent = learner;
    }
    
    //method that sets new game
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	
    	// MsPacMan info
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
  		
		//Ghosts info
		GHOST ghost = getNearestGhost(msPacManNode, msPacManMove);
		boolean edible = false;
		int distanceGhost = 3;
		MOVE directionGhost = MOVE.UP;
		
		
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
			
			//discretize distance to ghosts
			if(distanceGhost <= 20 )
				distanceGhost = 0;
			else if(distanceGhost <= 50)
				distanceGhost = 1;
			else if(distanceGhost <= 90)
				distanceGhost = 2;
			else
				distanceGhost = 3;
		}
		// Next state is updated
    	calculateState(distanceGhost, directionGhost);

    }
    /**
     * Method that gets the next move from mspacman in a normal game
     */
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
    
    /**
     * Method that calculate the state using the distance to the nearest ghost and the direction
     */
    private void calculateState(int distanceGhost, MOVE directionGhost) {
    	this.nextState =  directionGhost.ordinal()*10 + distanceGhost;
    	if(nextState == -1) {
			System.out.println("Fallo");
		}
    }
    /**
     * Method that updates the strategy using the rewards that are needed in each case
     */
    public void updateStrategy() {
    	int msPacManNode = -1;
    	MOVE msPacManMove = null;
    	
    	
    	GHOST ghost = null;
    	int distanceGhost = 3;
    	int ghostNode = -1;
    	
		int reward;
    	boolean eatenGhost = false;
    	for (GHOST g: GHOST.values()) {
    		if (game.wasGhostEaten(g)) {
    			eatenGhost = true;
    			break;
    		}
    	}
		// Reward calculation	
    	if (game.wasPacManEaten())
    		reward = REWARD[0];
    	else if (eatenGhost)
    		reward = REWARD[1];
    	else
    		reward = 0;
    	
    	// MsPacMan info
    	msPacManNode = game.getPacmanCurrentNodeIndex();
		msPacManMove = game.getPacmanLastMoveMade();
		
		//Ghost info
		ghost = getNearestGhost(msPacManNode, msPacManMove);
		boolean edible = false;
		MOVE directionGhost = MOVE.UP;
    		
    		
		if(ghost != null) {
			edible = game.isGhostEdible(ghost);
			
			ghostNode = game.getGhostCurrentNodeIndex(ghost);
			
			if(edible) {
				distanceGhost = game.getShortestPathDistance(msPacManNode, ghostNode, msPacManMove);
				directionGhost = game.getNextMoveTowardsTarget(msPacManNode, ghostNode, msPacManMove, DM.PATH);
			}
			else {
				distanceGhost = game.getShortestPathDistance(ghostNode, msPacManNode, game.getGhostLastMoveMade(ghost));
				directionGhost = game.getNextMoveTowardsTarget(ghostNode, msPacManNode, game.getGhostLastMoveMade(ghost), DM.PATH);
			}
			
			//discretize distance to ghost
			if(distanceGhost <= 20 )
				distanceGhost = 0;
			else if(distanceGhost <= 50)
				distanceGhost = 1;
			else if(distanceGhost <= 90)
				distanceGhost = 2;
			else
				distanceGhost = 3;
		}
   
		// Attributes are updated	
		if(game.isJunction(game.getPacmanCurrentNodeIndex()))
		this.lastJunctionState = this.nextState;
		
    	calculateState(distanceGhost, directionGhost);  //get state
    	
        agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), this.nextState, QConstants.actions, reward);  //update agent
    }
    
    /**
     * @return The nearest ghost to MsPacMan taking into account last MsPacMan move
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
	public String getActionId() {
		return "Chase Action";
	}
	/**
	 * Method that gets the next move in the FSM
	 */
	@Override
	public MOVE execute(Game game) {
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
}

