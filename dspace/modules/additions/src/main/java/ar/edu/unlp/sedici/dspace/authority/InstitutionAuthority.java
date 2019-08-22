package ar.edu.unlp.sedici.dspace.authority;

import org.dspace.content.authority.Choice;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
public class InstitutionAuthority extends GeneralSEDICIAuthorityProvider {

	@Override
	protected String getSelectQueryFields(boolean idSearch) {
		String selectFields= super.getSelectQueryFields(idSearch);
		//Si la query se hace por text hay que agregar mas variables
		return selectFields +=" ?name";
	}

	@Override
	protected void getWhereQueryFields(ParameterizedSparqlString pqs) {
		pqs.append("?concept foaf:name ?name .\n");
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("concept").getURI();
		String label = solution.getLiteral("label").getString();
		String value = solution.getLiteral("name").getString();

		return new Choice(key, value, label);
	}
	
}