package ar.edu.unlp.sedici.dspace.content.authority;

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.DCInputAuthority;

/**
 * This class simply modifies the attribute value of every Choice returned
 * by the DCInputAuthority. It copy the value of the Choice.label in the
 * Choice.value.
 * @author facundo
 *
 */
public class CICBADCInputAuthority extends DCInputAuthority {

	public CICBADCInputAuthority(){
		super();
	}
	
	public Choices getBestMatch(String field, String text, int collection, String locale)
	{
		Choices choices = super.getBestMatch(field, text, collection, locale);
		for(Choice c : choices.values){
			copyLabelValue(c);
		}
		return choices;
	}
	
	public Choices getMatches(String field, String query, int collection, int start, int limit, String locale){
		Choices choices = super.getMatches(field, query, collection, start, limit, locale);
		for(Choice c : choices.values){
			copyLabelValue(c);
		}
		return choices;
	}
	
	private void copyLabelValue(Choice choice){
		choice.value = choice.label;
	}
	
}
