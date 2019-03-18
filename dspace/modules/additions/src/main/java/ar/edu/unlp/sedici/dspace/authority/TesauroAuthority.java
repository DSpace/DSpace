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

		String rdfType = "";
		switch (field) {
		case "thesis_degree_name":
			rdfType = "sedici:Grado";
			break;
		case "sedici_subject_descriptores":
			rdfType = "sedici:Descriptor";
			break;
		case "sedici_subject_materias":
			rdfType = "sedici:Materia";
			break;
		case "sedici_subject_eurovoc":
			rdfType = "sedici:Eurovoc";
			break;
		case "sedici_subject_decs":
			rdfType = "sedici:Decs";
			break;
		case "dc_coverage_spatial":
			rdfType = "sedici:Spatial";
			break;
		default:
			break;
		}
		
		text = normalizeTextForParserSPARQL10(text);

		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("sedici", NS_SEDICI);

		pqs.setCommandText("SELECT ?term ?label ?parent ?parentLabel \n");
		pqs.append("WHERE {\n");
		pqs.append("?term a "+ rdfType +"; skos:prefLabel ?label .\n");
		pqs.append("OPTIONAL { ?term skos:broader ?parent . ?parent skos:prefLabel ?parentLabel } \n");
		if (!"".equals(text.trim())) {
			pqs.append("FILTER(REGEX(?label, ?text, 'i'))\n");
			pqs.setLiteral("text", text.trim());
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ASC(?label)\n");
		
		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("term").getURI();
		String value = solution.getLiteral("label").getString();
		String label = "";
		if (solution.contains("parentLabel")){
			label = solution.getLiteral("parentLabel").getString() + " - ";			
		}
		label += solution.getLiteral("label").getString();
		return new Choice(key, value, label);
	}

}