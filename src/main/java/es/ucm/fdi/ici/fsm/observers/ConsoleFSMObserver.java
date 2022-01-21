package es.ucm.fdi.ici.fsm.observers;

import es.ucm.fdi.ici.fsm.FSMObserver;

/**
 * Simple console observer to System.out for debugging.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public class ConsoleFSMObserver implements FSMObserver {

	String fsmId;
	public ConsoleFSMObserver(String fsmId) {
		this.fsmId = fsmId;
	}

	@Override
	public void fsmTransition(String sourceState, String transition, String targetState) {
		System.out.printf("[%s] %s >> %s >> %s\n", fsmId, sourceState, transition, targetState);

	}

	@Override
	public void fsmAdd(String sourceState, String transition, String targetState) {
		System.out.printf("Nodes added for [%s] %s >> %s >> %s\n", fsmId, sourceState, transition, targetState);
	}

	@Override
	public void fsmReady(String initialState) {
		System.out.printf("FSM ready. Initial State: %s\n", initialState);
		
	}

	@Override
	public void fsmReset(String initialState) {
		System.out.printf("FSM reset to initial State: %s\n", initialState);
		
	}
	
}
