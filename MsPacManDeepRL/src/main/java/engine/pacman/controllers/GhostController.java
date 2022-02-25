package engine.pacman.controllers;

import java.util.EnumMap;

import engine.pacman.game.Constants.GHOST;
import engine.pacman.game.Constants.MOVE;

public abstract class GhostController extends Controller<EnumMap<GHOST, MOVE>>{

	public GhostController copy(boolean po) {
		return this;
	}
}
