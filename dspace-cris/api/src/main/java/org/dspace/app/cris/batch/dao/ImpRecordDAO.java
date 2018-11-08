/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.bte.ImpRecordItem;
import org.dspace.app.cris.batch.bte.ImpRecordMetadata;
import org.dspace.app.cris.batch.dto.BitstreamInterface;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.app.cris.batch.dto.MetadataInterface;
import org.dspace.app.util.Util;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.util.ItemUtils;

public abstract class ImpRecordDAO
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ImpRecordDAO.class);

    private final String GET_IMPRECORDIDTOITEMID_BY_IMP_RECORD_ID_QUERY = "SELECT * FROM imp_record_to_item WHERE imp_record_id = ?";

    private final String GET_BY_LAST_MODIFIED_AND_EPERSON_ID_AND_SOURCEREF = "SELECT * FROM imp_record WHERE last_modified is NULL AND imp_eperson_id = ? AND imp_sourceref = ? order by imp_id ASC";
    
    private final String COUNT_BY_LAST_MODIFIED_AND_EPERSON_ID_AND_SOURCEREF = "SELECT count(*) FROM imp_record WHERE last_modified is NULL AND imp_eperson_id = ? AND imp_sourceref = ?";

    private final String GET_BY_METADATA_BY_IMPID = "SELECT * FROM imp_metadatavalue WHERE imp_id = ? ORDER BY imp_schema, imp_element, imp_qualifier, metadata_order ASC";
    
    protected Context context;
    
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd.HH-mm-ss");

    protected ImpRecordDAO(Context ctx)
    {
        context = ctx;
    }

    public Context getContext()
    {
        return context;
    }

    public abstract String getNEXTVALUESEQUENCE(String table);

    public int getNextValueSequence(String table) throws SQLException
    {
        int newID = -1;
        Statement statement = null;
        ResultSet rs = null;
        statement = context.getDBConnection().createStatement();
        rs = statement.executeQuery(getNEXTVALUESEQUENCE(table));

        rs.next();

        newID = rs.getInt(1);
        statement.close();
        rs.close();
        return newID;
    }

    public boolean checkImpRecordIdTOItemID(Integer imp_record_id)
            throws SQLException
    {
        TableRow row = DatabaseManager.querySingle(context,
                GET_IMPRECORDIDTOITEMID_BY_IMP_RECORD_ID_QUERY, imp_record_id);
        if (row == null)
        {
            return false;
        }
        return row.getIntColumn("imp_record_id") == -1 ? false : true;
    }

    public void write(DTOImpRecord impRecord, boolean buildUniqueImpRecordId) throws SQLException
    {
        String imp_record_id = impRecord.getImp_record_id();
        if(buildUniqueImpRecordId) {
            Calendar cal = Calendar.getInstance();
            String timestamp = df.format(cal.getTime());
            imp_record_id += ":" + timestamp;
        }
        log.debug("INSERT INTO imp_record " + " VALUES ( "
                + impRecord.getImp_id() + " , " + imp_record_id
                + " , " + impRecord.getImp_eperson_id() + " , "
                + impRecord.getImp_collection_id() + " , "
                + impRecord.getStatus() + " , " + impRecord.getOperation()
                + " , " + impRecord.getLast_modified() + " , "
                + impRecord.getHandle() + " )");
        DatabaseManager.updateQuery(context,
                "INSERT INTO imp_record(imp_id, imp_record_id, imp_eperson_id, imp_collection_id, status, operation, integra, last_modified, handle, imp_sourceref)"
                        + " VALUES (?, ?, ?, ?, ?, ?, null, null, null, ?)",
                impRecord.getImp_id(), imp_record_id,
                impRecord.getImp_eperson_id(), impRecord.getImp_collection_id(),
                impRecord.getStatus(), impRecord.getOperation(),
                impRecord.getImp_sourceRef());

        for (MetadataInterface o : impRecord.getMetadata())
        {
            //TODO authority confidence share
            log.debug("INSERT INTO imp_metadatavalue " + " VALUES ( "
                    + o.getPkey() + " , " + impRecord.getImp_id() + " , "
                    + o.getImp_schema() + " , " + o.getImp_element() + " , "
                    + o.getImp_qualifier() + " , " + o.getImp_value() + " , "
                    + o.getMetadata_order() + ")");
            if(o.getImp_qualifier()!=null && !o.getImp_qualifier().isEmpty()) {
                DatabaseManager.updateQuery(context,
                        "INSERT INTO imp_metadatavalue(imp_metadatavalue_id, imp_id, imp_schema, imp_element, imp_qualifier, imp_value, imp_authority, imp_confidence, imp_share, metadata_order, text_lang)"
                                + " VALUES (?, ?, ?, ?, ?, ?, null, null, null, ?, ?)",
                        o.getPkey(), impRecord.getImp_id(), o.getImp_schema(),
                        o.getImp_element(), o.getImp_qualifier(), o.getImp_value(),                    
                        o.getMetadata_order(), "en");
            }
            else {
                DatabaseManager.updateQuery(context,
                    "INSERT INTO imp_metadatavalue(imp_metadatavalue_id, imp_id, imp_schema, imp_element, imp_qualifier, imp_value, imp_authority, imp_confidence, imp_share, metadata_order, text_lang)"
                            + " VALUES (?, ?, ?, ?, null, ?, null, null, null, ?, ?)",
                    o.getPkey(), impRecord.getImp_id(), o.getImp_schema(),
                    o.getImp_element(), o.getImp_value(),                    
                    o.getMetadata_order(), "en");
            }
        }

        for (BitstreamInterface o : impRecord.getBitstreams())
        {
            // TODO manage blob
            log.debug(
                    "INSERT INTO imp_bitstream(imp_bitstream_id, imp_id, filepath, description, bundle, bitstream_order, primary_bitstream, assetstore, name, imp_blob, embargo_policy, embargo_start_date)"
                            + " VALUES ( " + o.getPkey() + " , "
                            + impRecord.getImp_id() + " , " + o.getFilepath()
                            + " , " + o.getDescription() + " , " + o.getBundle()
                            + " , " + o.getBitstream_order() + " , "
                            + o.getPrimary_bitstream() + " , "
                            + o.getAssetstore() + " , " + "null , "
                            + o.getEmbargoPolicy() + " , "
                            + o.getEmbargoStartDate());

            boolean primarybitstream = false;
            if (o.getPrimary_bitstream() != null
                    && o.getPrimary_bitstream() == true)
            {
                primarybitstream = true;
            }

            if (o.getEmbargoStartDate() == null) {
            	DatabaseManager.updateQuery(context,
	                    "INSERT INTO imp_bitstream(imp_bitstream_id, imp_id, filepath, description, bundle, bitstream_order, primary_bitstream, assetstore, name, imp_blob, embargo_policy)"
	                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, null, ?)",
	                    o.getPkey(), impRecord.getImp_id(), o.getFilepath(),
	                    o.getDescription(), o.getBundle(), o.getBitstream_order(),
	                    primarybitstream, o.getAssetstore(), o.getName(),
	                    o.getEmbargoPolicy());
            }
            else {
	            DatabaseManager.updateQuery(context,
	                    "INSERT INTO imp_bitstream(imp_bitstream_id, imp_id, filepath, description, bundle, bitstream_order, primary_bitstream, assetstore, name, imp_blob, embargo_policy, embargo_start_date)"
	                            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, null, ?, ?)",
	                    o.getPkey(), impRecord.getImp_id(), o.getFilepath(),
	                    o.getDescription(), o.getBundle(), o.getBitstream_order(),
	                    primarybitstream, o.getAssetstore(), o.getName(),
	                    o.getEmbargoPolicy(), o.getEmbargoStartDate());
            }
        }
    }

    public List<ImpRecordItem> findByEPersonIDAndSourceRefAndLastModifiedInNull(
            Integer epersonID, String sourceRef) throws SQLException
    {   
        List<ImpRecordItem> results = new ArrayList<ImpRecordItem>();
        TableRowIterator tri = DatabaseManager.query(context, GET_BY_LAST_MODIFIED_AND_EPERSON_ID_AND_SOURCEREF, epersonID, sourceRef);

        try
        {
            while (tri.hasNext())
            {
                ImpRecordItem impRecord = new ImpRecordItem();
                
                TableRow row = tri.next();
                Integer impId = row.getIntColumn("imp_id");
                String impRecordId = row.getStringColumn("imp_record_id");
                String impSourceRef = row.getStringColumn("imp_sourceref");
                
                impRecord.setSourceId(impRecordId);
                impRecord.setSourceRef(impSourceRef);
                
                TableRowIterator triMetadata = DatabaseManager.query(context, GET_BY_METADATA_BY_IMPID, impId);                
                try
                {
                    Set<ImpRecordMetadata> setMetadata = new HashSet<ImpRecordMetadata>();
                    while (triMetadata.hasNext())
                    {
                        ImpRecordMetadata impRecordMetadata = new ImpRecordMetadata();
                        
                        TableRow rowMetadata = triMetadata.next();
                        
                        String schema = rowMetadata.getStringColumn("imp_schema");
                        String element = rowMetadata.getStringColumn("imp_element");
                        String qualifier = rowMetadata.getStringColumn("imp_qualifier");
                        String authority = rowMetadata.getStringColumn("imp_authority");
                        String language = rowMetadata.getStringColumn("text_lang");
                        String value = rowMetadata.getStringColumn("imp_value");
                        Integer metadataOrder = rowMetadata.getIntColumn("metadata_order");
                        Integer share = rowMetadata.getIntColumn("imp_share");
                        Integer confidence = rowMetadata.getIntColumn("imp_confidence");

                        impRecordMetadata.setAuthority(authority);
                        impRecordMetadata.setConfidence(confidence);
                        impRecordMetadata.setMetadataOrder(metadataOrder);
                        impRecordMetadata.setShare(share);
                        impRecordMetadata.setValue(value);
                        impRecordMetadata.setLanguage(language);
                                                
                        String metadataName = Utils.standardize(schema, element, qualifier, ".");
                        impRecord.addMetadata(metadataName, impRecordMetadata); 
                    }
                    
                } finally
                {
                    if (triMetadata != null)
                    {
                        triMetadata.close();
                    }
                }       
                results.add(impRecord);
            }
        } finally
        {
            if (tri != null)
            {
                tri.close();
            }
        }
        
        return results;
    }

    public Integer countByEPersonIDAndSourceRefAndLastModifiedInNull(
            Integer epersonID, String sourceRef) throws SQLException
    {
        return DatabaseManager.querySingle(context, COUNT_BY_LAST_MODIFIED_AND_EPERSON_ID_AND_SOURCEREF, epersonID, sourceRef).getIntColumn("count");
    }

}
