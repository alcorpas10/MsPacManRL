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
import pacman.game.Constants.MOVE;

/**
 * Class that implements the  behaviour of mspacman with only pills in the maze
 */

public class QPacManPills extends QPacMan {
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int nextState;
    private int lastPillDistance;

    // 10: eat pill, 1: get closer to pill, -1: move away from pill
    private final int[] REWARD = {10, 1, -5};
    
    private final double[] MULTIPLIER = {5, 3, 2, 1.5, 1};
    private int reward = 0;


    // State codification: directionPill, distancePill : Max number -> 33
    
    public QPacManPills(QLearner learner) {
    	this.agent = learner;
    }
    
    /**
     * method that sets new game
     */
    public void setNewGame(Game game) {
		this.game = game;

    	// MsPacMan info
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
  		
		// Pills info
		int pillsNumber = game.getNumberOfActivePills();
		int pillNode = getNearestPill(msPacManNode, msPacManMove);
		int distancePill = game.getShortestPathDistance(msPacManNode, pillNode);
		MOVE directionPill = game.getNextMoveTowardsTarget(msPacManNode, pillNode, msPacManMove, DM.PATH);
		
		pillsNumber = discretizePillsNumber(pillsNumber);
		distancePill = discretizePillDistance(distancePill);
		
		// Next state is updated
    	calculateState(pillsNumber, directionPill, distancePill);

		
    	this.lastJunctionMove = msPacManMove;
		this.lastPillDistance = distancePill;
    	this.lastJunctionState = this.nextState;
    }
    /**
     * Method that gets the next move from mspacman in a normal game
     */
    public MOVE act() {
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
    	
    	if(game.isJunction(msPacManNode)) {
     
	        MOVE[] possibleActions = game.getPossibleMoves(msPacManNode, game.getPacmanLastMoveMade());
	        
	        Set<Integer> possibleActionsSet = new HashSet<>();
	        
	        for(MOVE a : possibleActions)
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
    private void calculateState(int pillsNumber, MOVE directionPill, int distancePill) {    	
    	this.nextState = directionPill.ordinal()*10 + distancePill;
    }
    
    /**
     * Method that discretize the number of active pills 
     */
    private int discretizePillsNumber(int number) {
    	if(number <= 10)
			return 0;
		else if(number <= 25)
			return 1;
		else if(number <= 60)
			return 2;
		else if(number <= 120)
			return 3;
		else
			return 4;
    }
    /**
     * Method that discretize the distance to a pill
     */
    private int discretizePillDistance(int distance) {
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
    	MOVE msPacManMove = MOVE.NEUTRAL;
    	int pillsNumber = -1;
    	int pillNode = -1;
    	int distancePill = -1;
    	MOVE directionPill = MOVE.NEUTRAL;
    	try {
        	
    		// MsPacMan info
    		msPacManNode = game.getPacmanCurrentNodeIndex();
    		msPacManMove = game.getPacmanLastMoveMade();
      		
    		// Pills info
    		pillsNumber = game.getNumberOfActivePills();
    		pillNode = getNearestPill(msPacManNode, msPacManMove);
    		distancePill = game.getShortestPathDistance(msPacManNode, pillNode);
    		directionPill = game.getNextMoveTowardsTarget(msPacManNode, pillNode, msPacManMove, DM.PATH);
    		
    		pillsNumber = discretizePillsNumber(pillsNumber);
    		distancePill = discretizePillDistance(distancePill);
    		
    		// Reward calculation
    		if (game.wasPillEaten())
        		reward += (int) (REWARD[0] * MULTIPLIER[pillsNumber]);
    		else if (distancePill <= this.lastPillDistance)
    			reward += REWARD[1];
        	else
        		reward += REWARD[2];
    		        	
        	calculateState(pillsNumber, directionPill, distancePill);      	
        	
    		// Attributes are updated	
    		if(game.isJunction(game.getPacmanCurrentNodeIndex())) {
    			agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), this.nextState, QConstants.actions, reward);  //update agent
        		reward = 0;
    			this.lastJunctionState = this.nextState;
    		}
    	} catch(Exception e) {
    		e.printStackTrace();
    		System.out.println("MsNode:    " + msPacManNode);
    		System.out.println("MsMove:    " + msPacManMove);
    		System.out.println("NumPills:  " + game.getNumberOfActivePills());
    		System.out.println("PillNode:  " + pillNode);
    		System.out.println("PillDtnce: " + distancePill);
    		System.out.println("PillDir:   " + directionPill);
    		System.out.println(game.getGameState());
    	}
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
  		if (nearestPills.isEmpty()) {
  			System.out.println("This shouldn't be printed"); 
  			return -1;
  		}
  		else {
  			Random rnd = new Random();
  			return nearestPills.get(rnd.nextInt(nearestPills.size()));
  		}
  	}

	@Override
	public String getActionId() {
		return "Pills Action";
	}

	/**
	 * Method that gets the next move in the FSM
	 */
	@Override
	public MOVE execute(Game game) {
		int msPacManNode = game.getPacmanCurrentNodeIndex();
    	
    	if(game.isJunction(msPacManNode)) {
     
	        MOVE[] possibleActions = game.getPossibleMoves(msPacManNode, game.getPacmanLastMoveMade());
	        
	        Set<Integer> possibleActionsSet = new HashSet<>();
	        
	        for(MOVE a : possibleActions)
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

