package es.ucm.fdi.ici.fuzzy;

import java.util.Collection;
import java.util.HashMap;

/**
 * An observer interface for classes that want to receive the events of the fuzzy rule engine.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public interface FuzzyEngineObserver {

	String getObservedRules();
	void actionFired(String action, HashMap<String,Double> input, HashMap<String,Double> output, Collection<String> rules);
}
