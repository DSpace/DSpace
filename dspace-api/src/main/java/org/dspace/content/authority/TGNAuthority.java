/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.content.Collection;

/**
 * Implementation to lookup value from Getty Thesaurus of Geographic Names® Online
 * https://www.getty.edu/research/tools/vocabularies/tgn/index.html
 *  
 * @author Riccardo Fazio (riccardo.fazio at 4science.it)
 *
 */
public class TGNAuthority extends GettyAuthority {

	String query ="SELECT ?Subject ?Term ?Parents ?ScopeNote ?Type ?long ?lat{?Subject luc:term \"%s\"; a ?typ. ?typ rdfs:subClassOf gvp:Subject; rdfs:label ?Type; rdfs:label \"Subject\";. ?Subject skos:inScheme ?vocab. ?vocab vann:preferredNamespacePrefix \"tgn\". ?Subject gvp:placeType <http://vocab.getty.edu/aat/300008347>. optional {?Subject gvp:prefLabelGVP [skosxl:literalForm ?Term]} optional {?Subject gvp:parentString ?Parents} optional {?Subject foaf:focus [wgs:lat ?lat; wgs:long ?long]} optional {?Subject skos:scopeNote [dct:language gvp_lang:en; skosxl:literalForm ?ScopeNote]}} order by ASC(?Term)";
	
	@Override
	public Choices getMatches(String field, String text, Collection collection, int start, int limit, String locale) {

    /* PMB: Insert "AND" between terms so that queries like "Los Angeles" work better */

    String searchText = text.replace(" ", " AND ");

		String sparQL = String.format(query, searchText);
		Choices results = query(sparQL);
		return results;
	}


}
