package org.datadryad.authority.hive;

import java.io.InputStream;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Generic HIVE choice authority that uses the HIVE auto-suggest feature. This 
 * choice authority is primarily used by the automatic metadata extraction (AME)
 * aspect. The dspace.cfg must contain the ame.host (HIVE server hostname)
 * and ame.field_name.cv (HIVE vocabulary/ies used for a particular field.
 * properties.
 * 
 * @author craig.willis@unc.edu
 */
public class HIVESubjectAuthority implements ChoiceAuthority {
	private static final Logger log = Logger.getLogger(HIVESubjectAuthority.class);
	
	@Override
	public Choices getMatches(String field, String text, int collection, int start, int limit,
			String locale) 
	{
		Choices choices = new Choices(true);
		
		try
		{
			// Get the vocabulary used for this field
			String cv = ConfigurationManager.getProperty("ame." + field + ".cv");
			
			// Get the HIVE server base URL, including protocol
			String host = ConfigurationManager.getProperty("ame.host");
			
			text = URLEncoder.encode(text, "UTF-8");
			
			// URL to HIVE auto-suggest servlet, returns JSON data
			String u = host + "/suggest?cv=" + cv + "&tx=" + text + "&mp=1&fmt=list";

			URL url = new URL(u);
			InputStream is = url.openStream();
			JSONTokener jt = new JSONTokener(is);
			
			// Construct the choice list from the JSON data
			List<Choice> cs = new ArrayList<Choice>();
			JSONArray array = new JSONArray(jt);
			for (int i = 0; i<array.length(); i++)
			{
				try
				{
					JSONObject o = array.getJSONObject(i);
					
					String id = o.getString("key");
					String label = o.getString("title");
					String value = o.getString("title");
					Choice c = new Choice(id, label, value);
					cs.add(c);
				} catch  (JSONException e) {
					log.error("Exception: error parsing HIVE JSON", e);
				}
			}
			
			int confidence;
			if (cs.size() == 0)
				confidence = Choices.CF_NOTFOUND;
			if (cs.size() == 1) 
				confidence = Choices.CF_UNCERTAIN;
			else
				confidence = Choices.CF_AMBIGUOUS;
			
			choices = new Choices(cs.toArray(new Choice[0]), start, cs.size(), confidence, false); 
		
		} catch (Exception e) {
			log.error("Exception: error processing HIVE response", e);
		}
		return choices;
	}

	@Override
	public Choices getBestMatch(String field, String text, int collection,
			String locale) {
		return getMatches(field, text, collection, 0, 2, locale);
	}

	@Override
	public String getLabel(String field, String key, String locale) {
		return key;
	}

}
