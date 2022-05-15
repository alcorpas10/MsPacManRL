package pacman.controllers;

import java.util.EnumMap;

import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;

public abstract class GhostController extends Controller<EnumMap<GHOST, MOVE>>{

	public GhostController copy(boolean po) {
		return this;
	}
}
