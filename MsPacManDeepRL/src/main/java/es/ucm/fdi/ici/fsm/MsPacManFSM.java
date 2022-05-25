package es.ucm.fdi.ici.fsm;

import java.io.IOException;
import java.net.Socket;
import es.ucm.fdi.ici.Input;
import pacman.controllers.PacmanController;
import pacman.game.Constants.MOVE;
import mspacman.MsPacMan;
import pacman.game.Game;

/**
 * Class that implements the FSM. 
 * It has two mspacman with different behaviors that change if its necessary using the transitions.
 */
public class MsPacManFSM extends PacmanController {

	FSM fsm;
	MsPacMan msPacManNotEdible; 
	MsPacMan msPacManEdible;
	
	public MsPacManFSM() {
		fsm = new FSM("MsPacMan");
    	
		//initialize the sockets
		Socket socketNotEdible = null;
		Socket socketEdible = null;
		try {
			socketNotEdible = new Socket("localhost",38514);  
			socketEdible = new Socket("localhost",38515);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		 //creates the mspacmans
    	msPacManNotEdible = new MsPacMan(socketNotEdible, "MsPacManNotEdible", 1); 
    	msPacManEdible = new MsPacMan(socketEdible, "MsPacManEdible", 2);
		
    	//creates the two states, edible and not edible
    	SimpleState edibleGhostState = new SimpleState("edibleGhostState", msPacManEdible);
    	SimpleState notEdibleGhostState = new SimpleState("notEdibleGhostState", msPacManNotEdible);
    	
    	//adds the states with their corresponding transition 
     	fsm.add(edibleGhostState, new FleeTransition("toNotEdibleGhost"), notEdibleGhostState);
    	fsm.add(notEdibleGhostState, new ChaseTransition("toEdibleGhost"), edibleGhostState);
    	
    	fsm.ready(notEdibleGhostState);
	}
	
	/**
	 * Sends to both mspacmans that the game is over
	 *
	 */
	public void gameOver(Game game) {
		msPacManNotEdible.gameOver(game);
		msPacManEdible.gameOver(game);
	}
	/**
	 * Gets from both python servers the ok
	 * 
	 */
	public void getOk() {
		msPacManNotEdible.getOk();
		msPacManEdible.getOk();
	}
	/**
	 * Resets the fsm before computing the game
	 */
	public void preCompute(String opponent) {
		fsm.reset();
    }
	/**
	 * Gets the move made from the fsm
	 */
    @Override
    public MOVE getMove(Game game, long timeDue) {
       	Input in = new MsPacManInput(game); 
       	
       	MOVE m=fsm.run(in);
       	
    	return m;
    }
    
    /**
     * Initializes the mspacmans 
     */
	public void init(Game game) {
		msPacManNotEdible.init(game);
		msPacManEdible.init(game);
	}
}