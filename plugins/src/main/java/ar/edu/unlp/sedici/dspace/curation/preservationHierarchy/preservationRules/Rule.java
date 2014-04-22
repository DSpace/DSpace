/**
 * 
 * Clase Abstracta PreservationRule
 * Esta clase abstracta representa la iterfaz que las reglas a evaluar 
 * en cada validación deben respetar.
 * De esta forma, el validador podrá utilizar cualquier conjunto arbitrario
 * de reglas que se le especifique cuando es invocado
 * @author terru
 * 
 */
package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules;



public abstract class Rule {
	public abstract int evaluate(org.dspace.content.Item item, ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.Reporter reporter);
}
