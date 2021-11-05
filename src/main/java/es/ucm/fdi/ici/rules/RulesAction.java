package es.ucm.fdi.ici.rules;

import es.ucm.fdi.ici.Action;
//import jess.Fact;

/**
 * An action to be executed once asserted in the Rule Engine.
 * The asserted fact must comply the following syntax:
 * (ACTION (id action) [(slot ...) (slot ...) ...])
 * @author Juan Ant. Recio García - Universidad Complutense de Madrid
 */
public interface RulesAction extends Action {
		
	public static final String FACT_NAME = "ACTION";
	public static final String ID_SLOT = "id";

	/**
	 * This method allows to parameterize the action from the slots in the corresponding fact.
	 * @param actionFact
	 */
	//public void parseFact(Fact actionFact);
	
}
