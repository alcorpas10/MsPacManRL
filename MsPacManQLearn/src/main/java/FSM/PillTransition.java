package FSM;

import es.ucm.fdi.ici.Input;
/**
 * Transiton to only pills.
 */
public class PillTransition implements Transition {
	String id;
	
	public PillTransition(String name){
		id = name;
	}
	
	@Override
	public boolean evaluate(Input in) {
		MsPacManInput input = (MsPacManInput) in;
		return input.pills();
	}
	
	public String toString() {
		return id + " Pills";
	}
}
