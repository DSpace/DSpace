/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.openarchives.resourcesync.ResourceSyncDocument;
import org.openarchives.resourcesync.URL;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
/**
 * @author Richard Jones
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
public class DSpaceResourceDumpZip extends DSpaceResourceList {
	private String dumpDir;
	private ZipOutputStream zos;
	private ZipOutputStream zosOnTheFly;
	boolean isOnTheFly = false;
	private OutputStream baos;
    private static Logger log = Logger.getLogger(DSpaceResourceDumpZip.class);

	public DSpaceResourceDumpZip(Context context, String dumpDir) {
		super(context, true);
		this.dumpDir = dumpDir;
		this.isOnTheFly = false;
	}

	public DSpaceResourceDumpZip(Context context,OutputStream os) {
		super(context, true);
		this.isOnTheFly = true;
		this.baos = os;
	}

	public ZipOutputStream getZos() {
		if (isOnTheFly) {
			if (this.zosOnTheFly == null) {
				zosOnTheFly = new ZipOutputStream(baos);
			}
			return zosOnTheFly;
		} else {
			if (this.zos == null) {
				try {
					this.zos = new ZipOutputStream(
							new FileOutputStream(this.dumpDir + File.separator + FileNames.resourceDumpZip));
				} catch (FileNotFoundException e) {
					log.error(e.getMessage(),e);				}
			}
			return zos;
		}
	}
	

	public void serialise(String handle, UrlManager um) throws SQLException, IOException {
		// first generate the manifest file. This uses the other overrides in this
		// object
		// to also copy in the bitstreams and metadata serialisations which are relevant
		// everything will be added to the zip
		if (!isOnTheFly) {
			String drlFile = FileNames.resourceDumpManifest;
			FileOutputStream fos = new FileOutputStream(new File(drlFile));
			this.serialise(fos, handle, um);

			// incorporate the manifest into the zip
			File manifest = new File(drlFile);
			FileInputStream is = new FileInputStream(manifest);
			
			this.copyToZip(FileNames.resourceDumpManifest, is);

			getZos().close();

			// get rid of the left over manifest file
			manifest.delete();
		}
		else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			this.serialise(baos, handle, um);
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			this.copyToZip(FileNames.resourceDumpManifest, bais);
		}


	}

	private void copyToZip(String entryName, InputStream is) throws IOException {
		getZos().putNextEntry(new ZipEntry(entryName));
		byte[] buffer = new byte[102400]; // 100k chunks
		int len = is.read(buffer);
		while (len != -1) {
			getZos().write(buffer, 0, len);
			len = is.read(buffer);
		}
		getZos().closeEntry();
	}

	@Override
	protected URL addBitstream(Bitstream bitstream, Item item, List<Collection> collections, ResourceSyncDocument rl) {
		URL url = super.addBitstream(bitstream, item, collections, rl);
		String dumppath = this.getPath(item, bitstream, null, false);
		url.setPath(dumppath);

		// now actually get the bitstream and stick it in the directory
		try {
			String entryName = this.getPath(item, bitstream, null, true);
			InputStream is = bitstream.retrieve();
			this.copyToZip(entryName, is);
		} catch (IOException e) {
			log.error(e.getMessage(),e);				
		} catch (SQLException e) {
			log.error(e.getMessage(),e);				
		} catch (AuthorizeException e) {
			log.error(e.getMessage(),e);				
		}

		return url;
	}

	private String getPath(Item item, Bitstream bitstream, MetadataFormat format, boolean nativeSeparator) {
		String separator = nativeSeparator ? File.separator : "/";
		String itempath = item.getHandle().replace("/", "_");

		String filepath;
		if (bitstream != null) {
			filepath = Integer.toString(bitstream.getSequenceID()) + "_" + bitstream.getName();
		} else if (format != null) {
			filepath = format.getPrefix();
		} else {
			throw new RuntimeException("must provide either bitstream or metadata format");
		}
		String dumppath = separator + FileNames.dumpResourcesDir + separator + itempath + separator + filepath;
		return dumppath;
	}

	@Override
	protected URL addMetadata(Item item, MetadataFormat format, List<Bitstream> describes, List<Collection> collections,
			ResourceSyncDocument rl) {
		URL url = super.addMetadata(item, format, describes, collections, rl);
		String dumppath = this.getPath(item, null, format, false);
		url.setPath(dumppath);

		// now actually get the metadata export and stick it in the directory
		try {
			String entryName = this.getPath(item, null, format, true);
			ZipEntry e = new ZipEntry(entryName);
			getZos().putNextEntry(e);

			// get the dissemination crosswalk for this prefix and get the element for the
			// object
			MetadataDisseminator.disseminate(item, format.getPrefix(), getZos());

			getZos().closeEntry();
		} catch (IOException e) {
			log.error(e.getMessage(),e);				
		} catch (SQLException e) {
			log.error(e.getMessage(),e);				
		} catch (AuthorizeException e) {
			log.error(e.getMessage(),e);				
		} catch (CrosswalkException e) {
			log.error(e.getMessage(),e);				
		}

		return url;
	}
}