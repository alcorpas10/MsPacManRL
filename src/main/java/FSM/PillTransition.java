package FSM;

import es.ucm.fdi.ici.Input;

public class PillTransition implements Transition {
	
	String id;
	public PillTransition(String nombre){
		super();
		id=nombre;
	}
	@Override
	public boolean evaluate(Input in) {
		// TODO Auto-generated method stub
		MsPacManInput input = (MsPacManInput)in;
		return input.pills();
	}
	public String toString() {
		return id + " Pills";
	}

}
