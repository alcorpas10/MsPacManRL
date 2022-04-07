package FSM;

import java.io.IOException;
import java.net.Socket;
import engine.es.ucm.fdi.ici.Input;
import engine.pacman.controllers.PacmanController;
import engine.pacman.game.Constants.MOVE;
import engine.pacman.game.Game;
import pacman.MsPacMan;

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
		
    	msPacManNotEdible = new MsPacMan(socketNotEdible, "MsPacManNotEdible");
    	msPacManEdible = new MsPacMan(socketEdible, "MsPacManNotEdible");
		
    	SimpleState edibleGhostState = new SimpleState("edibleGhostState", msPacManEdible);
    	SimpleState notEdibleGhostState = new SimpleState("notEdibleGhostState", msPacManNotEdible);
    	
     	fsm.add(edibleGhostState, new FleeTransition("toNotEdibleGhost"), notEdibleGhostState);
    	fsm.add(notEdibleGhostState, new ChaseTransition("toEdibleGhost"), edibleGhostState);
    	
    	fsm.ready(notEdibleGhostState);
	}
	
	public void gameOver() {
		msPacManNotEdible.gameOver();
		msPacManEdible.gameOver();
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
}