/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

/**
 * This class is the PostgreSQL driver class for reading information about the authority use.
 * It implements the AuthorityDAO interface, and also has a
 * constructor of the form:
 *
 * AuthorityDAOPostgres(Context context)
 *
 * As required by AuthorityDAOFactory.  This class should only ever be loaded by
 * that Factory object.
 *
 * @author bollini
 */
public class AuthorityDAOPostgres implements AuthorityDAO {

    private Context context;

//    private static final String SQL_NUM_METADATA_GROUP_BY_AUTHKEY_CONFIDENCE = "select authority, confidence, count(*) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) group by authority, confidence";
    private static final String SQL_NUM_METADATA_AUTH_GROUP_BY_CONFIDENCE = "select confidence, count(*) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null group by confidence";

    private static final String SQL_NUM_AUTHORED_ITEMS = "select count(distinct item.item_id) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null";
  
    private static final String SQL_NUM_ISSUED_ITEMS = "select count(distinct item.item_id) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and confidence <> "+ Choices.CF_ACCEPTED;

//    private static final String SQL_NUM_METADATA_GROUP_BY_AUTH_ISSUED = "select authority is not null as hasauthority, confidence <> "+ Choices.CF_ACCEPTED +" as bissued, count(*) as num from tem ileft join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) group by hasauthority, bissued";

    private static final String SQL_NUM_METADATA = "select count(*) as num from metadatavalue where metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER)";

    private static final String SQL_NUM_AUTHKEY = "select count (distinct authority) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null";

    private static final String SQL_NUM_AUTHKEY_ISSUED = "select count (distinct authority) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and confidence <> "+ Choices.CF_ACCEPTED + " and authority is not null";

    private static final String SQL_AUTHKEY_ISSUED = "select distinct authority from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and confidence <> "+ Choices.CF_ACCEPTED+" order by authority asc limit ? offset ?";

    private static final String SQL_NUM_ITEMSISSUED_BYKEY = "select count (distinct item.item_id) as num from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and confidence <> "+ Choices.CF_ACCEPTED+" and authority = ?";

    private static final String SQL_NEXT_ISSUED_AUTHKEY = "select min(authority) as key from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and authority > ? and confidence <> "+ Choices.CF_ACCEPTED;

    private static final String SQL_PREV_ISSUED_AUTHKEY = "select max(authority) as key from item left join metadatavalue on item.item_id = metadatavalue.item_id where in_archive = true and  metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) and authority is not null and authority < ? and confidence <> "+ Choices.CF_ACCEPTED;

    private static final String SQL_ITEMSISSUED_BYKEY_AND_CONFIDENCE = "SELECT item.* FROM item left join metadatavalue on item.item_id = metadatavalue.item_id WHERE in_archive=true AND metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) AND authority = ? AND confidence = ?";
    
    public AuthorityDAOPostgres(Context context) {
        this.context = context;
    }

    public AuthorityInfo getAuthorityInfo(String md) throws SQLException {      
        int[] fieldIds = new int[]{getFieldId(md)};
        return getAuthorityInfoByFieldIds(md, fieldIds);
    }

    @Override
    public AuthorityInfo getAuthorityInfoByAuthority(String authorityName)
            throws SQLException {
        int[] fieldIds = getFieldIds(authorityName);
        return getAuthorityInfoByFieldIds(authorityName, fieldIds);
    }

    private AuthorityInfo getAuthorityInfoByFieldIds(String scope, int[] fieldIds)
            throws SQLException
    {
        long[]numMetadataByConfidence = new long[7];

        TableRowIterator tri = DatabaseManager.query(context, getFinalQueryString(SQL_NUM_METADATA_AUTH_GROUP_BY_CONFIDENCE, fieldIds));
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            int conf = row.getIntColumn("confidence");
            if (conf < Choices.CF_NOVALUE || conf > Choices.CF_ACCEPTED)
            {
                conf = 0;
            }
            numMetadataByConfidence[conf/100] = row.getLongColumn("num");
        }
        tri.close();
        TableRow row = DatabaseManager.querySingle(context, getFinalQueryString(SQL_NUM_AUTHORED_ITEMS, fieldIds));
        long numItems = row.getLongColumn("num");
        if (numItems == -1)
        {
            numItems = 0;
        }

        row = DatabaseManager.querySingle(context, getFinalQueryString(SQL_NUM_ISSUED_ITEMS, fieldIds));
        long numIssuedItems = row.getLongColumn("num");
        if (numIssuedItems == -1)
        {
            numIssuedItems = 0;
        }

        row = DatabaseManager.querySingle(context, getFinalQueryString(SQL_NUM_METADATA, fieldIds));
        long numTotMetadata = row.getLongColumn("num");

        row = DatabaseManager.querySingle(context, getFinalQueryString(SQL_NUM_AUTHKEY, fieldIds));
        long numAuthorityKey = row.getLongColumn("num");
        
        long numAuthorityIssued = countIssuedAuthorityKeys(fieldIds);

        return new AuthorityInfo(scope, fieldIds.length > 1, numMetadataByConfidence, numTotMetadata,
                numAuthorityKey, numAuthorityIssued, numItems, numIssuedItems);
    }

    
    
    
    public List<String> listAuthorityKeyIssued(String md, int limit, int page) throws SQLException {
        int[] fieldIds = new int[]{getFieldId(md)};
        return listAuthorityKeyIssuedByFieldId(fieldIds, limit, page);
    }

    @Override
    public List<String> listAuthorityKeyIssuedByAuthority(String authorityName,
            int limit, int page) throws SQLException {
        int[] fieldIds = getFieldIds(authorityName);
        return listAuthorityKeyIssuedByFieldId(fieldIds, limit, page);
    }
    
    private List<String> listAuthorityKeyIssuedByFieldId(int[] fieldId,
            int limit, int page) throws SQLException
    {
        List<String> keys = new ArrayList<String>();

        TableRowIterator tri = DatabaseManager.query(context, getFinalQueryString(SQL_AUTHKEY_ISSUED,
                fieldId), limit, (page * limit));
        while (tri.hasNext())
        {
            TableRow row = tri.next();
            keys.add(row.getStringColumn("authority"));
        }
        tri.close();
        return keys;
    }


    

    public long countIssuedAuthorityKeys(String metadata) throws SQLException {
        return countIssuedAuthorityKeys(getFieldId(metadata));
    }
    
    
    @Override
    public long countIssuedAuthorityKeysByAuthority(String authorityName)
            throws SQLException {
        return countIssuedAuthorityKeys(getFieldIds(authorityName));
    }

    private long countIssuedAuthorityKeys(int... fieldId) throws SQLException {
        TableRow row = DatabaseManager.querySingle(context, getFinalQueryString(SQL_NUM_AUTHKEY_ISSUED, fieldId));
        long numAuthorityIssued = row.getLongColumn("num");
        return numAuthorityIssued;
    }
    
    
    
    public ItemIterator findIssuedByAuthorityValue(String metadata, String authority)
        throws SQLException, AuthorizeException, IOException
    {
        int[] fieldId = new int[]{getFieldId(metadata)};

        return findIssuedByAuthorityValueAndFieldId(authority, fieldId);
    }

    @Override
    public ItemIterator findIssuedByAuthorityValueInAuthority(
            String authorityName, String authority) throws SQLException,
            AuthorizeException, IOException {
        int[] fieldId = getFieldIds(authorityName);

        return findIssuedByAuthorityValueAndFieldId(authority, fieldId);
    }
    
    private ItemIterator findIssuedByAuthorityValueAndFieldId(String authority,
            int[] fieldId) throws SQLException
    {
        String query = "SELECT item.* FROM item left join metadatavalue " +
                "on item.item_id = metadatavalue.item_id " +
                "WHERE in_archive=true AND metadata_field_id in (QUESTION_ARRAY_PLACE_HOLDER) AND authority = ? AND confidence <> "+ Choices.CF_ACCEPTED;
        TableRowIterator rows = DatabaseManager.queryTable(context, "item",
            query, fieldId, authority);
        return new ItemIterator(context, rows);
    }
    
    

    public long countIssuedItemsByAuthorityValue(String metadata, String key) throws SQLException {
        int[] fieldId = new int[]{getFieldId(metadata)};
        return countIssuedItemsByAuthorityValueAndFieldId(key, fieldId);
    }

    @Override
    public long countIssuedItemsByAuthorityValueInAuthority(
            String authorityName, String key) throws SQLException {
        int[] fieldId = getFieldIds(authorityName);
        return countIssuedItemsByAuthorityValueAndFieldId(key, fieldId);
    }
    
    private long countIssuedItemsByAuthorityValueAndFieldId(String key,
            int[] fieldId) throws SQLException
    {
        TableRow row = DatabaseManager.querySingle(context,
                getFinalQueryString(SQL_NUM_ITEMSISSUED_BYKEY, fieldId), key);
        long numAuthorityIssued = row.getLongColumn("num");
        return numAuthorityIssued;
    }
    
    
    
    
    public String findNextIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
        int[] fieldId = new int[]{getFieldId(metadata)};
        return findNextIssuedAuthorityKeyByFieldId(focusKey, fieldId);
    }

    @Override
    public String findNextIssuedAuthorityKeyInAuthority(String authorityName,
            String focusKey) throws SQLException {
        int[] fieldId = getFieldIds(authorityName);
        return findNextIssuedAuthorityKeyByFieldId(focusKey, fieldId);
    }
    
    private String findNextIssuedAuthorityKeyByFieldId(String focusKey,
            int[] fieldId) throws SQLException
    {
        TableRow row = DatabaseManager.querySingle(context,
                getFinalQueryString(SQL_NEXT_ISSUED_AUTHKEY, fieldId), focusKey);
        if (row != null)
        {
            return row.getStringColumn("key");
        }        
        return null;
    }

    
    
    public String findPreviousIssuedAuthorityKey(String metadata, String focusKey) throws SQLException {
        int[] fieldId = new int[]{getFieldId(metadata)};
        return findPreviousIssuedAuthorityKeyByFieldId(focusKey, fieldId);
    }

    @Override
    public String findPreviousIssuedAuthorityKeyInAuthority(
            String authorityName, String focusKey) throws SQLException {
        int[] fieldId = getFieldIds(authorityName);
        return findPreviousIssuedAuthorityKeyByFieldId(focusKey, fieldId);
    }
    
    private String findPreviousIssuedAuthorityKeyByFieldId(String focusKey,
            int[] fieldId) throws SQLException
    {
        TableRow row = DatabaseManager.querySingle(context,
                getFinalQueryString(SQL_PREV_ISSUED_AUTHKEY, fieldId), focusKey);
        if (row != null)
        {
            return row.getStringColumn("key");
        }
        return null;
    }


    
    @Override
    public ItemIterator findIssuedByAuthorityValueAndConfidence(
            String metadata, String authority, int confidence)
            throws SQLException, AuthorizeException, IOException
    {
        int[] fieldId = new int[]{getFieldId(metadata)};
                
        return findIssuedByAuthorityValueAndConfidenceAndFieldId(authority,
                confidence, fieldId);
    }

	@Override
	public ItemIterator findIssuedByAuthorityValueAndConfidenceInAuthority(
			String authorityName, String authority, int confidence)
			throws SQLException, AuthorizeException, IOException {
	    int[] fieldId = getFieldIds(authorityName);
        
        return findIssuedByAuthorityValueAndConfidenceAndFieldId(authority,
                confidence, fieldId);
	}
    
	private ItemIterator findIssuedByAuthorityValueAndConfidenceAndFieldId(
            String authority, int confidence, int[] fieldId) throws SQLException
    {
        TableRowIterator rows = DatabaseManager.queryTable(context, "item",
                getFinalQueryString(SQL_ITEMSISSUED_BYKEY_AND_CONFIDENCE, fieldId), authority, confidence);
        return new ItemIterator(context, rows);
    }
    
	/*
	 * UTILITY METHODS
	 */
    private int getFieldId(String md) throws IllegalArgumentException, SQLException {
        String[] metadata = md.split("\\.");
        int fieldId = -1;
        try {
            int schemaID = MetadataSchema.find(context, metadata[0]).getSchemaID();
            fieldId = MetadataField.findByElement(context, schemaID, metadata[1], metadata.length > 2 ? metadata[2] : null).getFieldID();
        }catch (NullPointerException npe) {
            // the metadata field is not defined
            throw new IllegalArgumentException("Error retriving metadata field for input the supplied string: " + md, npe);
        } 
        catch (AuthorizeException ex) {
            // mmmm... we should not go here
            throw new IllegalArgumentException("Error retriving metadata field for input the supplied string: " + md, ex);
        }
        return fieldId;
    }
    
    private int[] getFieldIds(String authorityName) throws IllegalArgumentException, SQLException {
        List<String> metadata = ChoiceAuthorityManager.getManager()
                .getAuthorityMetadataForAuthority(authorityName);
        int[] ids = new int[metadata.size()];
        
        for (int i = 0; i < metadata.size(); i++)
        {
            ids[i] = getFieldId(metadata.get(i));
        }
        return ids;
    }
    
    private String getFinalQueryString(String queryTemplate, int[] fieldId)
    {
        String questions = String.valueOf(fieldId[0]);
        // start from 1 the first question mark is build-in
        for (int idx = 1; idx < fieldId.length; idx++)
        {
            questions += ", "+fieldId[idx];
        }
        return queryTemplate.replace("QUESTION_ARRAY_PLACE_HOLDER", questions);
    }
}