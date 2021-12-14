package FSM;

import es.ucm.fdi.ici.Input;

public class FleeTransition implements Transition {
	String id;
	public  FleeTransition(String nombre) {
		// TODO Auto-generated constructor stub
		super();
		id=nombre;
	}
	
	@Override
	public boolean evaluate(Input in) {
		// TODO Auto-generated method stub
		MsPacManInput input = (MsPacManInput)in;
		return input.flee();
	}
	public String toString() {
		return id + " Flee";
	}
}
