package es.ucm.fdi.ici.rl;

import java.util.List;
import java.util.Random;

import com.github.chen0040.rl.learning.qlearn.QLearner;

public class qlearn {
	public qlearn() {
		int stateCount = 100;
		int actionCount = 10;
		QLearner agent = new QLearner(stateCount, actionCount);

		Random random = new Random();
		int currentState = random.nextInt(stateCount));
		List<TupleThree<Integer, Integer, Double>> moves = new ArrayList<>();
		for(int time=0; time < 1000; ++time){

		 int actionId = agent.selectAction(currentState).getIndex();
		 System.out.println("Agent does action-"+actionId);
		 
		 int newStateId = world.update(agent, actionId);
		 double reward = world.reward(agent);

		 System.out.println("Now the new state is " + newStateId);
		 System.out.println("Agent receives Reward = "+reward);
		 int oldStateId = currentState;
		 moves.add(new Move(oldStateId, actionId, newStateId, reward));
		  currentState = newStateId;
		}

		for(int i=moves.size()-1; i >= 0; --i){
		    Move move = moves.get(i);
		    agent.update(move.oldState, move.action, move.newState, move.reward);
		}
	}
	
	class Move {
	    int oldState;
	    int newState;
	    int action;
	    double reward;
	    
	    public Move(int oldState, int action, int newState, double reward) {
	        this.oldState = oldState;
	        this.newState = newState;
	        this.reward = reward;
	        this.action = action;
	    }
	}
}
