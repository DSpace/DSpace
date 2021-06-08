package ar.edu.unlp.sedici.dspace.authority;

import java.util.Map;

import org.dspace.content.authority.Choice;

public class AuthorRestAuthorityProvider extends RestAuthorityProvider {

    private final String FILIACION_NAME_FIELD;
    private final String FILIACION_ACRONYM_FIELD;
    
    public AuthorRestAuthorityProvider() {
        super();
        this.FILIACION_NAME_FIELD = "institution_title";
        this.FILIACION_ACRONYM_FIELD = "institution_acronym";
    }
    
    @Override
    protected Choice extractChoice(String field, Map<String, Object> singleResult, boolean searchById) {
        String value = (String) singleResult.get(this.getFilterField(field));
        String key = (String) singleResult.get(this.getIdField(field));
        String label = value;
        // If searching by id (in example, if indexing using Discovery, then don't show acronym)...
        if (!searchById) {
			if (singleResult.containsKey(FILIACION_ACRONYM_FIELD)
					&& !singleResult.get(FILIACION_ACRONYM_FIELD).toString().isEmpty()) {
				label += " (" + singleResult.get(FILIACION_ACRONYM_FIELD) + ")";
			} else if (singleResult.containsKey(FILIACION_NAME_FIELD)
					&& !singleResult.get(FILIACION_NAME_FIELD).toString().isEmpty()) {
				label += " (" + singleResult.get(FILIACION_NAME_FIELD) + ")";
			}
        }
        return new Choice(key, value, label);
    }

    @Override
    protected void addExtraQueryTextParams(String field, Map<String, String> params) {
        // TODO Auto-generated method stub
    }

}
