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
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.dto.BitstreamInterface;
import org.dspace.app.cris.batch.dto.DTOImpRecord;
import org.dspace.app.cris.batch.dto.MetadataInterface;
import org.dspace.core.Context;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

public abstract class ImpRecordDAO
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ImpRecordDAO.class);

    private final String GET_IMPRECORDIDTOITEMID_BY_IMP_RECORD_ID_QUERY = "SELECT * FROM imp_record_to_item WHERE imp_record_id = ?";

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

}
