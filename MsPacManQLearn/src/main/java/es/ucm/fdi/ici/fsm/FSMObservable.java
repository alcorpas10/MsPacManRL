package es.ucm.fdi.ici.fsm;

import java.util.Vector;

/**
 * Implements the methods required to notify FSM events. Thus, FSM class will inherit this one.
 * Allow to register FSMObservers and notify the method defined in that interface. 
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public class FSMObservable {

	Vector<FSMObserver> observers = new Vector<FSMObserver>();
	public FSMObservable() {
	}

	/**
	 * Adds a new observer
	 */
	public void addObserver(FSMObserver observer) {
		this.observers.add(observer);
	}
	
	/**
	 * Notifies a FSM transition
	 */
	public void notifyFSMtransition(String sourceState, String transition, String targetState) {
		for(FSMObserver o: observers)
			o.fsmTransition(sourceState, transition, targetState);
	}
	
	/**
	 * Notifies the addition of a new transition to the FSM
	 */
	public void notifyFSMadd(String sourceState, String transition, String targetState) {
		for(FSMObserver o: observers)
			o.fsmAdd(sourceState, transition, targetState);
	}
	
	/**
	 * Notifies that the FSM is ready to be executed.
	 * @param initialState
	 */
	public void notifyFSMready(String initialState) {
		for(FSMObserver o: observers)
			o.fsmReady(initialState);
	}
	
	/**
	 * Notifies that the FSM is ready to be executed.
	 * @param initialState
	 */
	public void notifyFSMreset(String initialState) {
		for(FSMObserver o: observers)
			o.fsmReset(initialState);
	}
	
}
