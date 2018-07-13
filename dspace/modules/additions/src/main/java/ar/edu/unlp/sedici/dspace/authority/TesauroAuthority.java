package ar.edu.unlp.sedici.dspace.authority;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

public class TesauroAuthority extends SimpleSPARQLAuthorityProvider {

	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("skos", NS_SKOS);

		pqs.setCommandText("SELECT ?term ?label \n");
		pqs.append("WHERE {\n");
		pqs.append("?term a skos:Concept; skos:prefLabel ?label .\n");
		pqs.append("FILTER(REGEX(?term, ?key, \"i\"))\n");
		pqs.append("}\n");

		pqs.setLiteral("key", key);
		return pqs;
	}

	@Override
	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("skos", NS_SKOS);

		pqs.setCommandText("SELECT ?term ?label \n");
		pqs.append("WHERE {\n");
		pqs.append("?term a skos:Concept; skos:prefLabel ?label .\n");
		if (!"".equals(text)) {
			pqs.append("FILTER(REGEX(?label, ?text, \"i\") || REGEX(?id, ?text, \"i\"))\n");
			pqs.setLiteral("text", text);
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?label)\n");
		
		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("term").getURI();
		String id = solution.getLiteral("label").getString();
		return new Choice(key, id, id + " (" + id + ")");
	}

}