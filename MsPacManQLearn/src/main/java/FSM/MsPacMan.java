package FSM;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;

import chen0040.rl.learning.qlearn.QLearner;
import es.ucm.fdi.ici.Input;
import mspacmans.QPacMan;
import mspacmans.QPacManChase;
import mspacmans.QPacManFlee;
import mspacmans.QPacManOriginal;
import mspacmans.QPacManPills;
import pacman.controllers.PacmanController;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Class that implements the FSM. 
 * It has 4 mspacman with different behaviors that change if its necessary using the transitions.
 */
public class MsPacMan extends PacmanController {

	FSM fsm;
	QPacMan qPacManFlee;	//MsPacman that has trained to flee from ghosts
	QPacMan qPacManPills;	//MsPacman that has trained to eat pills
	QPacMan qPacManChase;  	//MsPacman that has trained to chase ghosts
	
	public MsPacMan(Game game, int numTrainings, String ghostType) {
		fsm = new FSM("MsPacMan");
		
		/*GraphFSMObserver observer = new GraphFSMObserver(fsm.toString()); //Code for visual fsm representation
    	fsm.addObserver(observer);*/
    	
    	//Load from json the flee model
    	StringBuilder contentBuilder1 = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("FleeModel" + ghostType + numTrainings+".json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder1.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
        //Load from json the pills model
        StringBuilder contentBuilder2 = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("PillsModel" + ghostType + numTrainings+".json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder2.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        
        //Load from json the chase model
        StringBuilder contentBuilder3 = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader("ChaseModel" + ghostType + numTrainings+".json"))) {
            String sCurrentLine;
            while ((sCurrentLine = br.readLine()) != null) {
                contentBuilder3.append(sCurrentLine).append("\n");
            }
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    	
        //Load the qlearners with theri respective model  
    	qPacManFlee = new QPacManFlee(QLearner.fromJson(contentBuilder1.toString()));
    	qPacManPills = new QPacManPills(QLearner.fromJson(contentBuilder2.toString()));
    	qPacManChase = new QPacManChase(QLearner.fromJson(contentBuilder3.toString()));
    	
    	
    	//Initialize the games in all the mspacmans
		qPacManFlee.setNewGame(game);
		qPacManPills.setNewGame(game);
		qPacManChase.setNewGame(game);
		
		
		//States of the fsm
    	SimpleState pillsState = new SimpleState("pillsState", qPacManFlee);
    	SimpleState chaseGhostState = new SimpleState("chaseGhostState", qPacManPills);
    	SimpleState fleeGhostState = new SimpleState("fleeGhostState", qPacManChase);
    	
    	
    	//Add the states with their respective transitions to the other states
    	
    	fsm.add(pillsState, new ChaseTransition("pills"), chaseGhostState);
    	fsm.add(pillsState, new FleeTransition("pills"), fleeGhostState);
    	
    	fsm.add(chaseGhostState, new PillTransition("chase"), pillsState );
     	fsm.add(chaseGhostState, new FleeTransition("chase"), fleeGhostState );
     	  	
    	fsm.add(fleeGhostState, new PillTransition("flee"), pillsState);
    	fsm.add(fleeGhostState, new ChaseTransition("flee"), chaseGhostState);
    	
    	fsm.ready(pillsState);
    	
    	
    	//Show the visual fsm representation 
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
	
	
	
    
	/**
	 * Gets the move made from the fsm
	 */
    @Override
    public MOVE getMove(Game game, long timeDue) {
       	Input in = new MsPacManInput(game); 
       	
       	MOVE m=fsm.run(in);  //gets the move
       	
       	//updates the stategy  
       	qPacManFlee.updateStrategy();  
       	qPacManChase.updateStrategy();  
       	qPacManPills.updateStrategy();  
       	
    	return m;
    }
    
    
}