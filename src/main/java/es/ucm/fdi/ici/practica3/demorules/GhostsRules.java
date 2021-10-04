package es.ucm.fdi.ici.practica3.demorules;

import java.util.EnumMap;
import java.util.HashMap;

import es.ucm.fdi.ici.practica3.demorules.ghosts.GhostsInput;
import es.ucm.fdi.ici.practica3.demorules.ghosts.actions.ChaseAction;
import es.ucm.fdi.ici.practica3.demorules.ghosts.actions.RunAwayAction;
import es.ucm.fdi.ici.rules.Action;
import es.ucm.fdi.ici.rules.Input;
import pacman.controllers.GhostController;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

public class GhostsRules  extends GhostController  {

	private static final String RULES_PATH = "es/ucm/fdi/ici/practica3/demorules/";
	
	HashMap<String,Action> map;
	
	//EnumMap<GHOST,RuleEngine> ghostRuleEngines;
	
	
	public GhostsRules() {
		
		map = new HashMap<String,Action>();
		//Fill Actions
		Action BLINKYchases = new ChaseAction(GHOST.BLINKY);
		Action INKYchases = new ChaseAction(GHOST.INKY);
		Action PINKYchases = new ChaseAction(GHOST.PINKY);
		Action SUEchases = new ChaseAction(GHOST.SUE);
		Action BLINKYrunsAway = new RunAwayAction(GHOST.BLINKY);
		Action INKYrunsAway = new RunAwayAction(GHOST.INKY);
		Action PINKYrunsAway = new RunAwayAction(GHOST.PINKY);
		Action SUErunsAway = new RunAwayAction(GHOST.SUE);
		
		map.put("BLINKYchases", BLINKYchases);
		map.put("INKYchases", INKYchases);
		map.put("PINKYchases", PINKYchases);
		map.put("SUEchases", SUEchases);	
		map.put("BLINKYrunsAway", BLINKYrunsAway);
		map.put("INKYrunsAway", INKYrunsAway);
		map.put("PINKYrunsAway", PINKYrunsAway);
		map.put("SUErunsAway", SUErunsAway);
		
		/*ghostRuleEngines = new EnumMap<GHOST,RuleEngine>(GHOST.class);
		for(GHOST ghost: GHOST.values())
		{
			String rulesFile = String.format("%s/%srules.clp", RULES_PATH, ghost.name().toLowerCase());
			RuleEngine engine  = new RuleEngine(ghost.name(),rulesFile, map);
			ghostRuleEngines.put(ghost, engine);
			
			//add observer to every Ghost
			//ConsoleRuleEngineObserver observer = new ConsoleRuleEngineObserver(ghost.name(), true);
			//engine.addObserver(observer);
		}
		
		//add observer only to BLINKY
		ConsoleRuleEngineObserver observer = new ConsoleRuleEngineObserver(GHOST.BLINKY.name(), true);
		ghostRuleEngines.get(GHOST.BLINKY).addObserver(observer);*/
		
	}

	@Override
	public EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
		
		//Process input
		Input input = new GhostsInput(game);
		
		EnumMap<GHOST,MOVE> result = new EnumMap<GHOST,MOVE>(GHOST.class);		

		
		//load facts
		//reset the rule engines
		/*for(RuleEngine engine: ghostRuleEngines.values()) {
			engine.reset();
			engine.assertFacts(input.getFacts());
		}
		
		EnumMap<GHOST,MOVE> result = new EnumMap<GHOST,MOVE>(GHOST.class);		
		for(GHOST ghost: GHOST.values())
		{
			RuleEngine engine = ghostRuleEngines.get(ghost);
			MOVE move = engine.run(game);
			result.put(ghost, move);
		}*/
		
		return result;
	}

}
