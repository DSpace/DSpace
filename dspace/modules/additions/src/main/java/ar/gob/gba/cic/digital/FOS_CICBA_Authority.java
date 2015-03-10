package ar.gob.gba.cic.digital;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

public class FOS_CICBA_Authority extends CICBAAuthority{
	
	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		//TODO: analizar si es posible pre-compilar el query
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("cic", NS_CIC);
		
		pqs.setCommandText("SELECT ?concept ?label\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a skos:Concept ; a cic:Materia ; skos:prefLabel ?label .\n");
		pqs.append("FILTER(REGEX(?concept, ?key, \"i\"))\n");
		pqs.append("}\n");	
		
		pqs.setLiteral("key", key);
		return pqs;
	}


	@Override
	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("cic", NS_CIC);
		
		pqs.setCommandText("SELECT ?concept ?label\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a skos:Concept ; a cic:Materia ; skos:prefLabel ?label .\n");
		if (!"".equals(text)) {
			pqs.append("FILTER(REGEX(?label, ?text, \"i\"))\n");
			pqs.setLiteral("text", text);
		}
		pqs.append("}\n");	
		pqs.append("ORDER BY ASC(?label)\n");

		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("concept").getURI();
		String label = solution.getLiteral("label").getString();
		return new Choice(key, label, label);
	}
}
