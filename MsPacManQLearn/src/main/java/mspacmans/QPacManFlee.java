package mspacmans;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import Utils.QConstants;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.game.Game;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

/**
 * Class that implements the flee behaviour of mspacman 
 */

public class QPacManFlee extends QPacMan{
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int nextState;

    //-1000:mspacman eaten, 1 rest
    private final int[] REWARD = {-1000, 10, 1};
    private int reward = 0;

    public QPacManFlee(QLearner learner) {
		this.agent = learner;
    }
    
    /**
     * method that sets new game
     */
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	
    	// MsPacMan info
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
  		
		// Pills info
		int pillNode = getNearestPill(msPacManNode, msPacManMove);
		int distancePill = game.getShortestPathDistance(msPacManNode, pillNode);
		//Ghosts info
		GHOST ghost = getNearestGhost(msPacManNode, msPacManMove);
		boolean edible = false;
		int distanceGhost = 9;
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
			
			//discretize distance to ghosts
			distanceGhost = discretizeDistance(distanceGhost);
		}
		
		//discretize distance to pills
		distancePill = discretizeDistance(distancePill);
		
		// Next state is updated
    	calculateState(distanceGhost, distancePill, directionGhost, directionPill);
    	
    	this.lastJunctionState = this.nextState;
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
    private void calculateState(int distanceGhost, int distancePill, MOVE directionGhost, MOVE directionPill) {
    	this.nextState = directionGhost.ordinal()*1000  + directionGhost.ordinal()*100 + distanceGhost * 10 + distancePill;
    }
    
    private int discretizeDistance(int distance) {
    	if(distance <= 5 )
    		return 0;
		else if(distance <= 10)
			return 1;
		else if(distance <= 20)
			return 2;
		else if(distance <= 30)
			return 3;
		else if(distance <= 40)
			return 4;
		else if(distance <= 60)
			return 5;
		else if(distance <= 80)
			return 6;
		else if(distance <= 100)
			return 7;
		else if(distance <= 150)
			return 8;
		else
			return 9;
    }
    
    /**
     * Method that updates the strategy using the rewards that are needed in each case
     */
    public void updateStrategy() {
    	int msPacManNode = -1;
    	MOVE msPacManMove = null;
    	int pillNode = -1;
    	int distancePill = 9;
    	GHOST ghost = null;
    	int distanceGhost = 9;
    	int ghostNode = -1;
    	try {
    		
        	
    		if (game.wasPacManEaten())
        		reward += REWARD[0];
        	else if(game.wasPillEaten())
        		reward += REWARD[1];
        	else
        		reward += REWARD[2];
        	
        	//mspacman info
        	msPacManNode = game.getPacmanCurrentNodeIndex();
    		msPacManMove = game.getPacmanLastMoveMade();
      		
    		//pills info
    		pillNode = getNearestPill(msPacManNode, msPacManMove);
    		distancePill = game.getShortestPathDistance(msPacManNode, pillNode);
    		
    		//ghosts info
    		ghost = getNearestGhost(msPacManNode, msPacManMove);
    		boolean edible = false;
    		MOVE directionGhost = MOVE.UP, directionPill = game.getNextMoveTowardsTarget(msPacManNode, pillNode, msPacManMove, DM.PATH);;
    		
    		
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
    			distanceGhost = discretizeDistance(distanceGhost);
    		}
    		
    		distancePill = discretizeDistance(distancePill);
   		
        	calculateState(distanceGhost, distancePill, directionGhost, directionPill);
     	   
    		// Attributes are updated	
    		if(game.isJunction(game.getPacmanCurrentNodeIndex())) {
    			agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), this.nextState, QConstants.actions, reward);  //update agent
        		reward = 0;
    			this.lastJunctionState = this.nextState;
    		}
    	} catch(Exception e) {
    		System.out.println("MsNode: " + msPacManNode);
    		System.out.println("MsMove: " + msPacManMove);
    		System.out.println("NumPills: " + game.getNumberOfActivePills());
    		System.out.println("PillNode: " + pillNode);
    		System.out.println("PillDtnce: " + distancePill);
    		System.out.println("Ghost: " + ghost);
    		System.out.println("GhostNode: " + ghostNode);
    		System.out.println("GhostDtnce: " + distanceGhost);
    		System.out.println(game.getGameState());
    	}
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
  	
  	/**
     * @return The nearest pill to MsPacMan taking into account last MsPacMan move
     */
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

	@Override
	public String getActionId() {
		return "Flee Action";
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

