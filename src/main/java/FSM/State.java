package FSM;

import es.ucm.fdi.ici.Input;
import pacman.game.Constants.MOVE;

/**
 * Interface for FSM states. 
 * Each state is responsible of executing an action over the Game object (inside Input) and according to other input variables.
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 *
 */
public interface State {

	/**
	 * Executes the corresponding action according to the input variables, and over the Game object also contained in the Input parameter.
	 */
	public MOVE execute(Input input);

	
	/**
	 * Indicates that the state is not longer active
	 */
	public void stop();
}