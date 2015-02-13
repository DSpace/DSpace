package org.dspace.content.authority;

/**
 * This authority will be used if you want to use and edit the authority hidden inputs,
 * without using a set of authorities predefined in some place. In combination
 * with the 'raw' presentation mode (in the 'dspace.cfg'), use this class to
 * modify 'at hand' the authority value input during the submission.
 * @author facundo@sedici.unlp.edu.ar
 */
public class EmptyAuthority implements ChoiceAuthority {

	/**
	 * This method returns an empty Choices for every value of 'text' received.
	 * @return empty Choices.
	 */
	@Override
	public Choices getMatches(String field, String text, int collection,
			int start, int limit, String locale) {
		
		return new Choices(Choices.CF_UNSET);
	}

	@Override
	public Choices getBestMatch(String field, String text, int collection,
			String locale) {
		return getMatches(field, text, collection, 0, 0, locale);
	}

	/**
	 * @return an empty label.
	 */
	public String getLabel(String field, String key, String locale) {
		
		return new String();
	}

}
