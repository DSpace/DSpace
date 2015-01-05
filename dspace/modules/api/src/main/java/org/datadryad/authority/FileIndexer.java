package org.datadryad.authority;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 1, 2011
 * Time: 2:42:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileIndexer implements AuthorityIndexerInterface {

    private String SOURCE="LOCAL-DryadJournal";

    private AuthorityValue nextValue;

    LinkedList<AuthorityValue> authorities = new LinkedList<AuthorityValue>();

    Context context;

    public void init() {
        Map<String, Map<String, String>> journalProperties = DryadJournalSubmissionUtils.journalProperties;
        Set<String> keys = journalProperties.keySet();
        for(String key :keys){
            try {
                if(key != null) {
                    Map<String, String> props = journalProperties.get(key);
                    authorities.push(createHashMap(props));
                }
            } catch (Exception e) {
                throw new RuntimeException("unable to process index for journal " + key, e);
            }
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


    private AuthorityValue createHashMap(Map<String, String> props) throws Exception {

        String value = props.get(DryadJournalSubmissionUtils.FULLNAME);

//        String integratedJournal = props.get(DryadJournalSubmissionUtils.INTEGRATED);
//        if(integratedJournal!=null && integratedJournal.equals("true"))
//            value+="*";

        AuthorityValue authorityValue = new AuthorityValue();

        authorityValue.setId(Utils.getMD5(value));
        authorityValue.setSource(SOURCE);
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


    public String getSource() {
        return SOURCE;
    }

    public Map<String, String> createHashMap(String fieldName, String value){
        return null;
    }

}



