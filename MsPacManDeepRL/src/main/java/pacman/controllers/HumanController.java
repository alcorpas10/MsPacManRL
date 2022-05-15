package pacman.controllers;

import java.awt.event.KeyEvent;

import pacman.game.Game;
import pacman.game.Constants.MOVE;

/*
 * Allows a human player to play the game using the arrow key of the keyboard.
 */
public class HumanController extends PacmanController {
    public KeyBoardInput input;

    public HumanController(KeyBoardInput input) {
        this.input = input;
    }

    public KeyBoardInput getKeyboardInput() {
        return input;
    }

    @Override
    public MOVE getMove(Game game, long dueTime) {
        //        System.out.println("Returned: " + input.getKey());
        switch (input.getKey()) {
            case KeyEvent.VK_UP:
                return MOVE.UP;
            case KeyEvent.VK_RIGHT:
                return MOVE.RIGHT;
            case KeyEvent.VK_DOWN:
                return MOVE.DOWN;
            case KeyEvent.VK_LEFT:
                return MOVE.LEFT;
            default:
                return MOVE.NEUTRAL;
        }
    }
}