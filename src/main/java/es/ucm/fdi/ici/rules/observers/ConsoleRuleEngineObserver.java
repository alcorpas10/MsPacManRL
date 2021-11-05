package es.ucm.fdi.ici.rules.observers;

import java.util.Collection;

import es.ucm.fdi.ici.rules.RuleEngineObserver;

public class ConsoleRuleEngineObserver implements RuleEngineObserver {

	String ruleEngineId;
	boolean showAll;
	public ConsoleRuleEngineObserver(String id, boolean showAllRules) {
		this.ruleEngineId = id;
		this.showAll = showAllRules;
	}

	@Override
	public void ruleEngineReset() {
		System.out.println(String.format("[%s] rule engine reset", this.ruleEngineId));

	}

	@Override
	public void actionFired(String action, Collection<String> allActions) {
		if(showAll)
		{
			System.out.println("***** Rules for "+ruleEngineId+" *****");
			for(String s: allActions)
				System.out.println(s);
		}
		System.out.println(String.format("[%s] action fired: %s", this.ruleEngineId, action));

	}

}
