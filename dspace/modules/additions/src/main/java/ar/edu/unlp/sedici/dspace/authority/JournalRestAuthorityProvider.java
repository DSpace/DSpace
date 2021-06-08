package ar.edu.unlp.sedici.dspace.authority;

import java.util.Map;

import org.dspace.content.authority.Choice;

public class JournalRestAuthorityProvider extends RestAuthorityProvider {

    @Override
    protected Choice extractChoice(String field, Map<String, Object> singleResult, boolean searchById) {
        String value = (String) singleResult.get(this.getFilterField(field));
        String key = (String) singleResult.get(this.getIdField(field));
        return new Choice(key, value, value);
    }

    @Override
    protected void addExtraQueryTextParams(String field, Map<String, String> params) {
        // TODO Auto-generated method stub

    }

}
