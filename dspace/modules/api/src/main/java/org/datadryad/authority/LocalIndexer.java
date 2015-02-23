package org.datadryad.authority;

import org.apache.solr.common.SolrDocument;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.indexer.AuthorityIndexerInterface;
import org.dspace.content.*;

import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
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
public class LocalIndexer implements AuthorityIndexerInterface {

    private String SOURCE="LOCAL";

    ItemIterator itemIterator = null;

    String[] authorityControlledFields;

    private AuthorityValue nextValue;

    LinkedList<AuthorityValue> authorities = new LinkedList();

    Context context;

    public static final String DOC_ID="id";
    public static final String DOC_DISPLAY_VALUE="display-value";
    public static final String DOC_VALUE="value";
    public static final String DOC_FULL_TEXT="full-text";
    public static final String DOC_FIELD="field";
    public static final String DOC_SOURCE="source";

    public void init() {
        try {
            authorityControlledFields = getAuthorityControlledFields();

            if(authorityControlledFields==null || authorityControlledFields.length==0) return;

            context =getContext();
            DSpaceObject dso = HandleManager.resolveToObject(context, ConfigurationManager.getProperty("stats.datapkgs.coll"));
            itemIterator = ((Collection)dso).getAllItems();
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

    }

    @Override
    public void init(Context context, Item item) {
        init();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void init(Context context) {
        init();
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public AuthorityValue nextValue() {
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
        if (item != null && item.isArchived()) {
            for (String fieldName : authorityControlledFields) {
                DCValue[] dcVaslues = item.getMetadata("dc.identifier");
                String doi=null;
                if(dcVaslues.length > 0) doi=dcVaslues[0].value;

                DCValue[] values = item.getMetadata(fieldName);

                for (DCValue value : values) {
                    if (value.authority == null || value.authority.equals(SOURCE)) {
                        //System.out.println(" adding:  " + value.value);
                        AuthorityValue doc = createHashMap(fieldName, value.value);
                        authorities.push(doc);
                    }
                }
            }
        }
        getContext().clearCache();
    }


    public AuthorityValue createHashMap(String fieldName, String value){

        // Removing the asterisk from metadata so that it will not be indexed
        if(value.endsWith("*"))
        {
            value = value.substring(0, value.length()-1);
        }

        AuthorityValue authorityValue = new AuthorityValue();

        authorityValue.setId(Utils.getMD5(value));
        authorityValue.setSource(SOURCE);
        authorityValue.setField(fieldName);
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



