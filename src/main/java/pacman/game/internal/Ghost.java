package pacman.game.internal;

import static pacman.game.Constants.EDIBLE_TIME;
import static pacman.game.Constants.EDIBLE_TIME_REDUCTION;
import static pacman.game.Constants.LEVEL_RESET_REDUCTION;

import java.util.Random;

import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * Data structure to hold all information pertaining to the ghosts.
 */
public final class Ghost {
    public int currentNodeIndex, edibleTime, lairTime;
    public GHOST type;
    public MOVE lastMoveMade;

    public Ghost(GHOST type, int currentNodeIndex, int edibleTime, int lairTime, MOVE lastMoveMade) {
        this.type = type;
        this.currentNodeIndex = currentNodeIndex;
        this.edibleTime = edibleTime;
        this.lairTime = lairTime;
        this.lastMoveMade = lastMoveMade;
    }
    
    public Ghost(GHOST type, Maze maze, int levelCount, Game game) {
    	Random rnd = new Random();
        this.type = type;
        this.currentNodeIndex = maze.graph[rnd.nextInt(maze.graph.length)].nodeIndex;
        if (rnd.nextBoolean())
        	this.edibleTime = rnd.nextInt((int) (EDIBLE_TIME * (Math.pow(EDIBLE_TIME_REDUCTION, levelCount % LEVEL_RESET_REDUCTION))));
        else
        	this.edibleTime = 0;
        this.lairTime = 0;
        MOVE[] moves = game.getPossibleMoves(this.currentNodeIndex);
        this.lastMoveMade = moves[rnd.nextInt(moves.length)];
    }
    

    public Ghost copy() {
        return new Ghost(type, currentNodeIndex, edibleTime, lairTime, lastMoveMade);
    }
}