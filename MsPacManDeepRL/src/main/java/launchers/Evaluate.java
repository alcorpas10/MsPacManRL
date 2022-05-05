package launchers;

import engine.es.ucm.fdi.ici.PacManEvaluatorTFG;
import engine.es.ucm.fdi.ici.Scores;

public class Evaluate {

	public static void main(String[] args) {
		PacManEvaluatorTFG evaluator = new PacManEvaluatorTFG();
		Scores scores = evaluator.evaluate();
		scores.printScoreAndRanking();
	}
}