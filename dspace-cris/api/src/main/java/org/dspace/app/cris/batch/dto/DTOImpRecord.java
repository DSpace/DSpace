/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch.dto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.app.cris.batch.dao.ImpRecordDAO;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitstreamStorageManager;

import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.GeneralFileOutputStream;
import edu.sdsc.grid.io.local.LocalFile;

public class DTOImpRecord
{
    
    private static final Logger log = Logger.getLogger(DTOImpRecord.class);
    
    private static int incoming = ConfigurationManager.getIntProperty("assetstore.incoming");
    private static String sAssetstoreDir = ConfigurationManager.getProperty("assetstore.dir");

    // These settings control the way an identifier is hashed into
    // directory and file names
    //
    // With digitsPerLevel 2 and directoryLevels 3, an identifier
    // like 12345678901234567890 turns into the relative name
    // /12/34/56/12345678901234567890.
    //
    // You should not change these settings if you have data in the
    // asset store, as the BitstreamStorageManager will be unable
    // to find your existing data.
    private static final int digitsPerLevel = 2;

    private static final int directoryLevels = 3;

    /**
     * This prefix string marks registered bitstreams in internal_id
     */
    private static final String REGISTERED_FLAG = "-R";
    
    private ImpRecordDAO dao;
    
    private Integer imp_id;
    
    private String imp_record_id;
    
    private Integer imp_eperson_id;
    
    private Integer imp_collection_id;
    
    private String status;
    
    private String operation;
    
    private Date last_modified;
    
    private String handle;
    
    private String imp_sourceRef;
    
    private List<DTOImpMetadata> metadata = new LinkedList<DTOImpMetadata>();
    
    private List<DTOImpBitstream> bitstreams = new LinkedList<DTOImpBitstream>();
   
    
    public DTOImpRecord(ImpRecordDAO dao) throws SQLException
    {      
      this.dao = dao;
      this.imp_id = dao.getNextValueSequence("imp_record");
    }
    public Integer getImp_id()
    {
        return imp_id;
    }

    public void setImp_id(Integer imp_id)
    {
        this.imp_id = imp_id;
    }

    public String getImp_record_id()
    {
        return imp_record_id;
    }

    public void setImp_record_id(String imp_record_id)
    {
        this.imp_record_id = imp_record_id;
    }

    public Integer getImp_eperson_id()
    {
        return imp_eperson_id;
    }

    public void setImp_eperson_id(Integer imp_eperson_id)
    {
        this.imp_eperson_id = imp_eperson_id;
    }

    public Integer getImp_collection_id()
    {
        return imp_collection_id;
    }

    public void setImp_collection_id(Integer imp_collection_id)
    {
        this.imp_collection_id = imp_collection_id;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public Date getLast_modified()
    {
        return last_modified;
    }

    public void setLast_modified(Date last_modified)
    {
        this.last_modified = last_modified;
    }

    public String getHandle()
    {
        return handle;
    }

    public void setHandle(String handle)
    {
        this.handle = handle;
    }
        
    public List<DTOImpMetadata> getMetadata()
    {
        return metadata;
    }
    public void setMetadata(List<DTOImpMetadata> metadata)
    {
        this.metadata = metadata;
    }     
    
    
    public List<DTOImpBitstream> getBitstreams()
    {
        return bitstreams;
    }
    public void setBitstreams(List<DTOImpBitstream> bitstreams)
    {
        this.bitstreams = bitstreams;
    }
    
    @Override
    public String toString()
    {
       String description = "imp_id(NOTNULL):" + imp_id+ " / imp_record_id(NOTNULL):" + imp_record_id + " / imp_eperson_id(NOTNULL) :" + imp_eperson_id + " / imp_collection_id(NOTNULL) :" + imp_collection_id + " / status :" + status + " / operation :" + operation + "/ last_modified : " + last_modified + " / handle :" + handle;
       for(DTOImpMetadata metadata : this.getMetadata()) {
           description += "\n METADATA pkey :" + metadata.pkey;
           description += " imp_schema(NOTNULL):" + metadata.imp_schema;
           description += " imp_element(NOTNULL):" + metadata.imp_element;
           description += " imp_qualifier:" + metadata.imp_qualifier;
           description += " imp_value(NOTNULL):" + metadata.imp_value;
           description += " metadata_order(NOTNULL):" + metadata.metadata_order;
       }
       for(DTOImpBitstream bitstream : this.getBitstreams()) {
           description += "\n BITSTREAM pkey :" + bitstream.pkey;
           description += " filepath(NOTNULL):" + bitstream.filepath;
           description += " description:" + bitstream.description;
           description += " primary_bitstream:" + bitstream.primary_bitstream;
           description += " bitstream_order:" + bitstream.bitstream_order;
       }
       return description;
    }


    class DTOImpMetadata implements MetadataInterface {
        
        private Integer pkey;        
       
        private String imp_schema;
        
        private String imp_element;
        
        private String imp_qualifier;
        
        private String imp_value;
        
        private String imp_authority;
        
        private Integer imp_confidence;
        
        private Integer imp_share;
        
        private Integer metadata_order;
        
        public DTOImpMetadata() throws SQLException
        {
            this.pkey = dao.getNextValueSequence("imp_metadatavalue");
        }

        public String getImp_schema()
        {
            return imp_schema;
        }

        public void setImp_schema(String imp_schema)
        {
            this.imp_schema = imp_schema;
        }

        public String getImp_element()
        {
            return imp_element;
        }

        public void setImp_element(String imp_element)
        {
            this.imp_element = imp_element;
        }

        public String getImp_qualifier()
        {
            return imp_qualifier;
        }

        public void setImp_qualifier(String imp_qualifier)
        {
            this.imp_qualifier = imp_qualifier;
        }

        public String getImp_value()
        {
            return imp_value;
        }

        public void setImp_value(String imp_value)
        {
            this.imp_value = imp_value;
        }

        public Integer getMetadata_order()
        {
            return metadata_order;
        }

        public void setMetadata_order(Integer metadata_order)
        {
            this.metadata_order = metadata_order;
        }
        public Integer getPkey()
        {
            return pkey;
        }

        public String getImp_authority()
        {
            return imp_authority;
        }

        public void setImp_authority(String imp_authority)
        {
            this.imp_authority = imp_authority;
        }

        public Integer getImp_confidence()
        {
            return imp_confidence;
        }

        public void setImp_confidence(Integer imp_confidence)
        {
            this.imp_confidence = imp_confidence;
        }

        public Integer getImp_share()
        {
            return imp_share;
        }

        public void setImp_share(Integer imp_share)
        {
            this.imp_share = imp_share;
        }

    }
    
    class DTOImpBitstream implements BitstreamInterface {  
        
        private Integer pkey;
        
        private String filepath;
        
        private String description;
        
        private Integer bitstream_order;
        
        private Boolean primary_bitstream;

        private String bundle;
        
        private Integer assetstore;
        
        private String name;
        
        private Integer embargoPolicy;
        
        private String embargoStartDate;
        
        private File blob;
        
        private String typeAttachment;
        
        public DTOImpBitstream() throws SQLException
        {
            this.pkey = dao.getNextValueSequence("imp_bitstream");           
        }
        
        public String getBundle()
        {
        	if(this.bundle==null) {
        		return "";
        	}
            return bundle;
        }

        public void setBundle(String bundle)
        {
            this.bundle = bundle;
        }
       
        public String getFilepath()
        {
            return filepath;
        }        

        public void setFilepath(String filepath)
        {
            this.filepath = filepath;
        }

        public String getDescription()
        {
            if(this.description==null) {
                return "";
            }
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        public Integer getBitstream_order()
        {
            return bitstream_order;
        }

        public void setBitstream_order(Integer bitstream_order)
        {
            this.bitstream_order = bitstream_order;
        }

        public Boolean getPrimary_bitstream()
        {
            return primary_bitstream;
        }

        public void setPrimary_bitstream(Boolean primary_bitstream)
        {
            this.primary_bitstream = primary_bitstream;
        }
        
        public Integer getPkey()
        {
            return pkey;
        }

        public void setPkey(Integer pkey)
        {
            this.pkey = pkey;
        }

        public Integer getAssetstore()
        {
            return assetstore;
        }

        public void setAssetstore(Integer assetstore)
        {
            this.assetstore = assetstore;
        }
        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Integer getEmbargoPolicy()
        {
            return embargoPolicy;
        }

        public void setEmbargoPolicy(Integer embargoPolicy)
        {
            this.embargoPolicy = embargoPolicy;
        }

        public String getEmbargoStartDate()
        {
            return embargoStartDate;
        }

        public void setEmbargoStartDate(String embargoStartDate)
        {
            this.embargoStartDate = embargoStartDate;
        }

        public File getBlob()
        {
            return blob;
        }

        public void setBlob(File blob)
        {
            this.blob = blob;
        }

        public String getTypeAttachment()
        {
            return typeAttachment;
        }

        public void setTypeAttachment(String typeAttachment)
        {
            this.typeAttachment = typeAttachment;
        }

    }

    public void addBitstream(Context context, InputStream is, String description, Boolean primary_bitstream, int bitstream_order_count, int assetstore, String bundle, int embargoPolicy, String embargoStartDate, String name, String typeAttachment) throws SQLException, IOException
    {
        
        GeneralFile file = DTOImpRecord.store(context, is);
        
        File dir = new File(sAssetstoreDir);
        String relativeFilePathAssetStore = file.getAbsolutePath().replace(
                dir.getCanonicalPath() + File.separatorChar, "");
        
        DTOImpBitstream bitstream = new DTOImpBitstream();
        bitstream.setFilepath(relativeFilePathAssetStore);
        bitstream.setDescription(description);        
        bitstream.setBitstream_order(bitstream_order_count++);
        bitstream.setPrimary_bitstream(primary_bitstream);
        bitstream.setFilepath(relativeFilePathAssetStore);
        bitstream.setAssetstore(assetstore);
        bitstream.setBundle(bundle);
        bitstream.setEmbargoPolicy(embargoPolicy);
        bitstream.setEmbargoStartDate(embargoStartDate);
        bitstream.setName(name);
        bitstream.setTypeAttachment(typeAttachment);
        
        getBitstreams().add(bitstream);
    }
    
    public void addMetadata(String dc, String element, String qualifier,
            String metadata_value, String authority, int confidence, int metadata_order, int share) throws SQLException
    {
        DTOImpMetadata metadata = new DTOImpMetadata();
        metadata.setImp_schema(dc);
        metadata.setImp_element(element);
        metadata.setImp_qualifier(qualifier);
        metadata.setImp_value(metadata_value);
        metadata.setMetadata_order(metadata_order);
        metadata.setImp_authority(authority);
        metadata.setImp_confidence(confidence);
        metadata.setImp_share(share);
        
        getMetadata().add(metadata);
        
    }
    
    public static GeneralFile store(Context context, InputStream is)
            throws SQLException, IOException {
        // Create internal ID
        String id = Utils.generateKey();

        // 'assetstore.dir' is always store number 0
        
        // Where on the file system will this new bitstream go?
        GeneralFile file = getFile(incoming, id, new LocalFile(sAssetstoreDir));

        // Make the parent dirs if necessary
        GeneralFile parent = file.getParentFile();

        if (!parent.exists()) {
            parent.mkdirs();
        }

        // Create the corresponding file and open it
        file.createNewFile();

        GeneralFileOutputStream fos = FileFactory.newFileOutputStream(file);

        // Read through a digest input stream that will work out the MD5
        DigestInputStream dis = null;

        try {
            dis = new DigestInputStream(is, MessageDigest.getInstance("MD5"));
        }
        // Should never happen
        catch (NoSuchAlgorithmException nsae) {
            log.warn("Caught NoSuchAlgorithmException", nsae);
        }

        Utils.bufferedCopy(dis, fos);
        fos.close();
        is.close();

        return file;
    }

    private static GeneralFile getFile(int storeNumber, String sInternalId,
            GeneralFile assetstore) throws IOException {

        // Default to zero ('assetstore.dir') for backwards compatibility
        if (storeNumber == -1) {
            storeNumber = 0;
        }

        // there are 4 cases:
        // -conventional bitstream, conventional storage
        // -conventional bitstream, srb storage
        // -registered bitstream, conventional storage
        // -registered bitstream, srb storage
        // conventional bitstream - dspace ingested, dspace random name/path
        // registered bitstream - registered to dspace, any name/path
        String sIntermediatePath = null;
        if (BitstreamStorageManager.isRegisteredBitstream(sInternalId)) {
            sInternalId = sInternalId.substring(REGISTERED_FLAG.length());
            sIntermediatePath = "";
        } else {

            // Sanity Check: If the internal ID contains a
            // pathname separator, it's probably an attempt to
            // make a path traversal attack, so ignore the path
            // prefix. The internal-ID is supposed to be just a
            // filename, so this will not affect normal operation.
            if (sInternalId.indexOf(File.separator) != -1)
                sInternalId = sInternalId.substring(sInternalId
                        .lastIndexOf(File.separator) + 1);

            sIntermediatePath = getIntermediatePath(sInternalId);
        }

        StringBuffer bufFilename = new StringBuffer();
        bufFilename.append(assetstore.getCanonicalPath());
        bufFilename.append(File.separator);
        bufFilename.append(sIntermediatePath);
        bufFilename.append(sInternalId);
        if (log.isDebugEnabled()) {
            log.debug("Local filename for " + sInternalId + " is "
                    + bufFilename.toString());
        }
        return new LocalFile(bufFilename.toString());
    }

    /**
     * Return the intermediate path derived from the internal_id. This method
     * splits the id into groups which become subdirectories.
     * 
     * @param iInternalId
     *            The internal_id
     * @return The path based on the id without leading or trailing separators
     */
    private static String getIntermediatePath(String iInternalId) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < directoryLevels; i++) {
            int digits = i * digitsPerLevel;
            if (i > 0) {
                buf.append(File.separator);
            }
            buf.append(iInternalId.substring(digits, digits + digitsPerLevel));
        }
        buf.append(File.separator);
        return buf.toString();
    }
    public String getImp_sourceRef()
    {
        return imp_sourceRef;
    }
    public void setImp_sourceRef(String imp_sourceRef)
    {
        this.imp_sourceRef = imp_sourceRef;
    }
}


