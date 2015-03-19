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

		pqs.setCommandText("SELECT ?person ?name ?surname ?id ?affiliation\n");
		pqs.append("WHERE {\n");
		pqs.append("?person a foaf:Person ; foaf:givenName ?name ; foaf:familyName ?surname .\n");
		pqs.append("OPTIONAL { ?person foaf:Organization ?a ; dc:identifier ?id . ?a a foaf:Organization ; dc:title ?affiliation }\n");
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

		pqs.setCommandText("SELECT ?person ?name ?surname ?id ?affiliation\n");
		pqs.append("WHERE {\n");
		pqs.append("?person a foaf:Person ; foaf:givenName ?name ; foaf:familyName ?surname .\n");
		pqs.append("OPTIONAL { ?person foaf:Organization ?a ; dc:identifier ?id . ?a a foaf:Organization ; dc:title ?affiliation }\n");
		if (!"".equals(text)) {
			String[] tokens = text.split(",");
			if (tokens.length > 1) {
				pqs.append("FILTER(REGEX(?name, ?text2, \"i\") && REGEX(?surname, ?text1, \"i\"))\n");
				pqs.setLiteral("text1", tokens[0].trim());
				pqs.setLiteral("text2", tokens[1].trim());
			} else {
				pqs.append("FILTER(REGEX(?name, ?text, \"i\") || REGEX(?surname, ?text, \"i\") || REGEX(?id, ?text, \"i\"))\n");
				pqs.setLiteral("text", tokens[0]);
			}
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?surname)\n");
		
		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("person").getURI();
		String name = solution.getLiteral("name").getString();
		String surname = solution.getLiteral("surname").getString();
		
		String label = surname + ", " + name;
		String value = label;
		
		if (solution.contains("id")) {
			String id = solution.getLiteral("id").getString();
			value = value + " (" + id + ")";
		}
			
		if (solution.contains("affiliation")) {
			String affiliation = solution.getLiteral("affiliation").getString();
			value = value + " (" + affiliation + ")";
		}
		
		return new Choice(key, label, value);
	}
}
