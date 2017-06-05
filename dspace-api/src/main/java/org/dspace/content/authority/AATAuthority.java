/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;


/**
 * Implementation to lookup value from The Art & Architecture Thesaurus (AAT),
 * 
 * @see https://www.getty.edu/research/tools/vocabularies/aat/index.html
 *  
 * @author Riccardo Fazio (riccardo.fazio at 4science dot it)
 */
public class AATAuthority extends GettyAuthority {

	String query ="SELECT ?Subject ?Term ?Parents ?ScopeNote { ?Subject luc:term \"%s\"; skos:inScheme aat: ; gvp:prefLabelGVP [skosxl:literalForm ?Term; gvp:term ?pureTerm]. optional {?Subject gvp:parentStringAbbrev ?Parents} optional {?Subject skos:scopeNote [dct:language gvp_lang:en; rdf:value ?ScopeNote]}} ORDER BY ASC(LCASE(STR(?pureTerm)))";
	
	@Override
	public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
		String sparQL = String.format(query, text);
		Choices results = query(sparQL);
		return results;
	}

}
