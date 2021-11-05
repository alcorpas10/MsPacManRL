package pacman.game.internal;

import static pacman.game.Constants.NUM_LIVES;

import java.util.Random;

import pacman.game.Game;
import pacman.game.Constants.MOVE;

/*
 * Data structure to hold all information pertaining to Ms Pac-Man.
 */
public final class PacMan {
    public int currentNodeIndex, numberOfLivesRemaining;
    public MOVE lastMoveMade;
    public MOVE lastDir;
    public boolean hasReceivedExtraLife;

    public PacMan(int currentNodeIndex, MOVE lastMoveMade, int numberOfLivesRemaining, boolean hasReceivedExtraLife) {
        this.currentNodeIndex = currentNodeIndex;
        this.lastDir = this.lastMoveMade = lastMoveMade;
        this.numberOfLivesRemaining = numberOfLivesRemaining;
        this.hasReceivedExtraLife = hasReceivedExtraLife;
    }
    
    public PacMan(Maze maze, Game game) {
    	Random rnd = new Random();
        this.currentNodeIndex = maze.graph[rnd.nextInt(maze.graph.length)].nodeIndex;
        this.numberOfLivesRemaining = 1; //rnd.nextInt(NUM_LIVES);
        MOVE[] moves = game.getPossibleMoves(this.currentNodeIndex);
        this.lastDir = this.lastMoveMade = moves[rnd.nextInt(moves.length)];
        this.hasReceivedExtraLife = false;
    }

    public PacMan copy() {
        return new PacMan(currentNodeIndex, lastMoveMade, numberOfLivesRemaining, hasReceivedExtraLife);
    }
}