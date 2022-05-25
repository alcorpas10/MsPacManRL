package es.ucm.fdi.ici.fsm;

import es.ucm.fdi.ici.Input;

/**
 * Transiton to flee from the ghosts.
 */
public class FleeTransition implements Transition {
	String id;
	
	public  FleeTransition(String name) {
		id = name;
	}
	
	@Override
	public boolean evaluate(Input in) {
		MsPacManInput input = (MsPacManInput) in;
		return input.flee();
	}
	
	public String toString() {
		return id + " Flee";
	}
}
