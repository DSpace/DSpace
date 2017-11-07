package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy;

import ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules.Rule;

public class RuleStep {
	private float weight;
	private Rule rule;
	private int order;
	
	public RuleStep(int order,Rule rule,float weigth){
		this.initialize(order,rule,weigth);
	}
	
	private void initialize(int order, Rule rule, float weigth) {
		this.setOrder(order);
		this.setRule(rule);
		this.setWeight(weigth);		
	}

	public float getWeight() {
		return weight;
	}

	public void setWeight(float weight) {
		this.weight = weight;
	}

	public Rule getRule() {
		return rule;
	}

	public void setRule(Rule rule) {
		this.rule = rule;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}
