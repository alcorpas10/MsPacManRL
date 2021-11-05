package es.ucm.fdi.ici.rules;

import java.util.Collection;

/**
 * An observer interface for classes that want to receive the events of the rule engine.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public interface RuleEngineObserver {

	void ruleEngineReset();
	
	void actionFired(String action, Collection<String> allActions);
}
