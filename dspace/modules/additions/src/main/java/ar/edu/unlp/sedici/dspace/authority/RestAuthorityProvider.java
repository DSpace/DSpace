package ar.edu.unlp.sedici.dspace.authority;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.dspace.content.Collection;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.json.JSONArray;

public abstract class RestAuthorityProvider implements ChoiceAuthority {

	private final String CHOICES_ENDPOINT_PATH_PREFIX;

	private final String CHOICES_FILTER_FIELD_PREFIX;

	private final String CHOICES_ID_FIELD_PREFIX;

	private final String CHOICES_AUTH_KEY_PREFIX;

	private final String ID_FIELD;

	private final String FILTER_FIELD;

	private final String AUTH_KEY_PREFIX_FILTER;

	protected ConfigurationService configurationService;

	public RestAuthorityProvider() {
		// Set config properties name prefix
		this.CHOICES_ENDPOINT_PATH_PREFIX = "choices.endpointPath.";
		this.CHOICES_FILTER_FIELD_PREFIX = "choices.filterField.";
		this.CHOICES_ID_FIELD_PREFIX = "choices.idField.";
		this.CHOICES_AUTH_KEY_PREFIX = "choices.authKeyPrefix.";
		// Default value for id field
		this.ID_FIELD = "auth_key";
		// Default value for text filter field
		this.FILTER_FIELD = "title";
		this.AUTH_KEY_PREFIX_FILTER = "";
		this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
	}

	/**
	 *
	 * @param field metadata field responsible for the query
	 * @return the path to the specific endpoint aimed by our query
	 */
	private String getPath(String field) {
		String metadataField = field.replace("_", ".");
		// Gets the value from conf file if setted, else uses default value
		String path = configurationService.getProperty(CHOICES_ENDPOINT_PATH_PREFIX + metadataField, "");
		return path;
	};

	/**
	 *
	 * @param field metadata field responsible for the query
	 * @return the attribute we are going to filter by
	 */
	protected final String getFilterField(String field) {
		String metadataField = field.replace("_", ".");
		// Gets the value from conf file if setted, else uses default value
		String filterField = configurationService.getProperty(CHOICES_FILTER_FIELD_PREFIX + metadataField,
				this.FILTER_FIELD);
		return filterField;
	}

	/**
	 *
	 * @param field metadata field responsible for the query
	 * @return the attribute containing the key by which we will do exact match
	 *         filtering
	 */
	protected final String getIdField(String field) {
		String metadataField = field.replace("_", ".");
		// Gets the value from conf file if set, else uses default value
		String idField = configurationService.getProperty(CHOICES_ID_FIELD_PREFIX + metadataField, this.ID_FIELD);
		return idField;
	};

	/**
    *
    * @param field metadata field responsible for the query
    * @return prefix applied to authority value when query by the ID_FIELD.
    *         filtering
    */
   protected final String getAuthKeyPrefixFilter(String field) {
       String metadataField = field.replace("_", ".");
       // Gets the value from conf file if set, else uses default value
       String authKeyPrefix = configurationService.getProperty(CHOICES_AUTH_KEY_PREFIX + metadataField, this.AUTH_KEY_PREFIX_FILTER);
       return authKeyPrefix;
   };

	/**
	 *
	 * @param field metadata field responsible for the query
	 * @param singleResult one of the results returned by the query as a Map
	 * @param searchById specify if searching by authority key (ID)
	 * @return a Choice object made from de json object result
	 */
	protected abstract Choice extractChoice(String field, Map<String, Object> singleResult, boolean searchById);

	private Choice[] extractChoicesfromQuery(String field, JSONArray response, boolean searchById) {
		List<Choice> choices = new LinkedList<Choice>();
		for (int i = 0; i < response.length(); i++) {
			Map<String, Object> singleResult = response.getJSONObject(i).toMap();
			choices.add(this.extractChoice(field, singleResult, searchById));
		}
		return choices.toArray(new Choice[0]);
	}

	private Choice[] doChoicesQuery(String field, HashMap<String, String> params, boolean searchById) {
		String path = getPath(field);
		JSONArray response = RestAuthorityConnector.executeGetRequest(path, params);
		return extractChoicesfromQuery(field, response, searchById);
	}

	private Choice[] doChoicesIdQuery(String field, String key) {
		String idField = getIdField(field);
		String authKeyPrefix = this.getAuthKeyPrefixFilter(field);
		HashMap<String, String> params = new HashMap<String, String>();
		if (!authKeyPrefix.isEmpty()) {
		    key = key.replace(authKeyPrefix, "");
		}
		params.put(idField, key);
		return doChoicesQuery(field, params, true);
	}

	private Choice[] doChoicesTextQuery(String field, String filter) {
		String filterField = getFilterField(field);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put(filterField, filter);
		this.addExtraQueryTextParams(field, params);
		return doChoicesQuery(field, params, false);
	}

    @Override
	public final Choices getMatches(String field, String text, Collection collection, int start, int limit,
			String locale) {
		Choice[] choices = this.doChoicesTextQuery(field, text);
		return new Choices(choices, start, limit, Choices.CF_ACCEPTED, false);
	}

	@Override
	public final Choices getBestMatch(String field, String text, Collection collection, String locale) {
		return this.getMatches(field, text, collection, 0, 1, locale);
	}

	@Override
	public final String getLabel(String field, String key, String locale) {
		Choice[] choices = this.doChoicesIdQuery(field, key);
		if (choices.length == 0)
			return null;
		else
			return choices[0].label;
	}

	protected abstract void addExtraQueryTextParams(String field, Map<String, String> params);

}
