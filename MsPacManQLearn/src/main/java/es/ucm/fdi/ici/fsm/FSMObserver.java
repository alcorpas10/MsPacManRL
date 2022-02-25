package es.ucm.fdi.ici.fsm;

/**
 * An observer interface for classes that want to receive the events of the FSM.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public interface FSMObserver {

	/**
	 * A new transition from a source state and to a target state has been created.
	 */
	void fsmAdd(String sourceState, String transition, String targetState);
	
	/**
	 * The FSM is completely configured and ready. 
	 * This method should be called only once, in the controller constructor.
	 * @param initialState of the FSM
	 */
	void fsmReady(String initialState);
	
	/**
	 * Notifies a transition in the FSM  
	 */
	void fsmTransition(String sourceState, String transition, String targetState);
	
	/**
	 * Notifies that the FSM has been reset.
	 * This method should be called to reset the FSM when evaluating performance 
	 * from the precompute() method of the controller.
	 */
	void fsmReset(String initialState);
}
