package ar.edu.unlp.sedici.dspace.authority;

import org.dspace.content.authority.Choice;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
public class InstitutionAuthority extends SimpleSPARQLAuthorityProvider {


	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("foaf", NS_FOAF);
		pqs.setNsPrefix("dc", NS_DC);
		pqs.setNsPrefix("sioc", NS_SIOC);

		pqs.setCommandText("SELECT ?institution ?label ?initials\n");
		pqs.append("WHERE {\n");
		pqs.append("?institution a foaf:Organization ; foaf:name ?label .\n");
		pqs.append("OPTIONAL { ?institution sioc:id ?initials} \n");
		pqs.append("FILTER(REGEX(?institution, ?key, \"i\"))\n");
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
		pqs.setNsPrefix("sioc", NS_SIOC);

		pqs.setCommandText("SELECT ?institution ?label ?initials\n");
		pqs.append("WHERE {\n");
		pqs.append("?institution a foaf:Organization ; foaf:name ?label .\n");
		pqs.append("OPTIONAL { ?institution sioc:id ?initials} \n");
		if (!"".equals(text)) {
			pqs.append("FILTER(REGEX(?label, ?text, \"i\") || REGEX(?initials, ?text, \"i\"))\n");
			pqs.setLiteral("text", text);
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?label)\n");

		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("institution").getURI();
		String label = solution.getLiteral("label").getString();
		
		if (solution.contains("initials") && !"".equals(solution.getLiteral("initials").getString())) {
			String initials = solution.getLiteral("initials").getString();
			label = label + " (" + initials + ")";
		}
		
		return new Choice(key, label, label);
	}
}