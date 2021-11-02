package PacMans;
import java.util.HashSet;
import java.util.Set;

import chen0040.rl.learning.qlearn.QLearner;
import pacman.game.Game;
import pacman.game.Constants.MOVE;

public class QPacMan {
    private Game game;
    private final QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;

    private final int[] REWARD = {-100, -10000};

    
    public QPacMan(QLearner learner) {
		this.agent = learner;
    }
    
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	this.lastJunctionState = game.getNumberOfActivePills();
    }

    public MOVE act() {
    	
    	if(game.isJunction(game.getPacmanCurrentNodeIndex())) {
    		this.lastJunctionState = game.getNumberOfActivePills();
	    	
	
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

}

