package ar.gob.gba.cic.digital;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

//FIXME cambiar  los queries para que levanten autores
public class Author_CICBA_Authority extends CICBAAuthority {

	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("foaf", NS_FOAF);
		pqs.setNsPrefix("dc", NS_DC);

		pqs.setCommandText("SELECT ?id ?name ?surname ?affiliation\n");
		pqs.append("WHERE {\n");
		pqs.append("?person a foaf:Person ; foaf:name ?name ; foaf:surname ?surname ; dc:identifier ?id; foaf:Organization ?affiliation .\n");
		pqs.append("FILTER(REGEX(?id, ?key, \"i\"))\n");
		pqs.append("}\n");

		pqs.setLiteral("key", key);
		return pqs;
	}

	@Override
	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("foaf", NS_FOAF);
		pqs.setNsPrefix("dc", NS_DC);

		pqs.setCommandText("SELECT ?id ?name ?surname ?affiliation\n");
		pqs.append("WHERE {\n");
		pqs.append("?person a foaf:Person ; foaf:name ?name ; foaf:surname ?surname ; dc:identifier ?id; foaf:Organization ?affiliation .\n");
		pqs.append("FILTER(REGEX(?name, ?text, \"i\") || REGEX(?surname, ?text, \"i\") || REGEX(?id, ?text, \"i\"))\n");
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?surname)\n");

		pqs.setLiteral("text", text);
		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getLiteral("id").getString();
		String name = solution.getLiteral("name").getString();
		String surname = solution.getLiteral("surname").getString();
		String affiliation = solution.getLiteral("affiliation").getString();
		String label = surname + ", " + name;
		return new Choice(key, label, label + "(" + affiliation + ")");
	}

}
