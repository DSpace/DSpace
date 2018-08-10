package ar.edu.unlp.sedici.dspace.authority;


import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import ar.edu.unlp.sedici.dspace.authority.SPARQLAuthorityProvider;

public abstract class AdvancedSPARQLAuthorityProvider extends SPARQLAuthorityProvider {
	
	protected static final String NS_CIC = "http://www.cic.gba.gov.ar/ns#";

	protected abstract ResIterator getRDFResources(Model model);
	protected abstract Choice extractChoice(Resource resource);

	protected Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery) {
		List<Choice> choices = new LinkedList<Choice>();
		
		Model model = httpQuery.execConstruct(ModelFactory.createDefaultModel());
		ResIterator RDFResources = getRDFResources(model);
		while (RDFResources.hasNext()){
			choices.add(this.extractChoice(RDFResources.next()));
		};		
		choices.sort(new Comparator<Choice>() {
		    @Override
		    public int compare(Choice m1, Choice m2) {
		        if(m1.label == m2.label){
		            return 0;
		        }
		        return m1.label.compareTo(m2.label) < 0 ? -1 : 1;
		     }
		});
		return choices.toArray(new Choice[0]);
	}


}