/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Asset store using Amazon's Simple Storage Service (S3).
 * and others S3 compatible storage
 * S3 is a commercial, web-service accessible, remote storage facility.
 * NB: you must have obtained an account with Amazon to use this store
 *
 * @author Richard Rodgers, Peter Dietz
 * @author Gerardo Flores Petlacalco
 * @author Alejandra Tenorio Robles
 */

public class S3BitStoreService implements BitStoreService
{
    /** log4j log */
    private static Logger log = Logger.getLogger(S3BitStoreService.class);

    /** Checksum algorithm */
    private static final String CSA = "MD5";

    private String awsAccessKey;
    private String awsSecretKey;
    private String awsRegionName;
    private String awsEndPoint;

    /** container for all the assets */
    private String bucketName = null;

    /** (Optional) subfolder within bucket where objects are stored */
    private String subfolder = null;

    /** S3 service */
    private AmazonS3 s3Service = null;

    /** S3 multipart options **/
    private boolean enableMultipart;
    private long ingestLimit;
    private long minPartSize;

    public S3BitStoreService()
    {
    }

    /**
     * Initialize the asset store
     * S3 Requires:
     *  - access key
     *  - secret key
     *  - bucket name
     */
    public void init() throws IOException {
        if(StringUtils.isBlank(getAwsAccessKey()) || StringUtils.isBlank(getAwsSecretKey())) {
            log.warn("Empty S3 access or secret");
        }

        // init client
        AWSCredentials awsCredentials = new BasicAWSCredentials(getAwsAccessKey(), getAwsSecretKey());
        s3Service = new AmazonS3Client(awsCredentials);

        if (StringUtils.isNotBlank(getAwsEndPoint())) {
            s3Service.setEndpoint(awsEndPoint);
        }


        // bucket name
        if(StringUtils.isEmpty(bucketName)) {
            bucketName = "dspace-asset-" + ConfigurationManager.getProperty("dspace.hostname");
            log.warn("S3 BucketName is not configured, setting default: " + bucketName);
        }

        try {
            if(! s3Service.doesBucketExist(bucketName)) {
                s3Service.createBucket(bucketName);
                log.info("Creating new S3 Bucket: " + bucketName);
            }
        }
        catch (Exception e)
        {
            log.error(e);
            throw new IOException(e);
        }

        // region
        if(StringUtils.isNotBlank(awsRegionName) && StringUtils.isBlank(awsEndPoint)) {
            try {
                Regions regions = Regions.fromName(awsRegionName);
                Region region = Region.getRegion(regions);
                s3Service.setRegion(region);
                log.info("S3 Region set to: " + region.getName());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid aws_region: " + awsRegionName);
            }
        }

        log.info("AWS S3 Assetstore ready to go! bucket:"+bucketName);
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
     * @exception java.io.IOException
     *                If a problem occurs while retrieving the bits
     *
     * @return The stream of bits, or null
     */
    public InputStream get(Bitstream bitstream) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
        try
        {
            S3Object object = s3Service.getObject(new GetObjectRequest(bucketName, key));
            return (object != null) ? object.getObjectContent() : null;
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
     * @exception java.io.IOException
     *             If a problem occurs while storing the bits
     */
    public void put(Bitstream bitstream, InputStream in) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
        //Copy istream to temp file, and send the file, with some metadata
        File scratchFile = File.createTempFile(bitstream.getInternalId(), "s3bs");
        long partSizeBytes = getMinPartSize() * 1024 * 1024;
        long ingestLimitbytes = getIngestLimit() * 1024 * 1024;

        try {
            FileUtils.copyInputStreamToFile(in, scratchFile);
            Long contentLength = Long.valueOf(scratchFile.length());

            log.info("-----------------------------------------");
            log.info("El tamano de este fichero es: " + FileUtils.byteCountToDisplaySize(contentLength));
            log.info("minPartSize: " + FileUtils.byteCountToDisplaySize(minPartSize));
            log.info("partSizeBytes: " + FileUtils.byteCountToDisplaySize(partSizeBytes));
            log.info("enableMultipart: " + Boolean.toString(enableMultipart));
            log.info("ingestLimitbytes: " + FileUtils.byteCountToDisplaySize(ingestLimitbytes));
            log.info("-----------------------------------------");

            if ( getEnableMultipart() && (ingestLimitbytes < contentLength)){
                log.info("Este fichero puede subirse por multipart");
                // Create a list of ETag objects. You retrieve ETags for each object part uploaded,
                // then, after each individual part has been uploaded, pass the list of ETags to
                // the request to complete the upload.
                List<PartETag> partETags = new ArrayList<PartETag>();
                // Initiate the multipart upload.
                InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, key);
                InitiateMultipartUploadResult initResponse = s3Service.initiateMultipartUpload(initRequest);
                long filePosition = 0;
                for (int i = 1; filePosition < contentLength; i++) {
                    // Because the last part could be less than 5 MB, adjust the part size as needed.
                    partSizeBytes = Math.min(partSizeBytes, (contentLength - filePosition));

                    // Create the request to upload a part.
                    UploadPartRequest uploadRequest = new UploadPartRequest()
                            .withBucketName(bucketName)
                            .withKey(key)
                            .withUploadId(initResponse.getUploadId())
                            .withPartNumber(i)
                            .withFileOffset(filePosition)
                            .withFile(scratchFile)
                            .withPartSize(partSizeBytes);

                    // Upload the part and add the response's ETag to our list.
                    UploadPartResult uploadResult = s3Service.uploadPart(uploadRequest);
                    partETags.add(uploadResult.getPartETag());

                    filePosition += partSizeBytes;
                }
                CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, key,
                        initResponse.getUploadId(), partETags);
                CompleteMultipartUploadResult objectResult = s3Service.completeMultipartUpload(compRequest);
                bitstream.setChecksum(objectResult.getETag());
            }
            else
            {
                log.info("Este fichero se subirá normalmente");
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, scratchFile);
                PutObjectResult putObjectResult = s3Service.putObject(putObjectRequest);
                bitstream.setChecksum(putObjectResult.getETag());
            }


            bitstream.setSizeBytes(contentLength);
            bitstream.setChecksumAlgorithm(CSA);

            scratchFile.delete();

        } catch(Exception e) {
            log.error("put(" + bitstream.getInternalId() +", is)", e);
            throw new IOException(e);
        } finally {
            if(scratchFile.exists()) {
                scratchFile.delete();
            }
        }
    }

    /**
     * Obtain technical metadata about an asset in the asset store.
     *
     * Checksum used is (ETag) hex encoded 128-bit MD5 digest of an object's content as calculated by Amazon S3
     * (Does not use getContentMD5, as that is 128-bit MD5 digest calculated on caller's side)
     *
     * @param bitstream
     *            The asset to describe
     * @param attrs
     *            A Map whose keys consist of desired metadata fields
     *
     * @exception java.io.IOException
     *            If a problem occurs while obtaining metadata
     * @return attrs
     *            A Map with key/value pairs of desired metadata
     *            If file not found, then return null
     */
    public Map about(Bitstream bitstream, Map attrs) throws IOException
    {
        String key = getFullKey(bitstream.getInternalId());
        try {
            ObjectMetadata objectMetadata = s3Service.getObjectMetadata(bucketName, key);

            if (objectMetadata != null) {
                if (attrs.containsKey("size_bytes")) {
                    attrs.put("size_bytes", objectMetadata.getContentLength());
                }
                if (attrs.containsKey("checksum")) {
                    attrs.put("checksum", objectMetadata.getETag());
                    attrs.put("checksum_algorithm", CSA);
                }
                if (attrs.containsKey("modified")) {
                    attrs.put("modified", String.valueOf(objectMetadata.getLastModified().getTime()));
                }
                return attrs;
            }
        } catch (AmazonS3Exception e) {
            if(e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
        } catch (Exception e) {
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
     * @exception java.io.IOException
     *             If a problem occurs while removing the asset
     */
    public void remove(Bitstream bitstream) throws IOException
    {
        log.info("-----------------------------------------");
        log.info("Estoy aqui para eliminar datos del bucket");
        log.info("-----------------------------------------");
        String key = getFullKey(bitstream.getInternalId());
        try {
            s3Service.deleteObject(bucketName, key);
        } catch (Exception e) {
            log.error("remove("+key+")", e);
            throw new IOException(e);
        }
    }

    /**
     * Utility Method: Prefix the key with a subfolder, if this instance assets are stored within subfolder
     * @param id
     * @return full key prefixed with a subfolder, if applicable
     */
    public String getFullKey(String id) {
        if(StringUtils.isNotEmpty(subfolder)) {
            return subfolder + "/" + id;
        } else {
            return id;
        }
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public String getAwsEndPoint() {
        return awsEndPoint;
    }

    @Required
    public void setAwsEndPoint(String awsEndPoint) {
        this.awsEndPoint = awsEndPoint;
    }

    @Required
    public void setAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    @Required
    public void setAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
    }

    public String getAwsRegionName() {
        return awsRegionName;
    }

    public void setAwsRegionName(String awsRegionName) {
        this.awsRegionName = awsRegionName;
    }

    @Required
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getSubfolder() {
        return subfolder;
    }

    public void setSubfolder(String subfolder) {
        this.subfolder = subfolder;
    }

    @Required
    public boolean getEnableMultipart(){
        return enableMultipart;
    }

    public void setEnableMultipart(boolean enableMultipart){
        this.enableMultipart = enableMultipart;
    }

    public long getIngestLimit() {
        return ingestLimit;
    }

    public void setIngestLimit(long ingestLimit) {
        this.ingestLimit = ingestLimit;
    }

    public Long getMinPartSize() {
        return minPartSize;
    }

    public void setMinPartSize(Long minPartSize) {
        this.minPartSize = minPartSize;
    }

    /**
     * Contains a command-line testing tool. Expects arguments:
     *  -a accessKey -s secretKey -f assetFileName -e endPoint
     *
     * @param args
     *        Command line arguments
     * @throws Exception if error
     */
    public static void main(String[] args) throws Exception
    {
        CommandLineParser parser = new PosixParser();
        Options options = new Options();

        options.addOption("a", "accessKey", true, "S3 accessKey");
        options.addOption("s", "secretKey", true, "S3 secretKey");
        options.addOption("f", "assetFile", true, "AssetFile");
        options.addOption("e", "endPoint", false, "S3 endPoint");

        CommandLine line = parser.parse(options, args);

        // parse command line
        String assetFile = null;
        String accessKey = null;
        String secretKey = null;
        String endPoint = null;

        if (line.hasOption('a'))
            accessKey = line.getOptionValue('a');
        if (line.hasOption('s'))
            secretKey = line.getOptionValue('s');
        if (line.hasOption('f'))
            assetFile = line.getOptionValue('f');
        if (line.hasOption('e'))
            endPoint = line.getOptionValue('e');

        if (accessKey == null || secretKey == null ||assetFile == null)
        {
            System.out.println("Error: missing arguments, accessKey, secretKey and  assetFile must be specified");
            System.exit(1);
        }
        S3BitStoreService store = new S3BitStoreService();

        AWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

        store.s3Service = new AmazonS3Client(awsCredentials);

        if (endPoint != null) {
            store.s3Service.setEndpoint(endPoint);
        }
        else {
            //Todo configurable region
            Region usEast1 = Region.getRegion(Regions.US_EAST_1);
            store.s3Service.setRegion(usEast1);
        }

        //Bucketname should be lowercase
        store.bucketName = "dspace-asset-" + ConfigurationManager.getProperty("dspace.hostname") + ".s3test";
        store.s3Service.createBucket(store.bucketName);
/* Broken in DSpace 6 TODO Refactor
        // time everything, todo, swtich to caliper
        long start = System.currentTimeMillis();
        // Case 1: store a file
        String id = store.generateId();
        System.out.print("put() file " + assetFile + " under ID " + id + ": ");
        FileInputStream fis = new FileInputStream(assetFile);
        //TODO create bitstream for assetfile...
        Map attrs = store.put(fis, id);
        long now =  System.currentTimeMillis();
        System.out.println((now - start) + " msecs");
        start = now;
        // examine the metadata returned
        Iterator iter = attrs.keySet().iterator();
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
*/
    }
}
