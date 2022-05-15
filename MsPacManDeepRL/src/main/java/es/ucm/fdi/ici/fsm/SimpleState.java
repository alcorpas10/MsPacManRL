package es.ucm.fdi.ici.fsm;

import es.ucm.fdi.ici.Action;
import es.ucm.fdi.ici.Input;
import pacman.game.Constants.MOVE;

/**
 * Simple state implementation. 
 * It receives an action to be executed when the state is activated.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public class SimpleState implements State {
	
	private Action action;
	public String id;

	/**
	 * Constructor. Id is taken from Action
	 * @param action to be executed
	 */
	public SimpleState(Action action) {
		super();
		this.action = action;
		this.id = action.getActionId();
	}
	
	/**
	 * Constructor
	 * @param id of the state
	 * @param action to be executed
	 */
	public SimpleState(String id, Action action) {
		super();
		this.action = action;
		this.id = id;
	}
	
	/* (non-Javadoc)
	 * @see es.ucm.fdi.ici.fsm.State#execute(es.ucm.fdi.ici.fsm.Input)
	 */
	@Override
	public MOVE execute(Input input){
		return this.action.execute(input.getGame());
	}

	@Override
	public String toString() {
		return id;
	}

	@Override
	public void stop() {
		// does nothing
		
	}
	

}