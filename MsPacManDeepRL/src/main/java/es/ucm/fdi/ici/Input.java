package es.ucm.fdi.ici;

import pacman.game.Game;

/**
 * Abstract class to encapsulate the input variables required to evaluate the transitions and apply the actions over the game.
 * It follows a template pattern where the constructor calls the parseInput() abstract method. Here, subclasses must extract from the game the required information. 
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public abstract class Input {

	protected Game game;
	
	/**
	 * Receives the game and calls the parseInput() method

	 */
	public Input(Game game)
	{
		this.game = game;
		parseInput();
	}

	public Game getGame() {
		return this.game;
	}
	
	/**
	 * Obtains the required variables from the game and stores them as attributes of the implementing subclasses.
	 */
	public abstract void parseInput();

}
