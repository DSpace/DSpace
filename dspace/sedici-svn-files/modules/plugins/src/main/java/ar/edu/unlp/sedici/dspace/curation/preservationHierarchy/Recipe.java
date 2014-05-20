package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Recipe {
	private ArrayList<RuleStep> steps;
	private Iterator<RuleStep> iterator;
	
	public Recipe(){
		ArrayList<RuleStep> steps = new ArrayList<RuleStep>();
		this.setSteps(steps);
		this.setIterator(null);//el iterador no existe al inicio
	}
	
	public void addStep(RuleStep step){
		this.getSteps().add(step.getOrder(), step);
	}
	
	public void delStep(RuleStep step){
		this.getSteps().remove(step);
	}

	public boolean hasNextStep(){
		if(this.getIterator()==null){ //puede preguntar si tiene next antes de inicializarse
			this.setIterator(this.getSteps().iterator());
		}
		return this.getIterator().hasNext();
	}
	
	public void resetRecipe(){
		this.setIterator(null);
	}
	
	public RuleStep nextStep() throws NoSuchElementException{
		if(this.getIterator()==null){
			this.setIterator(this.getSteps().iterator());
		}
		return this.getIterator().next();	
	}

	private ArrayList<RuleStep> getSteps() {
		return steps;
	}

	private void setSteps(ArrayList<RuleStep> steps) {
		this.steps = steps;
	}

	private void setIterator(Iterator<RuleStep> iterator) {
		this.iterator = iterator;		
	}

	private Iterator<RuleStep> getIterator() {
		return this.iterator;
	}
}
