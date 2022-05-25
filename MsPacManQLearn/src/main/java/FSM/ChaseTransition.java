package FSM;

import es.ucm.fdi.ici.Input;

/**
 * Transiton to chase the ghosts.
 */
public class ChaseTransition implements Transition {
	String id;
	
	public ChaseTransition(String name) {
		id = name;
	}
	
	@Override
	public boolean evaluate(Input in) {
		MsPacManInput input = (MsPacManInput) in;
		return input.chase();
	}
	
	public String toString() {
		return id + " Chase ";
	}
}
