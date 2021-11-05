package es.ucm.fdi.ici.rules;

import java.util.Collection;
import java.util.Vector;

/**
 * Implements the methods required to notify RuleEngine events. Thus, RuleEngine class will inherit this one.
 * Allow to register RuleEngineObservers and notify the method defined in that interface. 
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public class RuleEngineObservable {

	Vector<RuleEngineObserver> observers = new Vector<RuleEngineObserver>();
	public RuleEngineObservable() {
	}

	/**
	 * Adds a new observer
	 */
	public void addObserver(RuleEngineObserver observer) {
		this.observers.add(observer);
	}
	
	/**
	 * Notifies a FSM transition
	 */
	public void notifyReset() {
		for(RuleEngineObserver o: observers)
			o.ruleEngineReset();
	}
	
	/**
	 * Notifies the addition of a new transition to the FSM
	 */
	public void notifyActionFired(String action, Collection<String> allActions) {
		for(RuleEngineObserver o: observers)
			o.actionFired(action, allActions);
	}
	
}
