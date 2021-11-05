package es.ucm.fdi.ici.fuzzy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;

import es.ucm.fdi.ici.Action;
/*import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.rule.Rule;
import net.sourceforge.jFuzzyLogic.rule.Variable;*/
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * FuzzyEngine.
 * This class wraps and simplifies jFuzzyLogic library  
 * @author Juan A. Recio
 *
 */
public class FuzzyEngine extends FuzzyEngineObservable{

	//FIS fis;
	String fuzzyBlock;
	ActionSelector actionSelector;
	
	/**
	 * Constructor
	 * @param engineName is the name of the engine
	 * @param fclFile path to the fcl file. Be aware of include the complete path with src/, bin/ etc.
	 * @param fuzzyBlock to be loaded
	 * @param actionSelector is the class that selects the Action to be executed according to the output of the fuzzy engine
	 */
	public FuzzyEngine(String engineName, String fclFile, String fuzzyBlock, ActionSelector actionSelector)
	{
		super();
		this.fuzzyBlock = fuzzyBlock;
		this.actionSelector = actionSelector;
		
		/*fis = FIS.load(fclFile, false);

		if (fis == null) {
			System.err.println("Can't load file: '" + fclFile + "'");
		}*/
	}
	
	/**
	 * Evaluates a fuzzy function given the input variables and fills the output map with the output variables (defuzzified)
	 * @param input is a map containing the input values. These values will be fuzzified according to the FUZZIFY blocks
	 */
	public MOVE run(HashMap<String,Double> input, Game game)
	{
		MOVE move = MOVE.NEUTRAL;
		try {
			HashMap<String,Double> output = new HashMap<String,Double>();
			/*FunctionBlock fb = fis.getFunctionBlock(fuzzyBlock);
			// Set inputs
			for(String varName: input.keySet())
				fb.setVariable(varName, input.get(varName));
	
			// Evaluate
			fb.evaluate();
			
			
			// Get Output
			HashMap<String,Variable> variables = fb.getVariables();
			for(String varName: variables.keySet())
			{
				Variable var = variables.get(varName);
				if(var.isOutput())
					output.put(varName, var.defuzzify());
			}*/
			Action action = this.actionSelector.selectAction(output);
			
			try {
				move = action.execute(game);
				this.notifyActionFired(action.toString(), input, output);
			} catch (Exception e) {
				System.err.println("Error executing action "+ action.getClass().getCanonicalName().toString());
				//e.printStackTrace();
			}
		
		} catch (Exception e1) {
			System.err.println("Error evaluating fuzzy logic");
			e1.printStackTrace();
		}
		return move;
	}
	
	/**
	 * Shows the degree of support of each rule. Used only for debugging purposes. 
	 * @param function the function containing the rules.
	 * @param block the rules blocks to debug.
	 */
	public Collection<String> debugRules(String rules)
	{
		Vector<String> debugRules = new Vector<String>();
		/*for( Rule r : fis.getFunctionBlock(fuzzyBlock).getFuzzyRuleBlock(rules).getRules() )
		      debugRules.add(r.toString());*/
		return debugRules;
	}
	
	
}
