package ar.edu.unlp.sedici.dspace.authority;

import java.util.LinkedList;
import java.util.List;

import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import ar.edu.unlp.sedici.dspace.authority.SPARQLAuthorityProvider;

public abstract class GeneralSEDICIAuthorityProvider extends SPARQLAuthorityProvider {

    protected final String CHOICES_ONLYLEAFS_PREFIX = "sedici.choices.onlyLeafs.";
    protected final String CHOICES_PARENT_PREFIX = "sedici.choices.parent.";
    protected final String CHOICES_PARENTPROPERTY_PREFIX = "sedici.choices.parentProperty.";
    protected final String CHOICES_TYPEPROPERTY_PREFIX = "sedici.choices.typeProperty.";
    protected final String CHOICES_LABELPROPERTY_PREFIX = "sedici.choices.labelProperty.";
    protected final String CHOICES_EXTERNALKEY_PREFIX = "sedici.choices.externalKeyProperty.";

	protected ParameterizedSparqlString getSparqlSearch(
			String field, String filter, String locale,boolean idSearch) {

		String metadataField= field.replace("_",".");
		String typeProperty= ConfigurationManager.getProperty(CHOICES_TYPEPROPERTY_PREFIX+metadataField)!=null ?
				ConfigurationManager.getProperty(CHOICES_TYPEPROPERTY_PREFIX+metadataField) :"skos:concept";
		String labelProperty= ConfigurationManager.getProperty(CHOICES_LABELPROPERTY_PREFIX+metadataField) != null ?
				ConfigurationManager.getProperty(CHOICES_LABELPROPERTY_PREFIX+metadataField):"skos:prefLabel";
		String parent= ConfigurationManager.getProperty(CHOICES_PARENT_PREFIX+metadataField) != null ?
				ConfigurationManager.getProperty(CHOICES_PARENT_PREFIX+metadataField):null;
		String externalKey= ConfigurationManager.getProperty(CHOICES_EXTERNALKEY_PREFIX + metadataField) != null ?
				ConfigurationManager.getProperty(CHOICES_EXTERNALKEY_PREFIX + metadataField):null;
		String parentProperty= ConfigurationManager.getProperty(CHOICES_PARENTPROPERTY_PREFIX+metadataField) != null ?
				ConfigurationManager.getProperty(CHOICES_PARENTPROPERTY_PREFIX+metadataField):"skos:broader";
		boolean onlyLeafs= ConfigurationManager.getBooleanProperty(CHOICES_ONLYLEAFS_PREFIX+metadataField,false);

		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("foaf", NS_FOAF);
		pqs.setNsPrefix("dc", NS_DC);
		pqs.setNsPrefix("sedici", NS_SEDICI);
		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("sioc", NS_SIOC);
		pqs.setNsPrefix("owl", NS_OWL);

		pqs.setCommandText("SELECT "+ this.getSelectQueryFields(idSearch) + "\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a "+ typeProperty + " .\n");
		pqs.append("?concept "+ labelProperty +" ?label .\n");
		if (externalKey != null) {
			pqs.append("?concept "+ externalKey +" ?externalKey .\n");			
		}
		if (parent != null) {
		   //Si el parent es vacio se buscan nodos raiz, es decir, sin padre
		   if ("".equals(parent)){
		      pqs.append("OPTIONAL { ?concept " +parentProperty+" ?father } \n");
		      pqs.append("FILTER(!bound(?father)) \n");
		   }
		   //Si no es vacio es un parent id, se buscan los hijos directos de ese padre
		   else
		      pqs.append("?concept " +parentProperty+" "+parent+".\n");
		}
		//Si la propiedad onlyLeafs es true se buscan los nodos hoja, es decir, sin hijos
		if (onlyLeafs) {
			pqs.append("OPTIONAL { ?hijos "+parentProperty+" ?concept}\n");
			pqs.append("FILTER(!bound(?hijos)) \n");
		}
		//Dependiendo del paramtro idSearch la busqueda se hace por id o por text
		if (idSearch) {
			getIdSearchFilterQuery(pqs,filter);
			pqs.append("}\n");
		}
		else {
			getTextFilterQuery(pqs,filter);
			pqs.append("}\n");
			pqs.append("ORDER BY ASC(?label)\n");
		}

		return pqs;
	}

	protected void getIdSearchFilterQuery(ParameterizedSparqlString pqs, String filter) {
		pqs.append("FILTER(?concept = ?key) \n");
		pqs.setLiteral("key", filter);
	}

	protected String getSelectQueryFields(boolean idSearch){
		return "?concept ?label";
	}
	
	protected void getTextFilterQuery(ParameterizedSparqlString pqs, String filter) {
		if (!"".equals(filter)) {
			pqs.append("FILTER(REGEX(?label, ?text, \"i\") || REGEX(?label, ?textWithCaret, \"i\")) \n");
			pqs.setLiteral("text", " " + filter.trim());
			pqs.setLiteral("textWithCaret", "^" + filter.trim());
		}
	}
	
	protected abstract Choice extractChoice(QuerySolution solution);


	protected Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery) {
		List<Choice> choices = new LinkedList<Choice>();
		ResultSet results = httpQuery.execSelect();
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			choices.add(this.extractChoice(solution));
		}
		return choicesListToArraySorted(choices);
	}

}