/**
 * Clase RecipeBuilder
 * @autor terru
 * Esta clase permite construir una receta de un tipo determinado
 * 
 */
package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;


import ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules.Rule;

public class ConfigurationBuilder {
	private static HashMap<String,Rule> rules = new HashMap<String,Rule>();
	
	
	public ConfigurationBuilder(String config) throws SecurityException, IllegalArgumentException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException{	
		//FIXME: se podrían mapear las excepciones
		this.setRules(config);
	}
	
	
	private HashMap<String,Rule> getRules(){
		return ConfigurationBuilder.rules;
	}
	
	private void setRules(String config) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		String[] ruleCount = config.trim().split(",");
		for (int i = 0; i < ruleCount.length; i++){
			//separate ruleName-RuleClass;
			String ruleName = ruleCount[i].split("=")[1].trim();
			String ruleClassName = ruleCount[i].split("=")[0].trim();
			Class userClass = Class.forName(ruleClassName); 
			Constructor userConstructor = userClass.getConstructor(new Class[]{});
			Rule rule = (Rule) userConstructor.newInstance();
			this.getRules().put(ruleName, rule);
		}	
	}
	
	public Recipe newRecipe(String recipeName, String stepList) {
		Recipe recipe= new Recipe();
		String recipeString = stepList;
		//corto por paso
		String[]recipeSteps = recipeString.trim().split(";");
		for(int i = 0; i< recipeSteps.length; i++){
			//cada elemento de la iteración es un string regla,peso
			//Se recupera la regla y el peso
			String[] string2parse = recipeSteps[i].split(",");
			//se recupera el nombre de clase
			String ruleName = string2parse[0].trim(); 
			//se recupera la instancia
			//se levanta de la rule
			Rule rule = this.getRules().get(ruleName);
			float weigth = Float.parseFloat(string2parse[1].trim());
			//se crea un ruleStep con peso y orden, se añade a la receta
			RuleStep newStep = new RuleStep(i,rule,weigth);
			recipe.addStep(newStep);
		}
		return recipe;
		//retorna una receta con la lista de reglas que se recibe
	}
}
