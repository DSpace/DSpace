package ar.gob.gba.cic.digital;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

public class Journal_CICBA_Authority extends CICBAAuthority {

	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("dc", NS_DC);

		pqs.setCommandText("SELECT ?concept ?label\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a dc:BibliographicResource ; dc:title ?label .\n");
		pqs.append("FILTER(REGEX(?concept, ?key, \"i\"))\n");
		pqs.append("}\n");

		pqs.setLiteral("key", key);
		return pqs;
	}

	@Override
	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("dc", NS_DC);

		pqs.setCommandText("SELECT ?concept ?label\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a dc:BibliographicResource ; dc:title ?label .\n");
		pqs.append("FILTER(REGEX(?label, ?text, \"i\"))\n");
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?label)\n");

		pqs.setLiteral("text", text);
		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("concept").getURI();
		String label = solution.getLiteral("label").getString();
		return new Choice(key, label, label + "(" + key + ")");
	}

}
