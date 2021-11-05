package es.ucm.fdi.ici.fuzzy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

/**
 * Implements the methods required to notify RuleEngine events. Thus, RuleEngine class will inherit this one.
 * Allow to register RuleEngineObservers and notify the method defined in that interface. 
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public abstract class FuzzyEngineObservable {

	Vector<FuzzyEngineObserver> observers = new Vector<FuzzyEngineObserver>();
	public FuzzyEngineObservable() {
	}

	/**
	 * Adds a new observer
	 */
	public void addObserver(FuzzyEngineObserver observer) {
		this.observers.add(observer);
	}
	
	public abstract Collection<String> debugRules(String rules);
	
	/**
	 * Notifies that an action has been fired
	 */
	public void notifyActionFired(String action, HashMap<String,Double> input, HashMap<String,Double> output) {
		for(FuzzyEngineObserver o: observers)
		{
			Collection<String> rules = null;
			String observedRules = o.getObservedRules();
			if(observedRules!=null)
				rules = debugRules(observedRules);
			o.actionFired(action, input,output, rules);
		}
	}
	
}
