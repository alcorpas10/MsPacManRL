package es.ucm.fdi.ici.fsm;

import java.util.HashMap;
import java.util.Vector;

import es.ucm.fdi.ici.Input;
import pacman.game.Constants.MOVE;

/**
 * Implements a generic Finite State Machine. 
 * Extends FSMObservable to allow event notification to registered observers.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public class FSM extends FSMObservable {

	private static final String VERSION = "1.1.2 Hierarchical FSM ready (minor bug solved)";
	State current;
	State initial;
    HashMap<State,Vector<StateTransition>> stateMachine = new HashMap<State,Vector<StateTransition>>();
	String id;
	
	public FSM(String id)
	{
		this.id = id;
    	System.err.println("Finite State Machine Implementation - Ingeniería de Comportamientos Inteligentes. Version "+FSM.VERSION);
	}
    
	/**
	 * Adds a new transition from a source state to a target state
	 */
	public void add(State source, Transition transition, State target) {
		Vector<StateTransition> outTrans = stateMachine.get(source);
		if(outTrans == null)
		{
			outTrans = new Vector<StateTransition>();
			stateMachine.put(source, outTrans);
		}
		outTrans.add(new StateTransition(transition,target));
		this.notifyFSMadd(source.toString(), transition.toString(), target.toString());
    }
	
	/**
	 * Indicates that the FSM has been built and is ready for execution
	 * @param initial is the initial state of the FSM
	 */
	public void ready(State initial)
	{
		this.initial = this.current = initial;
		this.notifyFSMready(initial.toString());
	}
		
	/**
	 * Executes a tick on the FSM.
	 * Evaluates any out transition of the current state according to the input.
	 * If a transition is applicable, it changes to the target state and executes it.
	 * If no transitions are available or applicable, executes current state. 
	 */
	public MOVE run(Input in)
	{
		 Vector<StateTransition> outTrans = stateMachine.get(this.current);
		 if(outTrans == null)
			 return this.current.execute(in);
		 for(StateTransition st : outTrans)
		 {
			 Transition transition = st.getTransition();
			 if(transition.evaluate(in))
			 {
				 State target = st.getTarget();
				 this.notifyFSMtransition(this.current.toString(), transition.toString(), target.toString());
				 this.current.stop();
				 this.current =  target;
				 return target.execute(in);
			 }
		 }
		 return this.current.execute(in);
	}
	
	
	
	public State getInitialState() {
		return initial;
	}

	/**
	 * Resets the FSM to the initial state. 
	 * Should be called from the precompute() method of the controller to reset the FSM for each evaluation cycle.  
	 */
	public void reset() {
		this.current = initial;
		this.notifyFSMreset(initial.toString());	
	}

	@Override
	public String toString() {
		return id;
	}

	/**
	 * Internal class to represent <transition, target> pairs.
	 */
	private class StateTransition{
		Transition transition;
		State target;
		public StateTransition(Transition transition, State target) {
			super();
			this.transition = transition;
			this.target = target;
		}
		public Transition getTransition() {
			return transition;
		}
		public State getTarget() {
			return target;
		}
		
	}

}
