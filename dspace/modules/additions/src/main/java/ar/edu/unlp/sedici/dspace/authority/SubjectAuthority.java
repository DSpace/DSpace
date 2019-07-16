package ar.edu.unlp.sedici.dspace.authority;

import java.util.ArrayList;
import java.util.HashMap;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public class SubjectAuthority extends GeneralSEDICIAuthorityProvider {

	@Override
	protected String getSelectQueryFields(boolean idSearch) {
		return " DISTINCT ?label ?uri";
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = "";
		if (solution.contains("uri")){
			key = solution.getLiteral("uri").getString();			
		} else {
			key = solution.getResource("concept").getURI();			
		}
		String value = solution.getLiteral("label").getString();
		String label = value;
		return new Choice(key, value, label);
	}

	@Override
	protected Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery) {
		HashMap<String, Choice> choices = new HashMap<String, Choice>();
		ResultSet results = httpQuery.execSelect();
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			Choice choice = this.extractChoice(solution);
			if (!choices.containsKey(choice.label)){
				choices.put(choice.label, choice);
			}
		}
		ArrayList<Choice> list = new ArrayList<Choice>(choices.values());
		return list.toArray(new Choice[0]);
	}
	
}
