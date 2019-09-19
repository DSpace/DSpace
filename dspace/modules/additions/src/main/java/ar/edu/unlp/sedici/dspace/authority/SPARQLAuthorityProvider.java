package ar.edu.unlp.sedici.dspace.authority;

import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public abstract class SPARQLAuthorityProvider implements ChoiceAuthority {

	protected static Logger log = Logger
			.getLogger(SPARQLAuthorityProvider.class);

	protected static final String NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	protected static final String NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	protected static final String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";
	protected static final String NS_FOAF = "http://xmlns.com/foaf/0.1/";
	protected static final String NS_DC = "http://purl.org/dc/terms/";
	protected static final String NS_SIOC = "http://rdfs.org/sioc/ns#";
	protected static final String NS_CERIF = "http://spi-fm.uca.es/neologism/cerif/1.3#";
	protected static final String NS_SCHEMA = "http://schema.org/";
	protected static final String NS_SEDICI = "http://sedici.unlp.edu.ar/";
	protected static final String NS_OWL= "http://www.w3.org/2002/07/owl#";
	
	private QuerySolutionMap globalParameters;

	public SPARQLAuthorityProvider() {
		this(new QuerySolutionMap());
	}

	public SPARQLAuthorityProvider(QuerySolutionMap globalParameters) {
		this.globalParameters = globalParameters;
	}
	
	protected String getSparqlEndpoint() {
		String endpoint = ConfigurationManager.getProperty("sparql-authorities", "sparql-authorities.endpoint.url");
		if (endpoint != null) {
			return endpoint;
		} else {
			throw new NullPointerException("Missing endpoint configuration.");
		}
	}

	@Override
	public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
		if (text == null)
			text = "";
		else 
			text = text.replace("\"", "");

		ParameterizedSparqlString query = this.getSparqlSearch(
				field, text, locale, false);
		Choice[] choices = this.evalSparql(query, start, limit);
		log.trace(choices.length + "matches found for text " + text);
		return new Choices(choices, start, limit, Choices.CF_ACCEPTED, false);
		// TODO hasMore??
	}

	/*
	 * Busca en la base de autoridades choices (authorities) a partir de un string y
	 * retorna el primero cuyo value o label coincida con el string buscado
	 */
	@Override
	public final Choices getBestMatch(String field, String text, int collection, String locale) {
		Choices matches = this.getMatches(field, text, collection, 0, 0, locale);
		for (Choice match : matches.values) {
			// Con que el label o el value sea igual al valor buscado ya es suficiente
			if (text.trim().equalsIgnoreCase(match.value.trim()) || text.trim().equalsIgnoreCase(match.label.trim())) {
				Choice[] bestMatch = new Choice[1];
				bestMatch[0] = match;
				// Devuelvo el choice con confidence 500 porque no fue seteado por un humano
				return new Choices(bestMatch, 0, 1, Choices.CF_UNCERTAIN, false);
			}
		}
		// Si no se encontró ningún valor que coincida, devolvemos un array vacío de choices y confidence 300
		return new Choices(new Choice[0], 0, 1, Choices.CF_NOTFOUND, false);
	}

	@Override
	public String getLabel(String field, String key, String locale) {

		ParameterizedSparqlString query = this.getSparqlSearch(field,
				key, locale, true);
		Choice[] choices = this.evalSparql(query, 0,0);
		if (choices.length == 0)
			return null;
		else
			return choices[0].label;
	}

	protected abstract ParameterizedSparqlString getSparqlSearch(String field, String filter, String locale,boolean idSearch);

	protected abstract Choice[] extractChoicesfromQuery(QueryEngineHTTP httpQuery);

	protected Choice[] evalSparql(
			ParameterizedSparqlString parameterizedSparqlString, int offset,
			int limit) {

		parameterizedSparqlString.setParams(globalParameters);

		Query query = QueryFactory.create(normalizeTextForHttpQuery(parameterizedSparqlString.toString()),
				this.getSPARQLSyntax());
		query.setOffset(offset);

		if (limit == 0)
			query.setLimit(Query.NOLIMIT);
		else
			query.setLimit(limit);
		long inicio = System.currentTimeMillis();
		if (log.isDebugEnabled()) {
			log.debug("Excecuting SparqlQuery "
					+ query.toString(this.getSPARQLSyntax()));
		}

		QueryEngineHTTP httpQuery = new QueryEngineHTTP(this.getSparqlEndpoint(), query);
		httpQuery.setAllowDeflate(false);
		httpQuery.setAllowGZip(false);
		// TODO pull down de extractChocicesFromQuery a una nueva clase que haga un execSelect o un execConstruct
		Choice[] choices = extractChoicesfromQuery(httpQuery);
		httpQuery.close();

		if (log.isDebugEnabled()) {
			log.debug("El query tardó " + (System.currentTimeMillis() - inicio)
					+ "ms");
		}
		return choices;
	}

	private String normalizeTextForHttpQuery(String query) {
		if (query.indexOf("\\(") >= 0) {
			query = query.replace("\\(", "\\\\\\(");
		}
		if (query.indexOf("\\)") >= 0) {
			query = query.replace("\\)", "\\\\\\)");
		}
		return query;
	}

	protected String normalizeTextForParserSPARQL10(String text) {
		if (text.indexOf("(") >= 0) {
			text = text.replace("(", "\\\\(");
		}
		if (text.indexOf(")") >= 0) {
			text = text.replace(")", "\\\\)");
		}
		return text;
	}


	private Syntax getSPARQLSyntax() {
		// FIXME: la sintaxis debería ser protected
		return Syntax.syntaxSPARQL_10;
	}
	
	protected Choice[] choicesListToArraySorted(List<Choice> choices) {
		choices.sort(new Comparator<Choice>() {
		    @Override
		    public int compare(Choice m1, Choice m2) {
		        if(m1.label == m2.label){
		            return 0;
		        }
		        return m1.label.compareTo(m2.label) < 0 ? -1 : 1;
		     }
		});
		return choices.toArray(new Choice[0]);
	}




}