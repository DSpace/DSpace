/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.dspace.authority;

import java.util.Enumeration;
import java.util.List;

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

import com.sun.syndication.feed.atom.Entry;

public abstract class SeDiCI2003AuthorityProvider implements ChoiceAuthority {

	
    /**
     * Get all values from the authority that match the profferred value.
     * Note that the offering was entered by the user and may contain
     * mixed/incorrect case, whitespace, etc so the plugin should be careful
     * to clean up user data before making comparisons.
     *
     * Value of a "Name" field will be in canonical DSpace person name format,
     * which is "Lastname, Firstname(s)", e.g. "Smith, John Q.".
     *
     * Some authorities with a small set of values may simply return the whole
     * set for any sample value, although it's a good idea to set the
     * defaultSelected index in the Choices instance to the choice, if any,
     * that matches the value.
     *
     * @param field being matched for
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param start choice at which to start, 0 is first.
     * @param limit maximum number of choices to return, 0 for no limit.
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null).
     */
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale) {
    	if (text == null) text = "";
    	List<Choice> entities = this.findSeDiCI2003Entities(text, start, limit, new ChoiceFactory(field));
		
    	int confidence;
    	if (entities.size() == 0)
            confidence = Choices.CF_NOTFOUND;
        else if (entities.size() == 1)
            confidence = Choices.CF_UNCERTAIN;
        else
            confidence = Choices.CF_AMBIGUOUS;
    	Choice[] c = null;
    	
    	return new Choices(entities.toArray(c), start, entities.size(), confidence, (entities.size() <= limit));
	}

    protected abstract List<Choice> findSeDiCI2003Entities(String text, int start, int limit, ChoiceFactory choiceFactory) ;
    protected abstract String getSeDiCI2003EntityLabel(String key) ;
	/**
     * Get the single "best" match (if any) of a value in the authority
     * to the given user value.  The "confidence" element of Choices is
     * expected to be set to a meaningful value about the circumstances of
     * this match.
     *
     * This call is typically used in non-interactive metadata ingest
     * where there is no interactive agent to choose from among options.
     *
     * @param field being matched for
     * @param text user's value to match
     * @param collection database ID of Collection for context (owner of Item)
     * @param locale explicit localization key if available, or null
     * @return a Choices object (never null) with 1 or 0 values.
     */
    public Choices getBestMatch(String field, String text, int collection, String locale) {
    	return this.getMatches(field, text, collection, 0, 1, locale);
	}

    /**
     * Get the canonical user-visible "label" (i.e. short descriptive text)
     * for a key in the authority.  Can be localized given the implicit
     * or explicit locale specification[
     *
     * This may get called many times while populating a Web page so it should
     * be implemented as efficiently as possible.
     *
     * @param field being matched for     
     * @param key authority key known to this authority.
     * @param locale explicit localization key if available, or null
     * @return descriptive label - should always return something, never null.
     */
    public String getLabel(String field, String key, String locale) {
		return getSeDiCI2003EntityLabel(key);
	}

     static class ChoiceFactory{
    	private String authority;
    	protected ChoiceFactory(String authority){
    		this.authority = authority;
    	}
		public Choice createChoice(int id, String label){
			return new Choice(this.authority, String.valueOf(id), label);
		}
    }
	
}
