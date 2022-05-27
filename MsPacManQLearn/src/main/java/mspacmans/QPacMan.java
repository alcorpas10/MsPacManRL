package mspacmans;

import es.ucm.fdi.ici.Action;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Abstract class that implements the behaviour of mspacman with qlearn 
 */
public abstract class QPacMan implements Action {

	@Override
	public String getActionId() {
		return "QPacMan";
	}

	@Override
	public abstract MOVE execute(Game game) ;

	public abstract void setNewGame(Game game);

	public abstract void updateStrategy();

	public abstract MOVE act();
	
}
