package es.ucm.fdi.ici.fsm;

import java.io.IOException;
import java.net.Socket;
import es.ucm.fdi.ici.Input;
import pacman.controllers.PacmanController;
import pacman.game.Constants.MOVE;
import mspacman.MsPacMan;
import pacman.game.Game;

/*
 * The Class NearestPillPacMan.
 */
public class MsPacManFSM extends PacmanController {

	FSM fsm;
	MsPacMan msPacManNotEdible;
	MsPacMan msPacManEdible;
	
	public MsPacManFSM() {
		fsm = new FSM("MsPacMan");
    	    	
		Socket socketNotEdible = null;
		Socket socketEdible = null;
		try {
			socketNotEdible = new Socket("localhost",38514);
			socketEdible = new Socket("localhost",38515);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
    	msPacManNotEdible = new MsPacMan(socketNotEdible, "MsPacManNotEdible", 1);
    	msPacManEdible = new MsPacMan(socketEdible, "MsPacManEdible", 2);
		
    	SimpleState edibleGhostState = new SimpleState("edibleGhostState", msPacManEdible);
    	SimpleState notEdibleGhostState = new SimpleState("notEdibleGhostState", msPacManNotEdible);
    	
     	fsm.add(edibleGhostState, new FleeTransition("toNotEdibleGhost"), notEdibleGhostState);
    	fsm.add(notEdibleGhostState, new ChaseTransition("toEdibleGhost"), edibleGhostState);
    	
    	fsm.ready(notEdibleGhostState);
	}
	
	public void gameOver(Game game) {
		msPacManNotEdible.gameOver(game);
		msPacManEdible.gameOver(game);
	}
	
	public void getOk() {
		msPacManNotEdible.getOk();
		msPacManEdible.getOk();
	}
	
	public void preCompute(String opponent) {
		fsm.reset();
    }
	
    @Override
    public MOVE getMove(Game game, long timeDue) {
       	Input in = new MsPacManInput(game); 
       	
       	MOVE m=fsm.run(in);
       	
    	return m;
    }

	public void init(Game game) {
		msPacManNotEdible.init(game);
		msPacManEdible.init(game);
	}
}