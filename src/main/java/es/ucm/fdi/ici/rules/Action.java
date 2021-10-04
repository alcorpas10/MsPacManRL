package es.ucm.fdi.ici.rules;

import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * An action to be executed once asserted in the Rule Engine.
 * The asserted fact must comply the following syntax:
 * (ACTION (id action) [(slot ...) (slot ...) ...])
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public interface Action {
		
	public static final String FACT_NAME = "ACTION";
	public static final String ID_SLOT = "id";	
	
	/**
	 * Executes the action according to the game and returns the following move.
	 */
	public MOVE execute(Game game);
}
