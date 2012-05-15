package com.atmire.authority.indexer;

import com.atmire.authority.SolrDocumentFields;
import org.dspace.content.ItemIterator;
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
public class FileIndexer implements IndexerInterface, SolrDocumentFields {

    private String SOURCE="LOCAL";

    ItemIterator itemIterator = null;

    String[] authorityControlledFields;

    private Map<String, String> nextValue;

    LinkedList<Map<String, String>> authorities = new LinkedList();

    Context context;


    @Override
    public void init() {
        Map<String, Map<String, String>> journalProperties = DryadJournalSubmissionUtils.journalProperties;
        Set<String> keys = journalProperties.keySet();
        for(String key :keys){
            Map<String, String> props = journalProperties.get(key);

            authorities.push(createHashMap(props));
        }

    }

    @Override
    public Map<String, String> nextValue() {
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


    private Map<String, String> createHashMap(Map<String, String> props){
        Map<String, String> values = new HashMap <String, String>();


        String value = props.get(DryadJournalSubmissionUtils.FULLNAME);

        String integratedJournal = props.get(DryadJournalSubmissionUtils.INTEGRATED);
        if(integratedJournal!=null && integratedJournal.equals("true"))
            value+="*";

        values.put(DOC_ID, Utils.getMD5(SOURCE + "prism.publicationName" + value));
        values.put(DOC_SOURCE, SOURCE + "-DryadJournal");
        values.put(DOC_FIELD, "prism.publicationName");
        values.put(DOC_DISPLAY_VALUE, value);
        values.put(DOC_VALUE, value);
        values.put(DOC_FULL_TEXT, value);
        return values;
    }


    private Context getContext() throws SQLException {
        if(context==null) context=new Context();
        return context;
    }


    public String indexerName() {
        return this.getClass().getName();
    }

    @Override
    public String getSource() {
        return SOURCE;
    }

    public Map<String, String> createHashMap(String fieldName, String value){
        return null;
    }

}



