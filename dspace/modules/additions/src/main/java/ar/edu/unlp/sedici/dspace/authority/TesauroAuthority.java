package ar.edu.unlp.sedici.dspace.authority;

import org.dspace.content.authority.Choice;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

public class TesauroAuthority extends GeneralSEDICIAuthorityProvider {

	@Override
	protected String getSelectQueryFields(boolean idSearch) {
		String selectFields= super.getSelectQueryFields(idSearch);
		//Si la query se hace por text hay que agregar mas variables
		if (!idSearch)
			selectFields +=" ?parent ?parentLabel";
		return selectFields;
	}

	protected void getTextFilterQuery(ParameterizedSparqlString pqs, String filter) {
		pqs.append("OPTIONAL { ?concept skos:broader ?parent . ?parent skos:prefLabel ?parentLabel } \n");
		super.getTextFilterQuery(pqs, filter);
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("concept").getURI();
		String value = solution.getLiteral("label").getString();
		String label = "";
		if (solution.contains("parentLabel")){
			label = solution.getLiteral("parentLabel").getString() + " - ";			
		}
		label += solution.getLiteral("label").getString();
		return new Choice(key, value, label);
	}

}