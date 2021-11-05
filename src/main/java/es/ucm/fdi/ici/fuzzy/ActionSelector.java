package es.ucm.fdi.ici.fuzzy;

import java.util.HashMap;

import es.ucm.fdi.ici.Action;

public interface ActionSelector {

	public abstract Action selectAction(HashMap<String, Double> fuzzyOutput);
}
