package FSM;

import es.ucm.fdi.ici.Input;

/**
 * Represents a transition in the FSM.
 * To change to a target state, the evaluate() method must return true;
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public interface Transition {
		
	/**
	 * Returns if the transition is applicable according to the input.
	 */
	public boolean evaluate(Input in);
	
}
