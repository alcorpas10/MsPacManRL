package es.ucm.fdi.ici.fuzzy.observers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import es.ucm.fdi.ici.fuzzy.FuzzyEngineObserver;

/**
 * Default console observer for the fuzzy engine.
 * If the observed rule block is defined, it will show the support details for each fired rule.
 * @author Juan A. Recio García
 *
 */
public class ConsoleFuzzyEngineObserver implements FuzzyEngineObserver {

	String rules;
	String ruleEngineId;
	
	/**
	 * Constructor
	 * @param engineId
	 * @param observedRuleBlock If the observed rule block is defined, it will show the support details for each fired rule.
	 */
	public ConsoleFuzzyEngineObserver(String engineId, String observedRuleBlock)
	{
		this.rules = observedRuleBlock;
		this.ruleEngineId = engineId;
	}
	
	/*
	 * Constructor without observing rules support
	 */
	public ConsoleFuzzyEngineObserver(String engineId) {
		this(engineId,null);
	}
	
	@Override
	public String getObservedRules() {
		return rules;
	}

	@Override
	public void actionFired(String action, HashMap<String, Double> input, HashMap<String, Double> output,
			Collection<String> rulesInfo) {
		System.out.println(String.format("[%s] action fired: %s", this.ruleEngineId, action));
		System.out.println("***INPUT***");
		List<String> list = new ArrayList<String>(input.keySet());
		Collections.sort(list);
		for(String s: list)
			System.out.println(s+" : "+input.get(s));
		System.out.println("***OUTPUT***");
		list = new ArrayList<String>(output.keySet());
		Collections.sort(list);
		for(String s: list)
			System.out.println(s+" : "+output.get(s));
		if(rules!=null)
		{
			System.out.println("***RULES***");
			for(String s: rulesInfo)
				System.out.println(s);
		}
	}

}
