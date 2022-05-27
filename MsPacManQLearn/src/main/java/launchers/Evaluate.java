package launchers;

import es.ucm.fdi.ici.PacManEvaluatorTFG;
import es.ucm.fdi.ici.Scores;

/**
 * Class that evaluates the results using the PacManEvaluatorTFG
 */
public class Evaluate {

	public static void main(String[] args) {
		PacManEvaluatorTFG evaluator = new PacManEvaluatorTFG(); 	//Run the evaluator
		Scores scores = evaluator.evaluate();		//Get the scores from the evaluator
		scores.printScoreAndRanking();	//Print scores and ranking
	}	
}