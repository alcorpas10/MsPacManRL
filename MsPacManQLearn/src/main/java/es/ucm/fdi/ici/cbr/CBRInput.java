package es.ucm.fdi.ici.cbr;

import es.ucm.fdi.gaia.jcolibri.cbrcore.CBRQuery;
import es.ucm.fdi.ici.Input;
import pacman.game.Game;

/**
 * Interface to obtain the input variables required to evaluate the fuzzy over the game.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public abstract class CBRInput extends Input{
	
	public CBRInput(Game game)
	{
		super(game);
	}

	public abstract CBRQuery getQuery(); 
}
