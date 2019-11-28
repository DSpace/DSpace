package ar.edu.unlp.sedici.dspace.authority;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.dspace.content.authority.Choice;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
//FIXME cambiar  los queries para que levanten autores
public class AuthorAuthority extends SPARQLAuthorityProvider {

	protected static final Resource person = ResourceFactory.createResource(NS_FOAF + "Person");
	protected static final Property surname = ResourceFactory.createProperty(NS_FOAF + "surname");
	protected static final Property type = ResourceFactory.createProperty(NS_RDF + "type");
	protected static final Property givenName = ResourceFactory.createProperty(NS_FOAF + "givenName");
	protected static final Property mbox = ResourceFactory.createProperty(NS_FOAF + "mbox");
	protected static final Property organization = ResourceFactory.createProperty(NS_FOAF + "Organization");
	protected static final Property linksToOrganisationUnit = ResourceFactory.createProperty(NS_CERIF, "linksToOrganisationUnit");
	protected static final Property orgName = ResourceFactory.createProperty(NS_FOAF + "name");	
	protected static final Property siocId = ResourceFactory.createProperty(NS_SIOC + "id");
	protected static final Property skosBroader = ResourceFactory.createProperty(NS_SKOS + "broader");
	protected static final Property startDate = ResourceFactory.createProperty(NS_CERIF + "startDate");
	protected static final Property endDate = ResourceFactory.createProperty(NS_CERIF + "endDate");

	protected ResIterator getRDFResources(Model model) {
		return model.listSubjectsWithProperty(type, person);
	}

	protected Choice extractChoice(Resource subject) {
		
		String key = subject.getURI();
		String label = subject.getProperty(surname).getString() + ", " + subject.getProperty(givenName).getString() ;
		String value = label;
		StmtIterator links = subject.listProperties(linksToOrganisationUnit);
		if (links.hasNext()){
			label += getAffiliations(links);
		}
		
		return new Choice(key, value, label);
	}

	protected Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery) {
		List<Choice> choices = new LinkedList<Choice>();

		Model model = httpQuery.execConstruct(ModelFactory.createDefaultModel());
		ResIterator RDFResources = getRDFResources(model);
		while (RDFResources.hasNext()){
			choices.add(this.extractChoice(RDFResources.next()));
		};
		
		return choicesListToArraySorted(choices);
	}

	@Override
	protected ParameterizedSparqlString getSparqlSearch(String field, String filter, String locale, boolean idSearch) {
		if (idSearch)
			return this.getSparqlSearchByIdQuery(field, filter, locale);
		else
			return this.getSparqlSearchByTextQuery(field, filter, locale);
	}

	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("foaf", NS_FOAF);
		pqs.setNsPrefix("dc", NS_DC);
		pqs.setNsPrefix("cerif", NS_CERIF);
		pqs.setNsPrefix("rdf", NS_RDF);
		pqs.setNsPrefix("sioc", NS_SIOC);

		pqs.setCommandText("CONSTRUCT { ?person a foaf:Person. ?person foaf:givenName ?name . ?person foaf:surname ?surname . }\n");
		pqs.append("WHERE {\n");
		pqs.append("?person a foaf:Person ; foaf:givenName ?name ; foaf:surname ?surname .\n");
		pqs.append("FILTER(REGEX(?person, ?key, \"i\"))\n");
		pqs.append("}\n");
		pqs.append("ORDER BY ?surname \n");

		pqs.setLiteral("key", key);
		return pqs;
	}

	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("foaf", NS_FOAF);
		pqs.setNsPrefix("dc", NS_DC);
		pqs.setNsPrefix("cerif", NS_CERIF);
		pqs.setNsPrefix("rdf", NS_RDF);
		pqs.setNsPrefix("sioc", NS_SIOC);
		pqs.setNsPrefix("skos", NS_SKOS);

		pqs.setCommandText("CONSTRUCT { ?person a foaf:Person. ?person foaf:givenName ?name . ?person foaf:mbox ?mail . ?person foaf:surname ?surname. ?person cerif:linksToOrganisationUnit ?link . ?link cerif:startDate ?inicio. ?link cerif:endDate ?fin . ?link foaf:Organization ?org . ?org foaf:name ?affiliation. ?org sioc:id ?id. ?org skos:broader ?parent. ?parent foaf:name ?parentName . ?parent sioc:id ?idParent . ?parent skos:broader ?gParent. ?gParent foaf:name ?gParentName . ?gParent sioc:id ?idGParent }\n");
		pqs.append("WHERE {\n");
		pqs.append("?person a foaf:Person ; foaf:givenName ?name; foaf:surname ?surname. \n");
		pqs.append("	OPTIONAL { ?person foaf:mbox ?mail . } . \n"); 
		pqs.append("	OPTIONAL { " // begin 2do optional
				+ "	OPTIONAL { ?person cerif:linksToOrganisationUnit ?link . " // begin link
					+ " OPTIONAL { ?link cerif:startDate ?inicio; cerif:endDate ?fin . } ."
					+ " OPTIONAL { ?link foaf:Organization ?org . ?org foaf:name ?affiliation. " // begin org
						+ " OPTIONAL { ?org sioc:id ?id }. "
						+ " OPTIONAL { ?org skos:broader ?parent. ?parent foaf:name ?parentName . " //begin parent
						+ "		OPTIONAL {?parent sioc:id ?idParent} . "
							+ " OPTIONAL { ?parent skos:broader ?gParent. ?gParent foaf:name ?gParentName . " //begin gparent
								+ "    OPTIONAL {?gParent sioc:id ?idGParent} . "
							+ "} " // end gparent
						+ "} " // end parent
					+ "} " // end org
			+ "} \n"); // end link
		pqs.append("	}\n"); 
		if (!"".equals(text)) {
			String[] tokens = text.split(",");
			if (tokens.length > 1 && tokens[0].trim().length() > 0 && tokens[1].trim().length() > 0) {
				pqs.append("FILTER(REGEX(?name, ?text2, \"i\") && REGEX(?surname, ?text1, \"i\"))\n");
				pqs.setLiteral("text1", tokens[0].trim());
				pqs.setLiteral("text2", "^" + tokens[1].trim());
			} else {
				pqs.append("FILTER(REGEX(?name, ?text, \"i\") || REGEX(?surname, ?text, \"i\") || REGEX(?id, ?text, \"i\"))\n");
				pqs.setLiteral("text", tokens[0]);
			}
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ?surname ?link\n");
		
		return pqs;
	}


	private String getAffiliations(StmtIterator links) {
		String string = " (";
		while (links.hasNext()){
			Statement link = links.next();
			
			Resource affiliation = link.getObject().asResource();
			if (affiliation.hasProperty(organization)){
					
				Resource org = affiliation.getProperty(organization).getObject().asResource();			
				if (org.getProperty(orgName) != null) {
					String name = org.getProperty(orgName).getString();					
					if (org.getProperty(orgName) != null) {
						String id = org.getProperty(siocId).getString();
						if (org.hasProperty(skosBroader)){
							Resource orgParent = org.getProperty(skosBroader).getObject().asResource();
							if (orgParent.hasProperty(skosBroader)){
								string += this.getParentToString(orgParent.getProperty(skosBroader).getObject().asResource());					
							}
							string += this.getParentToString(orgParent);
						}
						string += (!"".equals(id)) ? id : name;
						String start = affiliation.getProperty(startDate).getString();
						String end = affiliation.getProperty(endDate).getString();
						if(!"".equals(start) || !"".equals(end)){
							string += getPeriodForFiliation(start, end);
						}
					}
				}
	
				if (links.hasNext()) string += ", ";
			}
		};
		return string += ")";
	}

	private String getParentToString(Resource orgParent) {
		String string = "";
		if (orgParent != null){
			String idParent = (orgParent.hasProperty(siocId)) ? orgParent.getProperty(siocId).getString() : null;
			String nameParent = (orgParent.hasProperty(orgName)) ? orgParent.getProperty(orgName).getString() : null;
			string += (!"".equals(idParent)) ? idParent : nameParent;
			string += " > ";
		}		
		return string;
	}

	private String getPeriodForFiliation(String start, String end) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy");
			if (!"".equals(start)){
				start = df.format(new java.util.Date((Long.valueOf(start)*1000)));
			}
			if (!"".equals(end)){
				end = df.format(new java.util.Date((Long.valueOf(end)*1000)));
			}
		return " [" + start + " - "+  end + "]";
	}


	public ParameterizedSparqlString getSparqlEmailByTextQuery(String field,
			String text, String locale) {
		return  this.getSparqlSearchByTextQuery(field,text,locale);		
	}
	
	
	/**
	 * Get the name and email from a Jena Model containing (NS_FOAF + "Person") elements.
	 * @param model
	 * @return return and array with the email in the 0 position and the name in the 1 position
	 */
	public ArrayList<String[]> extractNameAndEmailFromAuthors(Model model){
		ResIterator persons = getRDFResources(model);
		ArrayList<String[]> result = new ArrayList<String[]>();
		while(persons.hasNext()) {
			Resource currentPerson = persons.next();
			if(currentPerson.getProperty(mbox) != null) {
				String[] respuesta = new String[2];				
				respuesta[0] = currentPerson.getProperty(mbox).getString();
				respuesta[1] = currentPerson.getProperty(givenName).getString() + ", " + currentPerson.getProperty(surname).getString();
				result.add(respuesta);
			}
		}
		return result;
	}

}