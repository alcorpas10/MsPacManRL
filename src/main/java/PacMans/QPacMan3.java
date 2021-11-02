package PacMans;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import Utils.Pair;
import Utils.QConstants;
import chen0040.rl.learning.qlearn.QLearner;
import pacman.game.Game;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public class QPacMan3 {
    private Game game;
    private QLearner agent;
    private MOVE lastJunctionMove;
    private int lastJunctionState;
    private int nextState;

    private final int[] REWARD = {-1000, -10000, -1000000, -10};


    public QPacMan3(QLearner learner) {
		this.agent = learner;
    }
    
    public void setNewGame(Game game) {
    	this.game = game;
    	this.lastJunctionMove = MOVE.LEFT;
    	
    	int msPacManNode = game.getPacmanCurrentNodeIndex();
		MOVE msPacManMove = game.getPacmanLastMoveMade();
  		
		int distancePill = getDistanceNearestPill(msPacManNode, msPacManMove);
		Pair<Boolean, Integer> ghostData = getDistanceNearestGhost(msPacManNode, msPacManMove);
		boolean edible = ghostData.getFirst();
		int distanceGhost = ghostData.getSecond();
    	
    	calculateState(distanceGhost, distancePill, edible);

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
    
    private void calculateState(int distanceGhost, int distancePill, boolean edible) {
    	if (distanceGhost == -1)
			distanceGhost = QConstants.maxDistance+1;
		if (distancePill == -1)
			distancePill = QConstants.maxDistance+1;
		this.nextState = distanceGhost * (QConstants.maxDistance+2) + distancePill;
		
		if (edible)
			this.nextState += (QConstants.maxDistance+2)*(QConstants.maxDistance+2);
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
  		
		int distancePill = getDistanceNearestPill(msPacManNode, msPacManMove);
		Pair<Boolean, Integer> ghostData = getDistanceNearestGhost(msPacManNode, msPacManMove);
		boolean edible = ghostData.getFirst();
		int distanceGhost = ghostData.getSecond();
		
		if(game.isJunction(game.getPacmanCurrentNodeIndex()))
			this.lastJunctionState = this.nextState;
    	
    	calculateState(distanceGhost, distancePill, edible);
    	agent.update(this.lastJunctionState, this.lastJunctionMove.ordinal(), this.nextState, reward);
    }
    
    //Buscar los ghost no comibles problematicos
  	private Pair<Boolean, Integer> getDistanceNearestGhost(int msPacManNode, MOVE msPacManMove) {
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
  			return new Pair<Boolean, Integer>(false, -1);
  		return new Pair<Boolean, Integer>(game.getGhostEdibleTime(ghost) != 0, d);
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

