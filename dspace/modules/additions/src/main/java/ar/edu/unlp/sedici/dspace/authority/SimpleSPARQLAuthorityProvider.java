package ar.edu.unlp.sedici.dspace.authority;

import java.util.LinkedList;
import java.util.List;

import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import ar.edu.unlp.sedici.dspace.authority.SPARQLAuthorityProvider;

public abstract class SimpleSPARQLAuthorityProvider extends SPARQLAuthorityProvider {
	
	protected abstract Choice extractChoice(QuerySolution solution);

	protected Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery) {
		List<Choice> choices = new LinkedList<Choice>();
		ResultSet results = httpQuery.execSelect();
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			choices.add(this.extractChoice(solution));
		}
		return choices.toArray(new Choice[0]);
	}

}