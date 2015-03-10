package ar.gob.gba.cic.digital;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

public class Subject_CICBA_Authority extends CICBAAuthority {

	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("dc", NS_DC);
		pqs.setNsPrefix("cic", NS_CIC);

		pqs.setCommandText("SELECT ?term ?label ?id\n");
		pqs.append("WHERE {\n");
		pqs.append("?term a cic:Term ; dc:title ?label ; dc:identifier ?id .\n");
		pqs.append("FILTER(REGEX(?term, ?key, \"i\"))\n");
		pqs.append("}\n");

		pqs.setLiteral("key", key);
		return pqs;
	}

	@Override
	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("dc", NS_DC);
		pqs.setNsPrefix("cic", NS_CIC);

		pqs.setCommandText("SELECT ?term ?label ?id\n");
		pqs.append("WHERE {\n");
		pqs.append("?term a cic:Term ; dc:title ?label ; dc:identifier ?id .\n");
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
		String label = solution.getLiteral("label").getString();
		String id = solution.getLiteral("label").getString();
		return new Choice(key, label, label + " (" + id + ")");
	}

}
