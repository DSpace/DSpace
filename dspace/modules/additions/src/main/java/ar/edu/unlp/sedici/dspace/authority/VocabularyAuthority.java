package ar.edu.unlp.sedici.dspace.authority;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;

//FIXME cambiar  los queries para que levanten autores
public class VocabularyAuthority extends SPARQLAuthorityProvider {

	protected static final Resource subject = ResourceFactory.createResource(NS_SEDICI+ "Subject");
	protected static final Property prefLabel = ResourceFactory.createProperty(NS_SKOS + "prefLabel");
	protected static final Property type = ResourceFactory.createProperty(NS_RDF + "type");
	protected static final Property sameAs = ResourceFactory.createProperty(NS_OWL + "sameAs");
	protected static final Property skosBroader = ResourceFactory.createProperty(NS_SKOS + "broader");

	protected ResIterator getRDFResources(Model model) {
		return model.listSubjectsWithProperty(type, subject);
	}

	protected Choice extractChoice(Resource subject) {
		
		String key = subject.getProperty(sameAs).getString();
		String label = subject.getProperty(prefLabel).getString();
		String value = label;
		Statement broader = subject.getProperty(skosBroader);
		if (broader != null) {
			String fatherLabel = broader.getResource().getProperty(prefLabel).getString();
			if (!fatherLabel.isEmpty()) {
				label += " (" + fatherLabel + ")";
			}
		}
		return new Choice(key, value, label);
	}

	protected Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery) {
		HashMap<String, Choice> choices = new HashMap<String, Choice>();
		Model model = httpQuery.execConstruct(ModelFactory.createDefaultModel());
		ResIterator RDFResources = getRDFResources(model);
		while (RDFResources.hasNext()){
			Resource subject = RDFResources.next();
			String externalURI = subject.getProperty(sameAs).getString();
			if (!choices.containsKey(externalURI)) {
				choices.put(externalURI, this.extractChoice(subject));				
			}else {
				Statement broader = subject.getProperty(skosBroader);
				if (broader != null) {
					String fatherLabel = broader.getResource().getProperty(prefLabel).getString();
					if (!fatherLabel.isEmpty()) {
						String label = choices.get(externalURI).label;
						label = label.substring(0, label.length() -1) + ", " + fatherLabel + ")";
						choices.get(externalURI).label = label;
					}					
				}
			}
		};
	    

		Collection<Choice> values = this.sortByComparator(choices).values();
		
		return values.toArray(new Choice[0]);
	}

	

    private HashMap<String, Choice> sortByComparator(HashMap<String, Choice> unsortMap)
    {

        List<Entry<String, Choice>> list = new LinkedList<Entry<String, Choice>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Entry<String, Choice>>(){
            public int compare(Entry<String, Choice> o1, Entry<String, Choice> o2) {
                    return o1.getValue().label.compareTo(o2.getValue().label);
            }
        }
        );

        HashMap<String, Choice> sortedMap = new LinkedHashMap<String, Choice>();
        for (Entry<String, Choice> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
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

		pqs.setNsPrefix("owl", NS_OWL);
		pqs.setNsPrefix("rdf", NS_RDF);
		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("sedici", NS_SEDICI);

		pqs.setCommandText("CONSTRUCT {"
				+ "?concept a sedici:Subject . \n" + 
				"  ?concept skos:prefLabel ?label . \n" + 
				"  ?concept owl:sameAs ?externalURI. \n" + 
				"}\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a sedici:Subject . \n" + 
				" ?concept skos:prefLabel ?label . \n" + 
				" ?concept owl:sameAs ?externalURI. \n");
		pqs.append("FILTER(REGEX(?externalURI, ?key, \"i\")) \n");
		pqs.append("}\n");
		pqs.append("ORDER BY ?label \n");

		pqs.setLiteral("key", key);
		return pqs;
	}

	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		pqs.setNsPrefix("owl", NS_OWL);
		pqs.setNsPrefix("rdf", NS_RDF);
		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("sedici", NS_SEDICI);

		pqs.setCommandText("CONSTRUCT { "
				+ "?concept a sedici:Subject .\n" + 
				"?concept skos:prefLabel ?label .\n" + 
				"?concept owl:sameAs ?externalURI.\n" + 
				"?concept skos:broader ?parent .\n" + 
				"?parent skos:prefLabel ?parentLabel .\n" + 
				"?parent owl:sameAs ?parentURI.\n" + 
				"\n}");
		pqs.append("WHERE {\n");
		pqs.append("?concept a sedici:Subject .\n" + 
				"?concept skos:prefLabel ?label .\n" + 
				"?concept owl:sameAs ?externalURI.\n" + 
				"?concept skos:broader ?parent .\n" + 
				"?parent skos:prefLabel ?parentLabel .\n" + 
				"?parent owl:sameAs ?parentURI.\n" + 
				" \n");
		if (!"".equals(text)) {
			text = normalizeTextForParserSPARQL10(text);
			String[] tokens = text.split(",");
			if (tokens.length > 1 && tokens[0].trim().length() > 0 && tokens[1].trim().length() > 0) {
				pqs.append("FILTER(REGEX(?concept, ?text2, \"i\") && REGEX(?label, ?text1, \"i\"))\n");
				pqs.setLiteral("text1", tokens[0].trim());
				pqs.setLiteral("text2", "^" + tokens[1].trim());
			} else {
				pqs.append("FILTER(REGEX(?concept, ?text, \"i\") || REGEX(?label, ?text, \"i\"))\n");
				pqs.setLiteral("text", tokens[0]);
			}
		}
		pqs.append("}\n");
		pqs.append("ORDER BY ?label ?parentLabel\n");
		
		return pqs;
	}

	protected String getSparqlEndpoint() {
		String endpoint = ConfigurationManager.getProperty("sparql-authorities", "vocabularios-authorities.endpoint.url");
		if (endpoint != null) {
			return endpoint;
		} else {
			throw new NullPointerException("Missing endpoint configuration.");
		}
	}

}