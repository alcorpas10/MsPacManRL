package FSM;

import es.ucm.fdi.ici.Input;

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
