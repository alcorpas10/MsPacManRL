package FSM;

import es.ucm.fdi.ici.Input;

/**
 * Transition to general behavior of the mspacman.
 */
public class GeneralTransition implements Transition {
	String id;
	
	public GeneralTransition(String nombre) {
		id=nombre;
	}
	
	@Override
	public boolean evaluate(Input in) {
		MsPacManInput input = (MsPacManInput) in;	
		return input.general();
	}
	
	public String toString() {
		return id + " general";
	}
}
