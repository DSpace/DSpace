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

import java.util.List;

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.edu.unlp.sedici.sedici2003.service.SeDiCI2003Manager;


public abstract class SeDiCI2003AuthorityProvider implements ChoiceAuthority {

	private Logger log =LoggerFactory.getLogger(getClass()); 
	
	public SeDiCI2003AuthorityProvider() {
		SeDiCI2003Manager.prepare();
	}
	
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
    	
    	//FIXME Ver de donde viene el limit
    	//Hay que tener cuidado con el limite cuando es 0, ya que por otro lado se esta cargando con 20 si el limite es menos o igual a 0
    	if((limit > 100) || (limit <= 0)) limit = 100;

		int total=this.findSeDiCI2003EntitiesCount(field, text);
		
    	List<Choice> entities = this.findSeDiCI2003Entities(field, text, start, limit, new ChoiceFactory(field));

    	
    	int confidence;
    	if (entities.size() == 0)
            confidence = Choices.CF_NOTFOUND;
        else if (entities.size() == 1)
            confidence = Choices.CF_UNCERTAIN;
        else
            confidence = Choices.CF_AMBIGUOUS;
    	
    	return new Choices(entities.toArray(new Choice[entities.size()]), start, entities.size(), confidence, (total > (start + limit)));
	}

    protected abstract List<Choice> findSeDiCI2003Entities(String field, String text, int start, int limit, ChoiceFactory choiceFactory) ;
    protected abstract int findSeDiCI2003EntitiesCount(String field, String text) ;
    protected abstract String getSeDiCI2003EntityLabel(String field, String key) ;
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
		return getSeDiCI2003EntityLabel(field, key);
	}

    static class ChoiceFactory{
    	private String field;
    	
    	protected ChoiceFactory(String field){
    		this.field = field;
    	}
    	
		public Choice createChoice(String id, String value, String label){
			return new Choice(id, value, label);
		}
		
    }
	
}
