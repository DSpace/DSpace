/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package org.dspace.resourcesync;

import org.dspace.core.Context;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
/**
 * @author Richard Jones 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
public class DSpaceChangeDump extends DSpaceResourceDocument
{
	protected String metadataChangeFreq = null;
	protected String bitstreamChangeFreq = null;

	public DSpaceChangeDump(Context context)
	{
		super(context);
		this.metadataChangeFreq = this.getMetadataChangeFreq();
		this.bitstreamChangeFreq = this.getBitstreamChangeFreq();
	}

	public DSpaceChangeDump(Context context, List<String> exposeBundles, List<MetadataFormat> mdFormats,
			String mdChangeFreq, String bitstreamChangeFreq)
	{
		super(context, exposeBundles, mdFormats);
		this.metadataChangeFreq = mdChangeFreq;
		this.bitstreamChangeFreq = bitstreamChangeFreq;
	}


	public void serialiseChangeDump(String rdDir, UrlManager um,List<ResourceSyncEvent> rseList)
			throws IOException, SQLException
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
		String date = sdf.format(new Date());
		String rdFile = rdDir + File.separator + FileNames.changeDump(date);
		// this generates the manifest file and zip file
		DSpaceChangeDumpZip drl = new DSpaceChangeDumpZip(this.context,rdFile);
		drl.serialise(um,rseList); // no output stream required
	}
	public synchronized void serialiseChangeDump(String handle,UrlManager um,List<ResourceSyncEvent> rseList,OutputStream os)
			throws IOException, SQLException
	{
		DSpaceChangeDumpZip dcl = new DSpaceChangeDumpZip(this.context,os);
		dcl.serialise(um,rseList); // no output stream required
		dcl.getZos().close();
	}
	private long getDumpSize(String dir)
	{
		String path = dir + File.separator + FileNames.resourceDumpZip;
		File file = new File(path);
		return file.length();
	}
}
