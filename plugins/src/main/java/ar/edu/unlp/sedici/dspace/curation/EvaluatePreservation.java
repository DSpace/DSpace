package ar.edu.unlp.sedici.dspace.curation;

import java.io.IOException;
import java.util.ArrayList;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.*;
/**
 * @author terru
 * Esta tarea evalúa cada uno de los items que se le pasan, 
 * según ciertas reglas de preservación que se pueden definir como clases
 * que heredan de la clase abstracta RULE. 
 * Además puede evaluar los item como combinación de esta reglas, a las que
 * les asigna un valor y un orden específico en lo que se conoce como
 * RECETA, las cuales pueden ser configuradas, creadas y cambiadas en
 * tiempo de ejecución, en al archivo
 * @doc [dspace]/config/modules/evaluatePreservation.cfg
 * @see https://docs.google.com/document/d/11owxCgDUgsExpVyD4gpOrBwbc38GDm3c6K4cxXSYTa0/edit
 */
public class EvaluatePreservation extends AbstractCurationTask {
	private Recipe recipe=null;
	// guarda la receta que esta usando, para crearla una sola vez para todos los items

	@Override
	public int perform(DSpaceObject dso) throws IOException {
		this.initConfig(); //levanto las reglas y las recetas
		if (dso.getType() != Constants.ITEM) {
			String print = "Omitido por no ser Item";
			setResult(print);
			report(print);
			return Curator.CURATE_SKIP;
		}
		try {
			Item item = (Item)dso;
			Reporter reporter = new Reporter();
			reporter.addToItemReport("Procesando Item con id: "+item.getID());
			//report("Procesando Item  con id: "+item.getID());
			float value = this.evaluateItem(item,reporter);
			//report("La puntuación total del item procesado es: "+value);
			reporter.addToItemReport("La puntuación total del item procesado es: "+value);
			//String print= "El item: "+item.getID()+" fue procesado con éxito";
			reporter.addToItemReport("El item: "+item.getID()+" se finalizó de procesar con éxito");
			//report(El item: "+item.getID()+" fue procesado con éxito);
			report(reporter.getReport());
			setResult(reporter.getReport());
			return Curator.CURATE_SUCCESS;
		} catch(Exception e){
			String print = "Ha ocurrido un error en Item "+dso.getID()+":\n ["+e.getClass().getName()+"]"+e.getMessage()+"\n";
			e.printStackTrace();
			setResult(print);
			report(print);
			return Curator.CURATE_FAIL;
		}
	}

	private float evaluateItem(Item item, Reporter reporter) {
		float value = 0;
		while(this.getRecipe().hasNextStep()){
			RuleStep recipeStep = recipe.nextStep();
			int weigth = recipeStep.getRule().evaluate(item,reporter);//esta recibe el reporte
			value += weigth * recipeStep.getWeight();
		}
		//como ya terminó la reseta para el item, se resetea y devuelve el valor
		this.getRecipe().resetRecipe();
		return value;
	}

	private Recipe getRecipe() {
		return this.recipe;
	}
	
	private void setRecipe(Recipe recipe){
		this.recipe = recipe;
	}
	
	private void initConfig(){
		if(this.getRecipe()==null){ //si la receta esta en null levanta la configuración
			String config = taskProperty("validator.rules");
			try{
				ConfigurationBuilder builder = new ConfigurationBuilder(config);
				this.initRecipe(builder);
			} catch (Exception e) {
				String print = "Un error ha ocurrido al iniciar la configuración";
				report(print);
				setResult(print);
				e.printStackTrace();
			}
		}
	}
	
	private void initRecipe(ConfigurationBuilder builder){
		String recipeName = taskProperty("recipes.name");
		String recipeString = taskProperty("recipes.rules");
		Recipe recipe = builder.newRecipe(recipeName,recipeString);
		this.setRecipe(recipe);
	}
}