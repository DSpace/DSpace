package ar.edu.unlp.sedici.dspace.authority;

import org.dspace.content.authority.Choice;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
public class InstitutionAuthority extends GeneralSEDICIAuthorityProvider {

	@Override
	protected String getSelectQueryFields(boolean idSearch) {
		String selectFields= super.getSelectQueryFields(idSearch);
		//Si la query se hace por text hay que agregar mas variables
		if (!idSearch)
			selectFields +=" ?initials ?labelpadre ?initpadre";
		return selectFields;
	}

	protected void getTextFilterQuery(ParameterizedSparqlString pqs, String filter) {
		pqs.append("OPTIONAL { ?concept sioc:id ?initials } . \n");
		pqs.append("OPTIONAL { ?concept skos:broader ?padre . ?padre foaf:name ?labelpadre  OPTIONAL { ?padre sioc:id  ?initpadre } . } \n");
		if (!"".equals(filter)) {
			pqs.append("FILTER(REGEX(?label, ?text, \"i\") || REGEX(?initials, ?text, \"i\"))\n");
			pqs.setLiteral("text", filter.trim());
		}
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("concept").getURI();
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