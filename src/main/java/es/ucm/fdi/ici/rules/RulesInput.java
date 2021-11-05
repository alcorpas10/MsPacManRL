package es.ucm.fdi.ici.rules;

import java.util.Collection;

import es.ucm.fdi.ici.Input;
import pacman.game.Game;

/**
 * Abstract class to encapsulate the input variables required to evaluate the rules over the game.
 * It follows a template pattern where the constructor calls the parseInput() abstract method. Here, subclasses must extract from the game the required information. 
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public abstract class RulesInput extends Input {

	
	/**
	 * Receives the game and calls the parseInput() method

	 */
	public RulesInput(Game game)
	{
		super(game);
	}


	/**
	 * Returns a list of facts in CLIPS syntax to be asserted every game tick.
	 * These facts must be defined by the corresponding deftemplate rules in the clp file loaded into the RuleEngine
	 * @see es.ucm.fdi.ici.rules.RuleEngine
	 */
	public abstract Collection<String> getFacts(); 
}
