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

		pqs.setCommandText("SELECT ?institution ?label \n");
		pqs.append("WHERE {\n");
		pqs.append("?institution a foaf:Organization ; foaf:name ?label .\n");
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
		pqs.setNsPrefix("skos", NS_SKOS);

		pqs.setCommandText("SELECT ?institution ?label ?initials ?labelpadre ?initpadre \n");
		pqs.append("WHERE {\n");
		pqs.append("?institution a foaf:Organization ; foaf:name ?label .\n");
		pqs.append("OPTIONAL { ?institution sioc:id ?initials } . \n");
		pqs.append("OPTIONAL { ?institution skos:broader ?padre . ?padre foaf:name ?labelpadre  OPTIONAL { ?padre sioc:id  ?initpadre } . } \n");    
		if (!"".equals(text)) {
			pqs.append("FILTER(REGEX(?label, ?text, \"i\") || REGEX(?initials, ?text, \"i\"))\n");
			text = normalizeTextForParserSPARQL10(text);
			pqs.setLiteral("text", text.trim());
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?label)\n");

		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("institution").getURI();
		String label = "";
		String value = solution.getLiteral("label").getString();
		if (solution.contains("labelpadre") && !"".equals(solution.getLiteral("labelpadre").getString())) {
			label += solution.getLiteral("labelpadre").getString();
			if (solution.contains("initpadre") && !"".equals(solution.getLiteral("initpadre").getString())) {
				String initpadre = solution.getLiteral("initpadre").getString();
				label += " (" + initpadre + ")";
			}
			label += " - ";
		}
		label += solution.getLiteral("label").getString();
		if (solution.contains("initials") && !"".equals(solution.getLiteral("initials").getString())) {
			String initials = solution.getLiteral("initials").getString();
			label += " (" + initials + ")";
		}


		return new Choice(key, value, label);
	}
}