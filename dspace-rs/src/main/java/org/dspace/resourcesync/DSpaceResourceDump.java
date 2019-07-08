/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import org.dspace.core.Context;
import org.openarchives.resourcesync.ResourceDump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
/**
 * @author Richard Jones
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 *
 */
public class DSpaceResourceDump extends DSpaceResourceDocument
{
	protected String metadataChangeFreq = null;
	protected String bitstreamChangeFreq = null;

	public DSpaceResourceDump(Context context)
	{
		super(context);
		this.metadataChangeFreq = this.getMetadataChangeFreq();
		this.bitstreamChangeFreq = this.getBitstreamChangeFreq();
	}

	public DSpaceResourceDump(Context context, List<String> exposeBundles, List<MetadataFormat> mdFormats,
			String mdChangeFreq, String bitstreamChangeFreq)
	{
		super(context, exposeBundles, mdFormats);
		this.metadataChangeFreq = mdChangeFreq;
		this.bitstreamChangeFreq = bitstreamChangeFreq;
	}

	public void serialise(String rdDir, String handle,UrlManager um)
			throws IOException, SQLException
	{
		// this generates the manifest file and zip file
		
		DSpaceResourceDumpZip drl = new DSpaceResourceDumpZip(this.context,rdDir);
		drl.serialise(handle,um); // no output stream required
		// now generate the dump file for the resourcesync framework
		
		ResourceDump rd = new ResourceDump(new Date(), um.capabilityList());
		rd.addResourceZip(um.resourceDumpZip(), new Date(), "application/zip", this.getDumpSize(rdDir));

		String rdFile = rdDir + File.separator + FileNames.resourceDump;
		FileOutputStream fos = new FileOutputStream(new File(rdFile));
		rd.serialise(fos);
		fos.close();
	}
	public synchronized void serialise(String handle,UrlManager um,OutputStream os)
			throws IOException, SQLException
	{
		DSpaceResourceDumpZip drl = new DSpaceResourceDumpZip(this.context,os);
		drl.serialise(handle,um); // no output stream required
		drl.getZos().close();
	}

	private long getDumpSize(String dir)
	{
		String path = dir + File.separator + FileNames.resourceDumpZip;
		File file = new File(path);
		return file.length();
	}
}
