package ar.gob.gba.cic.digital;

import org.apache.log4j.Level;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.WriterAppender;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;

import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.QuerySolution;

public class FOS_CICBA_Authority extends CICBAAuthority{
	
	@Override
	protected ParameterizedSparqlString getSparqlSearchByIdQuery(String field,
			String key, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		//pss.setBaseUri("http://example.org/base#");
		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("cic", NS_CIC);
		
		pqs.setCommandText("SELECT ?concept ?label ?affiliation\n");
		pqs.append("WHERE {\n");
		pqs.append("?concept a skos:Concept ; a cic:Materia ; skos:prefLabel ?label .\n");
			pqs.append("FILTER(REGEX(?concept, ?key, \"i\"))\n");
		pqs.append("}\n");	
//		pqs.append("ORDER BY ASC(?label)\n");
		
		pqs.setLiteral("key", key);
		return pqs;
	}


	@Override
	protected ParameterizedSparqlString getSparqlSearchByTextQuery(
			String field, String text, String locale) {
		ParameterizedSparqlString pqs = new ParameterizedSparqlString();

		//pss.setBaseUri("http://example.org/base#");
		pqs.setNsPrefix("skos", NS_SKOS);
		pqs.setNsPrefix("cic", NS_CIC);
		
		pqs.setCommandText("SELECT ?concept ?label\n");
		pqs.append("WHERE {\n");
			pqs.append("?concept a skos:Concept ; a cic:Materia ; skos:prefLabel ?label .\n");
			pqs.append("FILTER(REGEX(?label, ?text, \"i\"))\n");
		pqs.append("}\n");	
		pqs.append("ORDER BY ASC(?label)\n");
		
		pqs.setLiteral("text", text);
		return pqs;
	}

	@Override
	protected Choice extractChoice(QuerySolution solution) {
		String key = solution.getResource("concept").getURI();
		String label = solution.getLiteral("label").getString();
		return new Choice(key, label, label + "("+ key+")");
	}
	
	public static void main(String[] args) {
	
		log.addAppender(new WriterAppender(new SimpleLayout(), System.out));
		log.setLevel(Level.TRACE);
		CICBAAuthority ca = new FOS_CICBA_Authority();
		for (int i = 0; i < 3; i++) {
			long l = System.currentTimeMillis();
			Choices cs = ca.getMatches("dc.subject", "Humanidades",0, 0, 10, "en");
			for (Choice c : cs.values) {
				System.out.println("AUTHORITY="+c.authority+",LABEL="+c.label+",VALUE="+c.value);
			}
			System.out.println("Tardo "+(System.currentTimeMillis() - l)+ "MS");
			
			
			l = System.currentTimeMillis();
			String label = ca.getLabel("dc.subject", "http://cicdigital.sedici.unlp.edu.ar/auth/taxonomy/term/67168", "en");
			System.out.println("LABEL="+label);
			System.out.println("Tardo "+(System.currentTimeMillis() - l)+ "MS");
			
		}
	}


}
