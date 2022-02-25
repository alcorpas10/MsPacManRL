package pacman.controllers;

import java.util.EnumMap;

import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Clase para gestionar los movimientos de los fantasmas con visibilidad limitada (PO).
 * Con PO cada fantasma recibe una copia del game donde solo puede acceder a los datos 
 * y posiciones que ve en cada tick de la simulación.
 * Por ello, no puede utilizarse la misma copia del objeto game para calcular los 
 * movimientos de todos los fantasmas, tal y como se hace en GhostController.
 * 
 * Esta clase obliga a calcular los movimientos de los fantasmas de forma individual a través del método abstracto getMove().
 * Este método se llama por cada fantasma, con la copia del game correspondiente a su visibilidad. 
 * La clase también permite hacer calculos globales para los fantasmas antes y despúes de solicitar los movimientos individuales a
 * través de los métodos preCompute() y postCompute(). Sin embargo, hay que tener presente que los accesos a gran parte de la información
 * generarán una excepción.
 * 
 * @author Juan A. Recio-Garcia
 *
 */
public abstract class POGhostController extends GhostController {

    @Override
    public final EnumMap<GHOST, MOVE> getMove(Game game, long timeDue) {
    	try {
    	preCompute(game);
    	} catch(Exception e)
    	{
    		System.err.println("Warning. Error precomputing ghosts global movements in PO mode.");
    	}
    	
    	EnumMap<GHOST, MOVE> myMoves = new EnumMap<GHOST, MOVE>(GHOST.class);
        for (GHOST ghost : GHOST.values()) {
            
        	myMoves.put(
                    ghost,
                    getMove(ghost, game.copy(ghost), timeDue));
        }

    	try {
    	postCompute(game);
    	} catch(Exception e)
    	{
    		System.err.println("Warning. Error postcomputing ghosts global movements in PO mode.");
    	}
        
        
        return myMoves;
    }
    
    /**
     * Método para realizar precálculos antes de solicitar los movimientos individuales de los fantasmas. 
     * Cuidado con los acceso a la información no visible del game.
     * @param game es la copia del game donde no se puede acceder a la información de fantasmas ni de pacman.
     */
    public void preCompute(Game game) {
    	//Does nothing by default
    }
    
    /**
     * Método para realizar postcálculos despúes de solicitar los movimientos individuales de los fantasmas. 
     * Cuidado con los acceso a la información no visible del game.
     * @param game es la copia del game donde no se puede acceder a la información de fantasmas ni de pacman.
     */
    public void postCompute(Game game) {
    	//Does nothing by default
    } 
    
    /**
     * Método para obtener el movimiento de cada fantasma. Se recibe un game con la visibilidad parcial de dicho fantasma.
     * @param ghost es el fantasma para el que se solicita el movimiento
     * @param game contiene copia del juego con la visibilidad del fantasma
     * @param timeDue tiempo restante para realizar el cálculo.
     * @return el movimiento del fantasma.
     */
    abstract public MOVE getMove(GHOST ghost, Game game, long timeDue);

}
