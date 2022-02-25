package engine.pacman.controllers;

import engine.pacman.game.Game;
import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;

/**
 * Created by Piers on 11/11/2015.
 */

public abstract class IndividualGhostController {

    protected final GHOST ghost;
    public IndividualGhostController(GHOST ghost) {
        this.ghost = ghost;
    }

    public abstract MOVE getMove(Game game, long timeDue);
}
