package FSM;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import PacMans.QPacMan;
import PacMans.QPacManChase;
import PacMans.QPacManFlee;
import PacMans.QPacManOriginal;
import PacMans.QPacManPills;
import Utils.QConstants;
import chen0040.rl.learning.qlearn.QLearner;
import es.ucm.fdi.ici.Input;
import pacman.controllers.PacmanController;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * The Class NearestPillPacMan.
 */
public class MsPacMan extends PacmanController {

	FSM fsm;
	QPacMan qPacManPills;
	QPacMan qPacManChase;
	QPacMan qPacManOriginal;
	QPacMan qPacManFlee;
	
	public MsPacMan(Game game) {
		setName("MsPacMan 05");
		fsm = new FSM("MsPacMan");
		
		GraphFSMObserver observer = new GraphFSMObserver(fsm.toString());
    	fsm.addObserver(observer);
    	
    	
    	qPacManPills = new QPacManPills(new QLearner(33, 4));
    	qPacManChase = new QPacManChase(new QLearner(33, 4));
    	qPacManOriginal = new QPacManOriginal(new QLearner(13333, 4));
    	qPacManFlee = new QPacManFlee(new QLearner(13333, 4));
    	
    	qPacManOriginal.setNewGame(game);
		qPacManFlee.setNewGame(game);
		qPacManPills.setNewGame(game);
		qPacManChase.setNewGame(game);
		
    	SimpleState pillsState = new SimpleState("pillsState",qPacManPills );
    	SimpleState chaseGhostState = new SimpleState("chaseGhostState", qPacManChase );
    	SimpleState fleeGhostState = new SimpleState("fleeGhostState", qPacManFlee);
    	SimpleState generalState = new SimpleState("generalState",qPacManOriginal );
    	
    	
       	fsm.add(chaseGhostState, new PillTransition("chase"), pillsState );
     	fsm.add(chaseGhostState, new FleeTransition("chase"), fleeGhostState );
     	fsm.add(chaseGhostState, new GeneralTransition("chase"), generalState );
     	
    	fsm.add(fleeGhostState, new PillTransition("flee"), pillsState);
    	fsm.add(fleeGhostState, new ChaseTransition("flee"), chaseGhostState);
    	fsm.add(fleeGhostState, new GeneralTransition("flee"), generalState);
    	
    	fsm.add(pillsState,  new ChaseTransition("pills"), chaseGhostState);
    	fsm.add(pillsState,  new FleeTransition("pills"), fleeGhostState);
    	fsm.add(pillsState,  new GeneralTransition("pills"), generalState);
    	
    	fsm.add(generalState,  new ChaseTransition("general"), chaseGhostState);
    	fsm.add(generalState,  new FleeTransition("general"), fleeGhostState);
    	fsm.add(generalState,  new PillTransition("general"), pillsState);
    	fsm.ready(generalState);
    	
    	
    	JFrame frame = new JFrame();
    	JPanel main = new JPanel();
    	main.setLayout(new BorderLayout());
    	main.add(observer.getAsPanel(true, null), BorderLayout.CENTER);
    	//main.add(c1observer.getAsPanel(true, null), BorderLayout.SOUTH);
    	frame.getContentPane().add(main);
    	frame.pack();
    	frame.setVisible(true);
    	
	}
	
	
	public void preCompute(String opponent) {
    		fsm.reset();
    }
	
	
	
    /* (non-Javadoc)
     * @see pacman.controllers.Controller#getMove(pacman.game.Game, long)
     */
    @Override
    public MOVE getMove(Game game, long timeDue) {
       	Input in = new MsPacManInput(game); 
       	
       	MOVE m=fsm.run(in);
       	
       	qPacManOriginal.updateStrategy();  
       	qPacManFlee.updateStrategy();  
       	qPacManChase.updateStrategy();  
       	qPacManPills.updateStrategy();  
       	
    	return m;
    }
    
    
}