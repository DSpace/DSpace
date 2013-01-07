/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.services.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.core.Utils;
import org.dspace.orm.dao.api.IBitstreamDao;
import org.dspace.orm.dao.api.IBundleDao;
import org.dspace.orm.entity.Bitstream;
import org.dspace.orm.entity.Bundle;
import org.dspace.services.ConfigurationService;
import org.dspace.services.StorageService;
import org.dspace.services.exceptions.StorageException;
import org.springframework.beans.factory.annotation.Autowired;

import edu.sdsc.grid.io.FileFactory;
import edu.sdsc.grid.io.GeneralFile;
import edu.sdsc.grid.io.GeneralFileOutputStream;
import edu.sdsc.grid.io.local.LocalFile;
import edu.sdsc.grid.io.srb.SRBAccount;
import edu.sdsc.grid.io.srb.SRBFile;
import edu.sdsc.grid.io.srb.SRBFileSystem;

/**
 * @author João Melo <jmelo@lyncode.com>
 */
public class DSpaceStorageService implements StorageService {
	private static Logger log = LogManager
			.getLogger(DSpaceStorageService.class);
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

	@Autowired
	ConfigurationService config;
	@Autowired
	IBitstreamDao bitstreamDao;
	@Autowired
	IBundleDao bundleDao;

	private List<Object> assetstores;
	private int incoming;

	/**
	 * Initializes some required objects (assetstores & incoming)
	 */
	private void init() {
		if (assetstores == null) {
			assetstores = new ArrayList<Object>();
			GeneralFile obj = readAssetstore("");
			if (obj == null)
				log.error("No default assetstore");
			else
				assetstores.add(obj);

			for (int i = 0;; i++) {
				obj = readAssetstore("." + i);
				if (obj == null)
					break;
				else
					assetstores.add(obj);
			}

			incoming = Integer.parseInt(config
					.getProperty("assetstore.incoming"));
		}
	}

	/**
	 * Reads one assetstore from configuration
	 * 
	 * @param n
	 * @return File or SRBFile
	 */
	private GeneralFile readAssetstore(String n) {
		String sAssetstoreDir = config.getProperty("assetstore.dir" + n);
		if (sAssetstoreDir != null)
			return new LocalFile(sAssetstoreDir);
		else {
			String srbHost = config.getProperty("srb.host" + n);
			if (srbHost != null) {
				String srbPort = config.getProperty("srb.port" + n);
				String srbUsername = config.getProperty("srb.username" + n);
				String srbPassword = config.getProperty("srb.password" + n);
				String srbH = config.getProperty("srb.homedirectory" + n);
				String srbDm = config.getProperty("srb.mdasdomainname" + n);
				String srbD = config.getProperty("srb.defaultstorageresource"
						+ n);
				String srbZone = config.getProperty("srb.mcatzone" + n);
				SRBAccount acc = new SRBAccount(srbHost,
						Integer.parseInt(srbPort), srbUsername, srbPassword,
						srbH, srbDm, srbD, srbZone);
				SRBFileSystem srbFileSystem = null;
				try {
					srbFileSystem = new SRBFileSystem(acc);
				} catch (NullPointerException e) {
					log.error("No SRBAccount for assetstore " + n);
				} catch (IOException e) {
					log.error("Problem getting SRBFileSystem for assetstore"
							+ n);
				}
				if (srbFileSystem == null) {
					log.error("SRB FileSystem is null for assetstore " + n);
				}
				String sSRBAssetstore = config.getProperty("srb.parentdir" + n);
				if (sSRBAssetstore == null)
					log.error("srb.parentdir is undefined for assetstore " + n);

				return new SRBFile(srbFileSystem, sSRBAssetstore);
			} else {
				return null;
			}
		}
	}

	/**
	 * Return the intermediate path derived from the internal_id. This method
	 * splits the id into groups which become subdirectories.
	 * 
	 * @param iInternalId
	 *            The internal_id
	 * @return The path based on the id without leading or trailing separators
	 */
	private String getIntermediatePath(String iInternalId) {
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

	private GeneralFile getFile(Bitstream bitstream) throws StorageException {
		// Check that bitstream is not null
		if (bitstream == null) {
			return null;
		}

		// Get the store to use
		int storeNumber = bitstream.getStoreNumber();

		// Default to zero ('assetstore.dir') for backwards compatibility
		if (storeNumber == -1) {
			storeNumber = 0;
		}

		// turn the internal_id into a file path relative to the assetstore
		// directory
		String sInternalId = bitstream.getInternalId();

		Object assetstore = assetstores.get(storeNumber);

		// there are 4 cases:
		// -conventional bitstream, conventional storage
		// -conventional bitstream, srb storage
		// -registered bitstream, conventional storage
		// -registered bitstream, srb storage
		// conventional bitstream - dspace ingested, dspace random name/path
		// registered bitstream - registered to dspace, any name/path
		String sIntermediatePath = null;
		if (bitstream.isRegistered()) {
			sInternalId = sInternalId.substring(Bitstream.REGISTERED_FLAG
					.length());
			sIntermediatePath = "";
		} else {
			// Sanity Check: If the internal ID contains a
			// pathname separator, it's probably an attempt to
			// make a path traversal attack, so ignore the path
			// prefix. The internal-ID is supposed to be just a
			// filename, so this will not affect normal operation.
			if (sInternalId.indexOf(File.separator) != -1) {
				sInternalId = sInternalId.substring(sInternalId
						.lastIndexOf(File.separator) + 1);
			}
			sIntermediatePath = this.getIntermediatePath(sInternalId);
		}

		StringBuffer bufFilename = new StringBuffer();
		if (assetstore instanceof File) {
			try {
				bufFilename.append(((File) assetstore).getCanonicalPath());
			} catch (IOException e) {
				throw new StorageException(e);
			}
			bufFilename.append(File.separator);
			bufFilename.append(sIntermediatePath);
			bufFilename.append(sInternalId);
			if (log.isDebugEnabled()) {
				log.debug("Local filename for " + sInternalId + " is "
						+ bufFilename.toString());
			}
			return new LocalFile(bufFilename.toString());
		}
		if (assetstore instanceof SRBFile) {
			bufFilename.append(sIntermediatePath);
			bufFilename.append(sInternalId);
			if (log.isDebugEnabled()) {
				log.debug("SRB filename for " + sInternalId + " is "
						+ ((SRBFile) assetstore).toString()
						+ bufFilename.toString());
			}
			return new SRBFile((SRBFile) assetstore, bufFilename.toString());
		}

		throw new StorageException("Unknonwn assetstore type");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dspace.services.StorageService#retrieve(org.dspace.orm.entity.Bitstream
	 * )
	 */
	@Override
	public InputStream retrieve(Bitstream bitstream) throws StorageException {
		this.init();
		GeneralFile file = getFile(bitstream);
		try {
			return (file != null) ? FileFactory.newFileInputStream(file) : null;
		} catch (IOException e) {
			throw new StorageException(e);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dspace.services.StorageService#cleanup()
	 */
	@Override
	public void cleanup() throws StorageException {
		this.init();
		// FIXME: Implement it
		throw new RuntimeException("Unimplemented method");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.dspace.services.StorageService#delete(org.dspace.orm.entity.Bitstream
	 * )
	 */
	@Override
	public void delete(Bitstream bitstream) throws StorageException {
		this.init();
		
		List<Bundle> primaries = bitstream.getPrimaryBundles();
		for (Bundle b : primaries) {
			b.setPrimary(null);
			bundleDao.save(b);
		}
		
		bitstream.setDeleted(true);
		bitstreamDao.save(bitstream);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dspace.services.StorageService#store(java.io.InputStream)
	 */
	@Override
	public Bitstream store(InputStream input) throws StorageException {
		this.init();
		// Create internal ID
		String id = Utils.generateKey();

		Bitstream bitstream = new Bitstream();
		bitstream.setDeleted(true);
		bitstream.setInternalId(id);
		bitstream.setStoreNumber(incoming);

		bitstreamDao.save(bitstream);

		try {
			GeneralFile file = this.getFile(bitstream);

			if (file != null && file.getParentFile() != null)
				file.getParentFile().mkdirs();

			file.createNewFile();

			GeneralFileOutputStream fos = FileFactory.newFileOutputStream(file);

			// Read through a digest input stream that will work out the MD5
			DigestInputStream dis = null;

			try {
				dis = new DigestInputStream(input,
						MessageDigest.getInstance("MD5"));
			} catch (NoSuchAlgorithmException nsae) // Should never happen
			{
				log.warn("Caught NoSuchAlgorithmException", nsae);
			}

			Utils.bufferedCopy(dis, fos);
			fos.close();
			input.close();

			bitstream.setSize(file.length());

			if (dis != null) {
				bitstream.setChecksum(Utils.toHex(dis.getMessageDigest()
						.digest()));
				bitstream.setChecksumAlgorithm("MD5");
			}

			bitstream.setDeleted(false);
			bitstreamDao.save(bitstream);

			if (log.isDebugEnabled()) {
				log.debug("Stored bitstream " + bitstream.getID() + " in file "
						+ file.getAbsolutePath());
			}

			return bitstream;
		} catch (IOException e) {
			throw new StorageException(e);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.dspace.services.StorageService#register(int, java.lang.String)
	 */
	@Override
	public Bitstream register(int assetstore, String path)
			throws StorageException {
		this.init();

		// mark this bitstream as a registered bitstream
		String sInternalId = Bitstream.REGISTERED_FLAG + path;

		// Create a deleted bitstream row, using a separate DB connection
		Bitstream bitstream = new Bitstream();
		bitstream.setDeleted(true);
		bitstream.setInternalId(sInternalId);
		bitstream.setStoreNumber(assetstore);

		bitstreamDao.save(bitstream);

		// get a reference to the file
		GeneralFile file = getFile(bitstream);

		// read through a DigestInputStream that will work out the MD5
		//
		// DSpace refers to checksum, writes it in METS, and uses it as an
		// AIP filename (!), but never seems to validate with it. Furthermore,
		// DSpace appears to hardcode the algorithm to MD5 in some places--see
		// METSExport.java.
		//
		// To remain compatible with DSpace we calculate an MD5 checksum on
		// LOCAL registered files. But for REMOTE (e.g. SRB) files we
		// calculate an MD5 on just the fileNAME. The reasoning is that in the
		// case of a remote file, calculating an MD5 on the file itself will
		// generate network traffic to read the file's bytes. In this case it
		// would be better have a proxy process calculate MD5 and store it as
		// an SRB metadata attribute so it can be retrieved simply from SRB.
		//
		// TODO set this up as a proxy server process so no net activity

		// FIXME this is a first class HACK! for the reasons described above
		if (file instanceof LocalFile) {

			// get MD5 on the file for local file
			DigestInputStream dis = null;
			try {
				dis = new DigestInputStream(
						FileFactory.newFileInputStream(file),
						MessageDigest.getInstance("MD5"));
			} catch (NoSuchAlgorithmException e) {
				log.warn("Caught NoSuchAlgorithmException", e);
				throw new StorageException("Invalid checksum algorithm", e);
			} catch (IOException e) {
				log.error("File: " + file.getAbsolutePath()
						+ " to be registered cannot be opened - is it "
						+ "really there?");
				throw new StorageException(e);
			}
			final int BUFFER_SIZE = 1024 * 4;
			final byte[] buffer = new byte[BUFFER_SIZE];
			try {
				while (true) {
					final int count = dis.read(buffer, 0, BUFFER_SIZE);
					if (count == -1) {
						break;
					}
				}
				bitstream.setChecksum(Utils.toHex(dis.getMessageDigest()
						.digest()));
				dis.close();
			} catch (IOException e) {
				throw new StorageException(e);
			}
		} else if (file instanceof SRBFile) {
			if (!file.exists()) {
				log.error("File: " + file.getAbsolutePath()
						+ " is not in SRB MCAT");
				throw new StorageException("File is not in SRB MCAT");
			}

			// get MD5 on just the filename (!) for SRB file
			int iLastSlash = path.lastIndexOf('/');
			String sFilename = path.substring(iLastSlash + 1);
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				log.error("Caught NoSuchAlgorithmException", e);
				throw new StorageException("Invalid checksum algorithm", e);
			}
			bitstream.setChecksum(Utils.toHex(md.digest(sFilename.getBytes())));
		} else {
			throw new StorageException("Unrecognized file type - "
					+ "not local, not SRB");
		}

		bitstream.setChecksumAlgorithm("MD5");
		bitstream.setSize(file.length());
		bitstream.setDeleted(false);

		bitstreamDao.save(bitstream);

		if (log.isDebugEnabled()) {
			log.debug("Stored bitstream " + bitstream.getID() + " in file "
					+ file.getAbsolutePath());
		}
		return bitstream;
	}

}
