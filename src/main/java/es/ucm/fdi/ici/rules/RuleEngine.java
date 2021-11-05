package es.ucm.fdi.ici.rules;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/*import jess.Fact;
import jess.JessException;
import jess.Rete;*/
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class RuleEngine extends RuleEngineObservable{

	//Rete jess;
	HashMap<String,RulesAction> map;
	String engineName;
	
	public RuleEngine(String engineName, String rulesFile, HashMap<String,RulesAction> map) {
		this.engineName = engineName;
		this.map = map;
		/*jess = new Rete();
		 try {
			jess.batch(rulesFile);
			jess.reset();
		} catch (JessException e) {
			System.err.println(engineName+": Exception loading rules from file: "+rulesFile);
			e.printStackTrace();
		}*/
	}
	
	/**
	 * Reset the rule engine. Clears working memory and the agenda, but rules remain defined. Clears all non-globals from the global scope. 
	 * Asserts (initial-fact), reasserts all deffacts and definstances.
	 */
	public void reset() {
		/*try {
			jess.reset();
		} catch (JessException e) {
			System.err.println(engineName+": Exception reseting engine");
			e.printStackTrace();
		}*/
	}
	
	public void assertFacts(Collection<String> facts)
	{
		for(String f: facts)
		{
			/*try {
					jess.assertString(f);
			} catch (JessException e) {
				System.err.println(engineName+" :Exception assserting fact: "+f);
				e.printStackTrace();
			}*/
		}
	}
	
	public MOVE run(Game game)
	{
		/*try {
			jess.run();
			Fact actionFact = findActionFact();
			
			if(actionFact!=null)
			{
				//notify
				Vector<String> facts = new Vector<String>();
				Iterator<?> it = jess.listFacts();
				while(it.hasNext())
					facts.add(((Fact)it.next()).toStringWithParens());
				this.notifyActionFired(actionFact.toStringWithParens(), facts);
				//end-notify
				
				RulesAction gameAction = map.get(actionFact.getSlotValue(RulesAction.ID_SLOT).toString());
				gameAction.parseFact(actionFact);
				return gameAction.execute(game);
			}
		} catch (JessException e) {
			System.err.println(engineName+" :Exception executing rules");
			e.printStackTrace();
		}*/
		return MOVE.NEUTRAL;
	}

	
	/*private Fact findActionFact() throws JessException
	{
		Iterator<?> it = jess.listFacts();
		while(it.hasNext()){ 
		    Fact dd = (Fact)it.next();
		    String nombre = dd.getName(); 
		    if(nombre.startsWith("MAIN::"+RulesAction.FACT_NAME))
		    	return dd;
		}
		return null;
	}*/
}
