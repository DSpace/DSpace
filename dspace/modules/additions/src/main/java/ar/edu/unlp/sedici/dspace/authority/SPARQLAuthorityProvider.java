package ar.edu.unlp.sedici.dspace.authority;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.Syntax;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

public abstract class SPARQLAuthorityProvider implements ChoiceAuthority {

    protected static Logger log = Logger.getLogger(SPARQLAuthorityProvider.class);
    
	protected static final String NS_RDFS = "http://www.w3.org/2000/01/rdf-schema#";
	protected static final String NS_SKOS = "http://www.w3.org/2004/02/skos/core#";
	

	private String sparqlEndpoint;
	private QuerySolutionMap globalParameters;

	public SPARQLAuthorityProvider(String sparqlEndpoint) {
		this(sparqlEndpoint, new QuerySolutionMap());
	}

	public SPARQLAuthorityProvider(String sparqlEndpoint,
			QuerySolutionMap globalParameters) {
		log.trace("New SPARQLAuthorityProvider created por endpoint: " + sparqlEndpoint);
		this.sparqlEndpoint = sparqlEndpoint;
		this.globalParameters = globalParameters;
	}

	@Override
	public Choices getMatches(String field, String text, int collection,
			int start, int limit, String locale) {
		if (text == null)
			text = "";

		ParameterizedSparqlString query = this.getSparqlSearchByTextQuery(
				field, text, locale);
		Choice[] choices = this.evalSparql(query, start, limit);
		log.trace(choices.length + "matches found for text " + text);
		return new Choices(choices, start, limit, Choices.CF_ACCEPTED, false);
		// TODO hasMore??
	}
	
	@Override
	public final Choices getBestMatch(String field, String text,
			int collection, String locale) {
		return this.getMatches(field, text, collection, 0, 1, locale);
	}

	@Override
	public String getLabel(String field, String key, String locale) {

		ParameterizedSparqlString query = this.getSparqlSearchByIdQuery(field,
				key, locale);
		Choice[] choices = this.evalSparql(query, 0, 1);
		if (choices.length ==0 )
			return null;
		else 
			return choices[0].label;
	}

	
	protected abstract ParameterizedSparqlString getSparqlSearchByIdQuery(
			String field, String key, String locale);

	protected abstract ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale);

	protected abstract Choice extractChoice(QuerySolution solution);

	private Choice[] evalSparql(
			ParameterizedSparqlString parameterizedSparqlString, int offset,
			int limit) {

		parameterizedSparqlString.setParams(globalParameters);
		Query query = QueryFactory.create(parameterizedSparqlString.toString(), this.getSPARQLSyntax());
		query.setOffset(offset);

		if (limit == 0)
			query.setLimit(Query.NOLIMIT);
		else
			query.setLimit(limit);
		long inicio = System.currentTimeMillis();
		if (log.isDebugEnabled()){ 
			log.debug("Excecuting SparqlQuery "+query.toString(this.getSPARQLSyntax()));
		}

		QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint, query);
		Choice[] choices = this.extractChoices(httpQuery.execSelect());
		httpQuery.close();

		if (log.isDebugEnabled()){ 
			log.debug("El query tardó "+(System.currentTimeMillis() - inicio) + "ms");
		}
		return choices;
	}

	private Syntax getSPARQLSyntax() {
		return Syntax.syntaxSPARQL_10;
	}

	private Choice[] extractChoices(ResultSet results) {
		List<Choice> choices = new LinkedList<Choice>();
		while (results.hasNext()) {
			QuerySolution solution = results.next();
			choices.add(this.extractChoice(solution));
		}
		return choices.toArray(new Choice[0]);
	}

	
	public static void main(String[] args) {
	
		log.addAppender(new WriterAppender(new SimpleLayout(), System.out));
		log.setLevel(Level.TRACE);
		SPARQLAuthorityProvider s = new SPARQLAuthorityProvider(
				"http://www.ebi.ac.uk/rdf/services/atlas/sparql") {

			@Override
			protected Choice extractChoice(QuerySolution solution) {
				String expressionValue = solution.getResource("experiment").getURI();
				String pValue = solution.getLiteral("description").getString();
				// print the output to stdout
				return new Choice("0", pValue, expressionValue + "\t" + pValue);
			}

			@Override
			protected ParameterizedSparqlString getSparqlSearchByIdQuery(
					String field, String key, String locale) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			protected ParameterizedSparqlString getSparqlSearchByTextQuery(
					String field, String text, String locale) {
				ParameterizedSparqlString pqs = new ParameterizedSparqlString();
				// pss.setBaseUri("http://example.org/base#");
				pqs.setNsPrefix("atlasterms",
						"http://rdf.ebi.ac.uk/terms/atlas/");
					pqs.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
				pqs.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
				pqs.setCommandText("SELECT DISTINCT ?experiment ?description \n");
				pqs.append("WHERE { \n");
					pqs.append("?experiment a atlasterms:Experiment .");
					pqs.append("?experiment dcterms:description ?description .");
				pqs.append("FILTER regex(?description, ?text, \"i\")");
				pqs.append("} \n");
				pqs.append("ORDER BY ASC(?description)");
				pqs.setLiteral("text", text);
				return pqs;

			}
		};
		
		Choices cs = s.getMatches("dc.title", "some",0, 0, 10, "en");
		for (Choice c : cs.values) {
			System.out.println("AUTHORITY="+c.authority+",LABEL="+c.label+",VALUE="+c.value);
		}
	}
}
