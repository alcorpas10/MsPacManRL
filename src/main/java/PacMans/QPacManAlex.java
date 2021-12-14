package PacMans;
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

public class QPacManAlex {
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int nextState;
    private int lastPillDistance;

    // 10: eat pill, 1: get closer to pill, -1: move away from pill
    private final int[] REWARD = {10, 1, -1};
    
    private final double[] MULTIPLIER = {5, 3, 2, 1.5, 1};

    // State codification: pillsNumber, directionPill, distancePill : Max number -> 437 TODO directionPill 4?
    
    public QPacManAlex(QLearner learner) {
    	this.agent = learner;
    }
    
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

		// Initialization of class attributes
    	this.lastJunctionMove = msPacManMove;
		this.lastPillDistance = distancePill;
    	this.lastJunctionState = this.nextState;
    }

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
    
    private void calculateState(int pillsNumber, MOVE directionPill, int distancePill) {    	
    	this.nextState = /*pillsNumber*100*/ + directionPill.ordinal()*10 + distancePill;
    }
    
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
    
    private int discretizePillDistance(int distance) {
    	/*if(distance <= 10 && distance >= 0)
			return 0;
		else if(distance <= 20)
			return 1;
		else if(distance <= 30)
			return 2;
		else if(distance <= 45)
			return 3;
		else if(distance <= 60)
			return 4;
		else if(distance <= 80)
			return 5;
		else if(distance <= 100)
			return 6;
		else
			return 7;*/
		if(distance <= 20)
			return 0;
		else if(distance <= 50)
			return 1;
		else if(distance <= 80)
			return 2;
		else
			return 3;
    }
    
    /**
     * Basic strategy 
     */
    public void updateStrategy() {
    	int msPacManNode = -1;
    	MOVE msPacManMove = MOVE.NEUTRAL;
    	int pillsNumber = -1;
    	int pillNode = -1;
    	int distancePill = -1;
    	MOVE directionPill = MOVE.NEUTRAL;
    	try {
    		int reward;
        	
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
        		reward = (int) (REWARD[0] * MULTIPLIER[pillsNumber]);
    		else if (distancePill <= this.lastPillDistance)
    			reward = REWARD[1];
        	else
        		reward = REWARD[2];
    		        	
    		// Attributes are updated
        	calculateState(pillsNumber, directionPill, distancePill);

        	this.lastPillDistance = distancePill;
    		if(game.isJunction(game.getPacmanCurrentNodeIndex()))
    			this.lastJunctionState = this.nextState;
        	
        	agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), this.nextState, QConstants.actions, reward);
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
}

