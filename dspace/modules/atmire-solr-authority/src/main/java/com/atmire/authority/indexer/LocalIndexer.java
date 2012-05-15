package com.atmire.authority.indexer;

import com.atmire.authority.SolrDocumentFields;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;

import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
import org.dspace.utils.DSpace;
import org.dspace.core.Utils;

import java.sql.SQLException;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Mar 1, 2011
 * Time: 2:42:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class LocalIndexer implements com.atmire.authority.indexer.IndexerInterface, SolrDocumentFields {

    private String SOURCE="LOCAL";

    ItemIterator itemIterator = null;

    String[] authorityControlledFields;

    private Map<String, String> nextValue;

    LinkedList<Map<String, String>> authorities = new LinkedList();

    Context context;
   


    @Override
    public void init() {
        try {
            authorityControlledFields = getAuthorityControlledFields();

            if(authorityControlledFields==null || authorityControlledFields.length==0) return;

            itemIterator = Item.findAll(getContext());
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

    }

    @Override
    public Map<String, String> nextValue() {
        return nextValue;
    }

    @Override
    public boolean hasMore() {
        try {

            if(authorityControlledFields==null || authorityControlledFields.length==0) return false;

            if (authorities.size() == 0) {
                Item item = itemIterator.next();
                populateAuthorities(item);
            }

            if(authorities.size() == 0 && itemIterator.hasNext()) hasMore();

            else if(authorities.size() == 0) nextValue = null;

            else nextValue = authorities.poll();

            return(nextValue != null);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }


    private void populateAuthorities(Item item) throws SQLException {
        if (item != null) {
            for (String fieldName : authorityControlledFields) {


                DCValue[] dcVaslues = item.getMetadata("dc.identifier");
                String doi=null;
                if(dcVaslues.length > 0) doi=dcVaslues[0].value;

                System.out.println(" item.id ******************************* " + item.getID() + " - " + doi + " *******************************");
                DCValue[] values = item.getMetadata(fieldName);

                for (DCValue value : values) {
                    if (value.authority == null || value.authority.equals(SOURCE)) {
                        System.out.println(" adding:  " + value.value);
                        authorities.push(createHashMap(fieldName, value.value));
                    }
                    else System.out.println("VALUE NOT PROCESSED! " + value.authority + " " + value.value);
                }
            }
        }
        getContext().clearCache();
    }


    public Map<String, String> createHashMap(String fieldName, String value){
        Map<String, String> values = new HashMap <String, String>();


        if(haveToAddAsterisk(value)){
            value+="*";
        }




        values.put(DOC_ID, Utils.getMD5(SOURCE + fieldName + value));
        values.put(DOC_SOURCE, SOURCE);
        values.put(DOC_FIELD, fieldName);
        values.put(DOC_DISPLAY_VALUE, value);
        values.put(DOC_VALUE, value);
        values.put(DOC_FULL_TEXT, value);
        return values;
    }


    private Context getContext() throws SQLException {
        if(context==null) context=new Context();
        return context;
    }


    private String[] getAuthorityControlledFields() {
        ConfigurationService cs = new DSpace().getConfigurationService();
        String[] fields = cs.getPropertyAsType("solr.authority.indexes", new String[0]);
        
        //printArray(fields);


        return fields;
    }


    //TODO: when everything will be ok, remove this method
    private void printArray(String[] strs) {
        for (String s : strs) {
            System.out.println("field in dspace.cfg ==>> " + s);
        }
    }

    public String indexerName() {
        return this.getClass().getName();
    }

    @Override
    public String getSource() {
        return SOURCE;
    }


    public boolean haveToAddAsterisk(String value){
        Map<String, Map<String, String>> journalProperties = DryadJournalSubmissionUtils.journalProperties;
        Set<String> keys = journalProperties.keySet();
        for(String key :keys){
            Map<String, String> props = journalProperties.get(key);
            String valueProp = props.get(DryadJournalSubmissionUtils.FULLNAME);

            if(value.equals(valueProp)){
                String integratedJournal = props.get(DryadJournalSubmissionUtils.INTEGRATED);
                if(integratedJournal!=null && integratedJournal.equals("true"))
                    return true;
            }
        }
        return false;
    }



}



