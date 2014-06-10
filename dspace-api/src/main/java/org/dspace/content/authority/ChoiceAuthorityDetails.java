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
