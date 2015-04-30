/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.dspace.content.Bitstream;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.storage.bitstore.BitStore;

/**
 * Asset store using Amazon's Simple Storage Service (S3).
 * S3 is a commercial, web-service accessible, remote storage facility.
 * This implementation uses jetS3t (a java client library) with S3's RESTful interface
 * NB: you must have obtained an account with Amazon to use this store
 * 
 * @author Richard Rodgers
 */ 

public class S3BitStore implements BitStore
{
    /** log4j log */
    private static Logger log = Logger.getLogger(S3BitStore.class);
    
    /** Checksum algorithm */
    private static final String CSA = "MD5";
    
    /** container for all the assets */
	private S3Bucket bucket = null;
	
	/** S3 service */
	private S3Service service = null;
		
	public S3BitStore()
	{
	}
	
	/**
     * Initialize the asset store
     * 
     * @param config
     *        String used to characterize configuration - may be a configuration
     *        value, or the name of a config file containing such values
     */
	public void init(String config) throws IOException
	{
		// use DSpace host name as bucket name - probably should be more unique
		String bucketName = ConfigurationManager.getProperty("dspace.host");
		
		//  params string contains just the filename of the AWT account data
		Properties props = new Properties();
		try
		{
		    props.load(new FileInputStream(config));
		}
		catch(Exception e)
		{
			throw new IOException("Exception loading properties: " + e.getMessage());
		}
        String awsAccessKey = props.getProperty("access.key");
        String awsSecretKey = props.getProperty("secret.key");
        AWSCredentials creds = new AWSCredentials(awsAccessKey, awsSecretKey);
        try
		{
            service = new RestS3Service(creds);
            // verify that we have a bucket to use
            bucket = service.createBucket(bucketName);
		}
        catch (S3ServiceException s3se)
		{
        	throw new IOException("S3ServiceException: " + s3se.getS3ErrorMessage());
		}
	}
	
	/**
     * Return an identifier unique to this asset store instnace
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
			object = service.getObject(bucket, id);
			return (object != null) ? object.getDataInputStream() : null;
		}
        catch (org.jets3t.service.ServiceException s3se)
		{
        	throw new IOException("S3ServiceException: " + s3se.getMessage());
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
     * @param context
     *            The current context
     * @param in
     *            The stream of bits to store
     * @exception IOException
     *             If a problem occurs while storing the bits
     * 
     * @return Map containing technical metadata (size, checksum, etc)
     */
	public Map put(InputStream in, String id) throws IOException
	{
		S3Object object = new S3Object(id);
		object.setDataInputStream(in);
		try
		{
			object = service.putObject(bucket, object);
		}
        catch (S3ServiceException s3se)
		{
        	throw new IOException("S3ServiceException: " + s3se.getS3ErrorMessage());
		}
     
        Map attrs = new HashMap();
	    attrs.put(Bitstream.SIZE_BYTES, String.valueOf(object.getContentLength()));
	    attrs.put(Bitstream.CHECKSUM, object.getETag());
	    attrs.put(Bitstream.CHECKSUM_ALGORITHM, CSA);
	    attrs.put("modified", 
	    	      String.valueOf(object.getLastModifiedDate().getTime()));
        return attrs;
	}
	
    /**
     * Obtain technical metadata about an asset in the asset store.
     * 
     * @param context
     *            The current context
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
		S3Object object = null;
		try
		{
			// NB: this invocation only retrieves metadata
			object = service.getObjectDetails(bucket, id);
			if (object != null)
			{
			    if (attrs.containsKey(Bitstream.SIZE_BYTES))
			    {
			        attrs.put(Bitstream.SIZE_BYTES, String.valueOf(object.getContentLength()));
			    }
			    if (attrs.containsKey(Bitstream.CHECKSUM))
			    {
			    	// WARNING! Amazon docs indicate that ETag value
			    	// may not always contain the MD-5 checksum
				    attrs.put(Bitstream.CHECKSUM, object.getETag());
				    attrs.put(Bitstream.CHECKSUM_ALGORITHM, CSA);
			    }
				if (attrs.containsKey("modified"))
				{
				    attrs.put("modified", 
				    	      String.valueOf(object.getLastModifiedDate().getTime()));
				}			
				return attrs;
			}
			return null;
		}
        catch (S3ServiceException s3se)
		{
        	throw new IOException("S3ServiceException: " + s3se.getS3ErrorMessage());
		}
	}
	
    /**
     * Remove an asset from the asset store. An irreversible operation.
     * 
     * @param context
     *            The current context
     * @param id
     *            The ID of the asset to delete
     * @exception IOException
     *             If a problem occurs while removing the asset
     */
	public void remove(String id) throws IOException
	{
		try
		{
			service.deleteObject(bucket, id);
		}
        catch (S3ServiceException s3se)
		{
        	throw new IOException("S3ServiceException: " + s3se.getS3ErrorMessage());
		}
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
        AWSCredentials creds = new AWSCredentials(accessKey, secretKey);
        try
		{
            store.service = new RestS3Service(creds);
            // verify/make a bucket to use
            store.bucket = store.service.createBucket(accessKey + ".s3test");
		}
        catch (S3ServiceException s3se)
		{
        	throw new IOException("S3ServiceException: " + s3se.getS3ErrorMessage());
		}
        // time everything
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
