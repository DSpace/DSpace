/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

public interface ChoiceAuthorityDetails {
    /**
	 * Get an html description for the authority value. It is indeed to use in
	 * submission and public page to provide additional information about the
	 * authority value.
	 * 
	 * @param field
	 * @param key
	 * @param locale
	 * @return
	 */
    public Object getDetailsInfo(String field, String key, String locale);
}
