/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Asset store using Google Cloud Platform's Storage.
 * You must have an account and project with Google Cloud Platform and created a service account in the project to use this store.
 * You need to set GOOGLE_APPLICATION_CREDENTIALS environment variable pointing to json key file given by GCP after creating a service account (see https://developers.google.com/identity/protocols/application-default-credentials)
 * 
 * @author Pedro Amorim
 */ 

public class GCSBitStoreService implements BitStoreService
{
    /** log4j log */
    private static Logger log = Logger.getLogger(GCSBitStoreService.class);
    
    /** Checksum algorithm */
    private static final String CSA = "MD5";

    /** container for all the assets */
    private String bucketName = null;
    
    /** GCP storage */
    private Storage storage = null;

    /** GCP project ID */
    private String projectID = null;

    /** Secret key json file path */
    private String secretFile = null;

    public GCSBitStoreService()
    {
    }

    public void init() throws IOException {
	 storage = StorageOptions.newBuilder()
		.setCredentials(ServiceAccountCredentials.fromStream(new FileInputStream(secretFile)))
		.setProjectId(projectID)
		.build()
		.getService();
    }
    

    /**
     * Return an identifier unique to this asset store instance
     * 
     * @return a unique ID
     */
    public String generateId()
    {
        return Utils.generateKey();
    }

    /**
     * Retrieve the bits for the asset with ID. If the asset does not
     * exist, returns null.
     * 
     * @param bitstream
     *            The ID of the asset to retrieve
     * @throws java.io.IOException
     *                If a problem occurs while retrieving the bits
     *
     * @return The stream of bits, or null
     */
    public InputStream get(Bitstream bitstream) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
        try {
	Blob blob = storage.get(bucketName, key);
	if(blob != null){
		return new ByteArrayInputStream(blob.getContent());
	}
	return null;
        }
        catch (Exception e)
        {
            log.error("get("+key+")", e);
            throw new IOException(e);
        }
    }

    /**
     * Store a stream of bits.
     *
     * <p>
     * If this method returns successfully, the bits have been stored.
     * If an exception is thrown, the bits have not been stored.
     * </p>
     *
     * @param in
     *            The stream of bits to store
     * @throws java.io.IOException
     *             If a problem occurs while storing the bits
     */
    public void put(Bitstream bitstream, InputStream in) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = File.createTempFile(bitstream.getInternalId(), "gcs");
        try {
            FileUtils.copyInputStreamToFile(in, scratchFile);
            Long contentLength = Long.valueOf(scratchFile.length());

	    BlobId blobId = BlobId.of(bucketName, bitstream.getInternalId());
       	    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
	    InputStream targetStream = new FileInputStream(scratchFile);
	    Blob blob = storage.create(blobInfo, targetStream);

            bitstream.setSizeBytes(contentLength);
            bitstream.setChecksum(blobInfo.getEtag());
            bitstream.setChecksumAlgorithm(CSA);

            scratchFile.delete();

        } catch(Exception e) {
            log.error("put(" + bitstream.getInternalId() +", is)", e);
            throw new IOException(e);
        } finally {
            if (scratchFile.exists()) {
                scratchFile.delete();
            }
        }
    }

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * Checksum used is (ETag) hex encoded 128-bit MD5 digest of an object's content
     * (Does not use getContentMD5, as that is 128-bit MD5 digest calculated on caller's side)
     *
     * @param bitstream
     *            The asset to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     *
     * @throws java.io.IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     *            If file not found, then return null
     */
    public Map about(Bitstream bitstream, Map attrs) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
        try {
		Blob blob = storage.get(bucketName, key);
		if(blob != null){
			BlobInfo blobInfo = blob.toBuilder().build();
			if (attrs.containsKey("size_bytes")) {
		    		attrs.put("size_bytes", blobInfo.getSize());
			}
		        if (attrs.containsKey("checksum")) {
		            attrs.put("checksum", blobInfo.getEtag());
		            attrs.put("checksum_algorithm", CSA);
		        }
			if (attrs.containsKey("modified")) {
		    		attrs.put("modified", String.valueOf(blobInfo.getUpdateTime()));
			}
			return attrs;	
		}	
        }  catch (Exception e) {
            log.error("about("+key+", attrs)", e);
            throw new IOException(e);
        }
        return null;
    }

    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param bitstream
     *            The asset to delete
     * @throws java.io.IOException
     *             If a problem occurs while removing the asset
     */
    public void remove(Bitstream bitstream) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
	try {
            BlobId blobId = BlobId.of(bucketName, key);
	    storage.delete(blobId);
        } catch (Exception e) {
            log.error("remove("+key+")", e);
            throw new IOException(e);
        }
    }

    /**
     * Utility Method: Prefix the key with a subfolder, if this instance assets are stored within subfolder
     * @param id
     *     DSpace bitstream internal ID
     * @return full key prefixed with a subfolder, if applicable
     */
    public String getFullKey(String id) {
            return id;
    }

    @Required
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @Required
    public String getProjectID() {
        return projectID;
    }

    public void setProjectID(String projectID) {
        this.projectID = projectID;
    }

    @Required
    public String getSecretFile() {
        return secretFile;
    }

    public void setSecretFile(String secretFile) {
        this.secretFile = secretFile;
    }

    /**
     * Contains a command-line testing tool. Expects arguments:
     *  -a accessKey -s secretKey -f assetFileName
     *
     * @param args the command line arguments given
     * @throws Exception
     *     generic exception
     */
    public static void main(String[] args) throws Exception
    {
        //TODO To be implemented

    }
}

