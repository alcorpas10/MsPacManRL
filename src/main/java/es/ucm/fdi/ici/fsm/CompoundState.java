package es.ucm.fdi.ici.fsm;

import es.ucm.fdi.ici.Input;
import pacman.game.Constants.MOVE;

/**
 * Compound state that stores internally another FSM.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public class CompoundState implements State {
	
	private FSM stateMachine;
	public String id;
	public State initialState;

	public CompoundState(String id, FSM stateMachine) {
		super();
		this.stateMachine = stateMachine;
		this.id = id;
		this.initialState = stateMachine.getInitialState();
	}
	
	/* (non-Javadoc)
	 * @see es.ucm.fdi.ici.fsm.State#execute(es.ucm.fdi.ici.fsm.Input)
	 */
	@Override
	public MOVE execute(Input input){
		return this.stateMachine.run(input);
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public void stop() {
		this.stateMachine.reset();
		
	}
	

}