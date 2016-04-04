package org.datadryad.authority;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.JournalUtils;
import java.sql.SQLException;
import java.util.*;
import org.datadryad.api.DryadJournalConcept;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 1, 2011
 * Time: 2:42:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileIndexer implements AuthorityIndexerInterface {
    private AuthorityValue nextValue;

    LinkedList<AuthorityValue> authorities = new LinkedList<AuthorityValue>();

    Context context;

    public void init() {
        DryadJournalConcept[] journalConcepts = JournalUtils.getAllJournalConcepts();
        for (DryadJournalConcept journalConcept : journalConcepts) {
            AuthorityValue authorityValue = createAuthorityValue(journalConcept);
            authorities.push(authorityValue);
        }
    }

    @Override
    public void init(Context context, Item item) {
        //To change body of implemented methods use File | Settings | File Templates.
        init();
    }

    @Override
    public void init(Context context) {
        //To change body of implemented methods use File | Settings | File Templates.
        init();
    }

    @Override
    public AuthorityValue nextValue() {
        return nextValue;
    }

    @Override
    public boolean hasMore() {
        try {

            if(authorities.size() == 0) nextValue = null;

            else nextValue = authorities.poll();

            return(nextValue != null);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    private AuthorityValue createAuthorityValue(DryadJournalConcept journalConcept) {
        String value = journalConcept.getFullName();

        AuthorityValue authorityValue = new AuthorityValue();

        authorityValue.setId(Utils.getMD5(value));
        authorityValue.setField("prism.publicationName");
        authorityValue.setValue(value);
        authorityValue.setFullText(value);
        authorityValue.setCreationDate(new Date());
        authorityValue.setLastModified(new Date());

        return authorityValue;
    }

    private Context getContext() throws SQLException {
        if(context==null) context=new Context();
        return context;
    }


    public String indexerName() {
        return this.getClass().getName();
    }

    public Map<String, String> createHashMap(String fieldName, String value){
        return null;
    }

}



