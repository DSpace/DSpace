/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.content.Bitstream;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitStore;

/**
 * Asset store using Amazon's Simple Storage Service (S3).
 * S3 is a commercial, web-service accessible, remote storage facility.
 * NB: you must have obtained an account with Amazon to use this store
 * 
 * @author Richard Rodgers, Peter Dietz
 */ 

public class S3BitStore implements BitStore
{
    /** log4j log */
    private static Logger log = Logger.getLogger(S3BitStore.class);
    
    /** Checksum algorithm */
    private static final String CSA = "MD5";
    
    /** container for all the assets */
	private String bucketName = null;
	
	/** S3 service */
	private AmazonS3 s3Service = null;
		
	public S3BitStore()
	{
	}
	
	/**
     * Initialize the asset store
     * S3 Requires:
     *  - access key
     *  - secret key
     *  - bucket name
     *  - Region (optional)
     * 
     * @param config
     *        String used to characterize configuration - may be a configuration
     *        value, or the name of a config file containing such values
     */
	public void init(String config) throws IOException
	{
        // load configs
		Properties props = new Properties();
		try
		{
		    props.load(new FileInputStream(config));
		}
		catch(Exception e)
		{
			throw new IOException("Exception loading properties. Config: " + config + ", exception: " + e.getMessage());
		}

        // access / secret
        String awsAccessKey = props.getProperty("aws_access_key_id");
        String awsSecretKey = props.getProperty("aws_secret_access_key");
        if(StringUtils.isBlank(awsAccessKey) || StringUtils.isBlank(awsSecretKey)) {
            log.warn("Empty S3 access or secret");
        }

        // init client
        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
        s3Service = new AmazonS3Client(awsCredentials);

        // bucket name
        bucketName = props.getProperty("bucketName");
        if(StringUtils.isEmpty(bucketName)) {
            bucketName = "dspace-asset-" + ConfigurationManager.getProperty("dspace.hostname");
            log.warn("S3 BucketName is not configured, setting default: " + bucketName);
        }

        try {
            if(! s3Service.doesBucketExist(bucketName)) {
                s3Service.createBucket(bucketName);
                log.info("Creating new S3 Bucket: " + bucketName);
            } else {
                log.info("Using existing S3 Bucket: " + bucketName);
            }
        }
        catch (Exception e)
        {
            throw new IOException("Amazon S3 bucket Exception: " + e.getMessage());
        }

        // region
        String regionName = props.getProperty("aws_region");
        if(StringUtils.isNotBlank(regionName)) {
            try {
                Regions regions = Regions.fromName(regionName);
                Region region = Region.getRegion(regions);
                s3Service.setRegion(region);
                log.info("S3 Region set to: " + region.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid aws_region");
            }
        }

        log.debug("AWS S3 Assetstore ready to go!");
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
     * @param id
     *            The ID of the asset to retrieve
     * @exception IOException
     *                If a problem occurs while retrieving the bits
     * 
     * @return The stream of bits, or null
     */
	public InputStream get(String id) throws IOException
	{
        S3Object object = null;

		try
		{
            object = s3Service.getObject(new GetObjectRequest(bucketName, id));
			return (object != null) ? object.getObjectContent() : null;
		}
        catch (Exception e)
		{
        	throw new IOException("S3 get Exception: " + e.getMessage());
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
     * @exception IOException
     *             If a problem occurs while storing the bits
     * 
     * @return Map containing technical metadata (size, checksum, etc)
     */
	public Map put(InputStream in, String id) throws IOException
	{
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = File.createTempFile(id, "s3bs");
        try {
            FileUtils.copyInputStreamToFile(in, scratchFile);
            Long contentLength = Long.valueOf(scratchFile.length());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, id, scratchFile);
            PutObjectResult putObjectResult = s3Service.putObject(putObjectRequest);

            Map attrs = new HashMap();
            attrs.put(Bitstream.SIZE_BYTES, contentLength);
            attrs.put(Bitstream.CHECKSUM, putObjectResult.getContentMd5());
            attrs.put(Bitstream.CHECKSUM_ALGORITHM, CSA);
            //attrs.put("modified",
            //	      String.valueOf(object.getLastModifiedDate().getTime()));

            scratchFile.delete();
            return attrs;

        } finally {
            if(scratchFile.exists()) {
                scratchFile.delete();
            }
        }

	}
	
    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * @param id
     *            The ID of the asset to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     * 
     * @exception IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     */
	public Map about(String id, Map attrs) throws IOException
	{
        ObjectMetadata objectMetadata = s3Service.getObjectMetadata(bucketName, id);

        if (objectMetadata != null)
        {
            if (attrs.containsKey(Bitstream.SIZE_BYTES))
            {
                attrs.put(Bitstream.SIZE_BYTES, objectMetadata.getContentLength());
            }
            if (attrs.containsKey(Bitstream.CHECKSUM))
            {
                attrs.put(Bitstream.CHECKSUM, objectMetadata.getContentMD5());
                attrs.put(Bitstream.CHECKSUM_ALGORITHM, CSA);
            }
            if (attrs.containsKey("modified"))
            {
                attrs.put("modified", String.valueOf(objectMetadata.getLastModified().getTime()));
            }
            return attrs;
        }
        return null;
	}
	
    /**
     * Remove an asset from the asset store. An irreversible operation.
     *
     * @param id
     *            The ID of the asset to delete
     * @exception IOException
     *             If a problem occurs while removing the asset
     */
	public void remove(String id) throws IOException
	{
        s3Service.deleteObject(bucketName, id);
	}
	
	/**
	 * Contains a command-line testing tool. Expects arguments:
	 *  -a accessKey -s secretKey -f assetFileName
	 * 
	 * @param args
	 *        Command line arguments
	 */
	public static void main(String[] args) throws Exception
	{
        //TODO use proper CLI, or refactor to be a unit test. Can't mock this without keys though.

		// parse command line
		String assetFile = null;
		String accessKey = null;
		String secretKey = null;
		
		for (int i = 0; i < args.length; i+= 2)
		{
			if (args[i].startsWith("-a"))
			{
				accessKey = args[i+1];
			}
			else if (args[i].startsWith("-s"))
			{
				secretKey = args[i+1];
			}
			else if (args[i].startsWith("-f"))
			{
				assetFile = args[i+1];
			}
		}
		
		if (accessKey == null || secretKey == null ||assetFile == null)
		{
			System.out.println("Missing arguments - exiting");
			return;
		}
		S3BitStore store = new S3BitStore();

        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

        store.s3Service = new AmazonS3Client(awsCredentials);

        //Todo configurable region
        Region usEast1 = Region.getRegion(Regions.US_EAST_1);
        store.s3Service.setRegion(usEast1);

        //Bucketname should be lowercase
        store.bucketName = "dspace-asset-" + ConfigurationManager.getProperty("dspace.hostname") + ".s3test";
        store.s3Service.createBucket(store.bucketName);

        // time everything, todo, swtich to caliper
        long start = System.currentTimeMillis();
        // Case 1: store a file
        String id = store.generateId();
        System.out.print("put() file " + assetFile + " under ID " + id + ": ");
        FileInputStream fis = new FileInputStream(assetFile);
        Map attrs = store.put(fis, id);
        long now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        // examine the metadata returned
        java.util.Iterator iter = attrs.keySet().iterator();
        System.out.println("Metadata after put():");
        while (iter.hasNext())
        {
        	String key = (String)iter.next();
        	System.out.println( key + ": " + (String)attrs.get(key) );
        }
        // Case 2: get metadata and compare
        System.out.print("about() file with ID " + id + ": ");
        Map attrs2 = store.about(id, attrs);
        now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        iter = attrs2.keySet().iterator();
        System.out.println("Metadata after about():");
        while (iter.hasNext())
        {
        	String key = (String)iter.next();
        	System.out.println( key + ": " + (String)attrs.get(key) );
        }
        // Case 3: retrieve asset and compare bits
        System.out.print("get() file with ID " + id + ": ");
        java.io.FileOutputStream fos = new java.io.FileOutputStream(assetFile+".echo");
        InputStream in = store.get(id);
        Utils.bufferedCopy(in, fos);
        fos.close();
        in.close();
        now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        // Case 4: remove asset
        System.out.print("remove() file with ID: " + id + ": ");
        store.remove(id);
        now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        System.out.flush();
        // should get nothing back now - will throw exception
        store.get(id);
	}
}
