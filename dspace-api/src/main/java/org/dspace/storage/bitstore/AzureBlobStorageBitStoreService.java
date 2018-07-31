/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.bitstore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Required;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.BlockEntry;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * @author Ernani Joppert Pontes Martins
 *
 */
public class AzureBlobStorageBitStoreService implements BitStoreService {

	/** log4j log */
	private static Logger log = Logger.getLogger(AzureBlobStorageBitStoreService.class);

	private final String containerReferenceDefaultPrefix = "container-dspace";
	private final String algorithm = "MD5";

	private String azureAccountName;
	private String azureAccountKey;
	private String azureContainerReference;

	/** (Optional) subfolder within bucket where objects are stored */
	private String subfolder = null;

	private CloudBlobClient serviceClient;
	private CloudBlobContainer blobContainer;

	@Required
	public String getAzureAccountName() {
		return azureAccountName;
	}

	public void setAzureAccountName(String azureAccountName) {
		this.azureAccountName = azureAccountName;
	}

	@Required
	public String getAzureAccountKey() {
		return azureAccountKey;
	}

	public void setAzureAccountKey(String azureAccountKey) {
		this.azureAccountKey = azureAccountKey;
	}

	@Required
	public String getAzureContainerReference() {
		return azureContainerReference;
	}

	public void setAzureContainerReference(String azureContainerReference) {
		// make sure it is always lowercase
		this.azureContainerReference = azureContainerReference.toLowerCase();
	}

	public String getSubfolder() {
		return subfolder;
	}

	public void setSubfolder(String subfolder) {
		this.subfolder = subfolder;
	}

	/**
	 * Default constructor.
	 */
	public AzureBlobStorageBitStoreService() {
	}

	/**
	 * Utility Method: Prefix the key with a subfolder, if this instance assets are
	 * stored within subfolder
	 * 
	 * @param id
	 * @return full key prefixed with a subfolder, if applicable
	 */
	private String getFullKey(String id) {
		if (StringUtils.isNotEmpty(this.subfolder)) {
			return this.subfolder + "/" + id;
		} else {
			return id;
		}
	}

	/**
	 * Initializes the Azure Blob Storage Service.
	 * 
	 * It requires: - Account Name - Account Key - Container Reference/Name
	 */
	@Override
	public void init() throws IOException {
		// check for minimum required parameters
		if (StringUtils.isBlank(getAzureAccountName()) || StringUtils.isBlank(getAzureAccountKey())) {
			log.warn("Empty Azure Blob Storage Account Name or Azure Account Key");
		}

		String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=".concat(getAzureAccountName())
				.concat(";AccountKey=").concat(getAzureAccountKey());

		try {
			CloudStorageAccount azureAccount = CloudStorageAccount.parse(storageConnectionString);
			this.serviceClient = azureAccount.createCloudBlobClient();

			// container reference aka container name
			if (StringUtils.isEmpty(getAzureContainerReference())) {
				String defaultContainerReference = this.containerReferenceDefaultPrefix.concat(
						DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.hostname"));
				log.warn("Azure Container Reference/Name is not configured, setting default: "
						+ defaultContainerReference);
				setAzureContainerReference(defaultContainerReference);
			}

			this.blobContainer = this.serviceClient.getContainerReference(getAzureContainerReference());

			if (!this.blobContainer.exists()) {
				this.blobContainer.create();
				log.info("Creating new Azure Container: ".concat(getAzureContainerReference()));
			}
		} catch (Exception e) {
			log.error(e);
			throw new IOException(e);
		}

		log.info("Azure Blob Storage is ready to go! Container Reference: ".concat(getAzureContainerReference())
				.concat(System.getProperty("line.separator")));
	}

	/**
	 * Returns an identifier unique to this asset store instance
	 * 
	 * @return a unique ID
	 */
	@Override
	public String generateId() {
		return Utils.generateKey();
	}

	/**
	 * Retrieve the bits for the asset with ID. If the asset does not exist, returns
	 * null.
	 * 
	 * @param bitstream
	 *            The ID of the asset to retrieve
	 * @exception java.io.IOException
	 *                If a problem occurs while retrieving the bits
	 *
	 * @return The stream of bits, or null
	 */
	@Override
	public InputStream get(Bitstream bitstream) throws IOException {
		String key = getFullKey(bitstream.getInternalId());
		try {
			CloudBlob blob = this.blobContainer.getBlockBlobReference(key);
			return (blob != null) ? blob.openInputStream() : null;
		} catch (Exception e) {
			log.error("get(" + key + ")", e);
			throw new IOException(e);
		}
	}

	/**
	 * Store a stream of bits.
	 *
	 * <p>
	 * If this method returns successfully, the bits have been stored. If an
	 * exception is thrown, the bits have not been stored.
	 * </p>
	 *
	 * @param in
	 *            The stream of bits to store
	 * @exception java.io.IOException
	 *                If a problem occurs while storing the bits
	 */
	@Override
	public void put(Bitstream bitstream, InputStream inputStream) throws IOException {
		String key = getFullKey(bitstream.getInternalId());
		// Copy istream to temp file, and send the file, with some metadata
		File scratchFile = File.createTempFile(bitstream.getInternalId(), "azureblobstorage");
		try {
			FileUtils.copyInputStreamToFile(inputStream, scratchFile);
			Long contentLength = Long.valueOf(scratchFile.length());

			CloudBlockBlob blob = this.blobContainer.getBlockBlobReference(key);

			this.uploadFileBlocksAsBlockBlob(blob, scratchFile.getAbsolutePath());

			bitstream.setSizeBytes(contentLength);
			bitstream.setChecksum(blob.getProperties().getEtag());
			bitstream.setChecksumAlgorithm(this.algorithm);

			scratchFile.delete();

		} catch (Throwable t) {
			log.error("put(" + bitstream.getInternalId() + ", is)", t);

			StringWriter stringWriter = new StringWriter();
			PrintWriter printWriter = new PrintWriter(stringWriter);
			t.printStackTrace(printWriter);
			if (t instanceof StorageException) {
				if (((StorageException) t).getExtendedErrorInformation() != null) {
					log.error(String.format("\nError: %s",
							((StorageException) t).getExtendedErrorInformation().getErrorMessage()), t);
				}
			}
			log.error(String.format("Exception details:\n%s", stringWriter.toString()), t);
			throw new IOException(t);
		} finally {
			if (scratchFile.exists()) {
				scratchFile.delete();
			}
		}
	}

	/**
	 * Obtain technical metadata about an asset in the asset store.
	 *
	 * Checksum used is 128-bit MD5 digest.
	 *
	 * @param bitstream
	 *            The asset to describe
	 * @param attrs
	 *            A Map whose keys consist of desired metadata fields
	 *
	 * @exception java.io.IOException
	 *                If a problem occurs while obtaining metadata
	 * @return attrs A Map with key/value pairs of desired metadata If file not
	 *         found, then return null
	 */
	@Override
	public Map about(Bitstream bitstream, Map attrs) throws IOException {
		String key = getFullKey(bitstream.getInternalId());
		try {
			CloudBlob blob = this.blobContainer.getBlockBlobReference(key);
			blob.downloadAttributes();

			if (blob != null) {
				BlobProperties blobProps = blob.getProperties();

				if (blobProps != null) {
					Date lastModified = blobProps.getLastModified();
					String lenght = String.valueOf(blobProps.getLength());
					String checksum = blobProps.getEtag();
					String lastModifiedTimeString = null;

					if (lastModified != null) {
						lastModifiedTimeString = String.valueOf(lastModified.getTime());
					}

					String newLine = System.getProperty("file.separator");

					log.debug("blobProperties:" + newLine + "lenght: " + lenght + newLine + "checksum: " + checksum
							+ newLine + "last modified: " + lastModifiedTimeString);

					log.debug("Attributes raw: " + attrs);

					if (attrs != null) {

						log.debug("Attributes as String: " + attrs.toString());

						if (lenght != null && attrs.containsKey("size_bytes")) {
							attrs.put("size_bytes", lenght);
						}

						if (checksum != null && attrs.containsKey("checksum")) {
							attrs.put("checksum", checksum);
							attrs.put("checksum_algorithm", this.algorithm);
						}

						if (lastModifiedTimeString != null && attrs.containsKey("modified")) {
							attrs.put("modified", lastModifiedTimeString);
						}

						return attrs;
					} else {
						// not sure if this would ever happen
						return null;
					}
				}
			}

		} catch (Exception e) {
			log.error("about(" + key + ", attrs)", e);
			throw new IOException(e);
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dspace.storage.bitstore.BitStoreService#remove(org.dspace.content.
	 * Bitstream)
	 */
	@Override
	public void remove(Bitstream bitstream) throws IOException {

		String key = getFullKey(bitstream.getInternalId());
		log.info("Attempting removal of Azure Blob storage file: " + key);

		try {
			CloudBlob blob = this.blobContainer.getBlockBlobReference(key);
			if (blob.exists()) {
				blob.delete();
				if (!blob.exists()) {
					log.info("Removal of Azure Blob storage file: " + key + " was successful");
				}
			} else {
				log.warn("File didn't exist in Azure Blob Storage " + key);
			}
		} catch (Exception e) {
			log.error("remove(" + key + ")", e);
			throw new IOException(e);
		}

	}

	/**
	 * Creates and returns a temporary local file for use by the sample.
	 *
	 * @param blockBlob
	 *            CloudBlockBlob object.
	 * @param filePath
	 *            The path to the file to be uploaded.
	 *
	 * @throws Throwable
	 */
	private void uploadFileBlocksAsBlockBlob(CloudBlockBlob blockBlob, String filePath) throws Throwable {

		FileInputStream fileInputStream = null;
		try {
			// Open the file
			fileInputStream = new FileInputStream(filePath);

			// Split the file into 4mb blocks
			int blockNum = 0;
			String blockId = null;
			String blockIdEncoded = null;
			ArrayList<BlockEntry> blockList = new ArrayList<BlockEntry>();
			while (fileInputStream.available() > (4 * 1024 * 1024)) {
				blockId = String.format("%05d", blockNum);
				blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
				blockBlob.uploadBlock(blockIdEncoded, fileInputStream, (32 * 1024));
				blockList.add(new BlockEntry(blockIdEncoded));
				blockNum++;
			}
			blockId = String.format("%05d", blockNum);
			blockIdEncoded = Base64.getEncoder().encodeToString(blockId.getBytes());
			blockBlob.uploadBlock(blockIdEncoded, fileInputStream, fileInputStream.available());
			blockList.add(new BlockEntry(blockIdEncoded));

			// Commit the blocks
			blockBlob.commitBlockList(blockList);
		} catch (Throwable t) {
			throw t;
		} finally {
			// Close the file output stream writer
			if (fileInputStream != null) {
				fileInputStream.close();
			}
		}
	}
}
