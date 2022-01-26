package FSM;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import PacMans.QPacMan;
import PacMans.QPacManChase;
import PacMans.QPacManFlee;
import PacMans.QPacManOriginal;
import PacMans.QPacManPills;
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
	QPacMan qPacManGeneral;
	QPacMan qPacManFlee;
	QPacMan qPacManPills;
	QPacMan qPacManChase;
	
	public MsPacMan(Game game, int numTrainings) {
		fsm = new FSM("MsPacMan");
		
		/*GraphFSMObserver observer = new GraphFSMObserver(fsm.toString());
    	fsm.addObserver(observer);*/
    	
    	/*qPacManGeneral = new QPacManOriginal(new QLearner(13333, 4));
    	qPacManFlee = new QPacManFlee(new QLearner(13333, 4));
    	qPacManPills = new QPacManPills(new QLearner(33, 4));
    	qPacManChase = new QPacManChase(new QLearner(33, 4));*/
    	
    	StringBuilder contentBuilder1 = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("modelFlee"+numTrainings+".json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder1.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder contentBuilder2 = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("modelPills"+numTrainings+".json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder2.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        StringBuilder contentBuilder3 = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("modelChase"+numTrainings+".json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder3.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    	
    	qPacManGeneral = new QPacManOriginal(new QLearner(13333, 4));
    	qPacManFlee = new QPacManFlee(QLearner.fromJson(contentBuilder1.toString()));
    	qPacManPills = new QPacManPills(QLearner.fromJson(contentBuilder2.toString()));
    	qPacManChase = new QPacManChase(QLearner.fromJson(contentBuilder3.toString()));
    	
    	qPacManGeneral.setNewGame(game);
		qPacManFlee.setNewGame(game);
		qPacManPills.setNewGame(game);
		qPacManChase.setNewGame(game);
		
    	SimpleState generalState = new SimpleState("generalState", qPacManGeneral);
    	SimpleState pillsState = new SimpleState("pillsState", qPacManFlee);
    	SimpleState chaseGhostState = new SimpleState("chaseGhostState", qPacManPills);
    	SimpleState fleeGhostState = new SimpleState("fleeGhostState", qPacManChase);
    	
    	fsm.add(generalState, new ChaseTransition("general"), chaseGhostState);
    	fsm.add(generalState, new FleeTransition("general"), fleeGhostState);
    	fsm.add(generalState, new PillTransition("general"), pillsState);
    	
    	fsm.add(pillsState, new ChaseTransition("pills"), chaseGhostState);
    	fsm.add(pillsState, new FleeTransition("pills"), fleeGhostState);
    	fsm.add(pillsState, new GeneralTransition("pills"), generalState);
    	
    	fsm.add(chaseGhostState, new PillTransition("chase"), pillsState );
     	fsm.add(chaseGhostState, new FleeTransition("chase"), fleeGhostState );
     	fsm.add(chaseGhostState, new GeneralTransition("chase"), generalState );
     	
    	fsm.add(fleeGhostState, new GeneralTransition("flee"), generalState);    	
    	fsm.add(fleeGhostState, new PillTransition("flee"), pillsState);
    	fsm.add(fleeGhostState, new ChaseTransition("flee"), chaseGhostState);
    	
    	fsm.ready(generalState);
    	
    	
    	/*JFrame frame = new JFrame();
    	JPanel main = new JPanel();
    	main.setLayout(new BorderLayout());
    	main.add(observer.getAsPanel(true, null), BorderLayout.CENTER);
    	frame.getContentPane().add(main);
    	frame.pack();
    	frame.setVisible(true);*/
    	
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
       	
       	qPacManGeneral.updateStrategy();  
       	qPacManFlee.updateStrategy();  
       	qPacManChase.updateStrategy();  
       	qPacManPills.updateStrategy();  
       	
    	return m;
    }
    
    
}