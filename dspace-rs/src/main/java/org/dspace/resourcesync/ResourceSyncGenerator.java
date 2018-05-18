/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.SiteService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.openarchives.resourcesync.ResourceSync;
import org.openarchives.resourcesync.ResourceSyncDescription;
import org.openarchives.resourcesync.ResourceSyncDescriptionIndex;
/**
 * @author Richard Jones
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
public class ResourceSyncGenerator
{
	private static Logger log = Logger.getLogger(ResourceSyncGenerator.class);
	
	public static void main(String[] args)
			throws Exception
	{
		Options options = new Options();
		options.addOption("i", "init", false, "Create a fresh ResourceSync description of this repository - this will remove any previous ResourceSync documents");
		options.addOption("u", "update", false, "Update the Change List ResourceSync document with the changes since this script last ran");
		options.addOption("r", "rebase", false, "Update the Resource List ResourceSync document to reflect the current state of the archive, and bring the Change List up to the same level");
		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse( options, args);

		Context context = new Context();
		List<String> handles = buildHandleForResourceSync(context);

		SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
		String siteHandle = siteService.findSite(context).getHandle();
		ResourceSyncGenerator rsg = new ResourceSyncGenerator(context,siteHandle, handles, null);

		try
		{
			if (cmd.hasOption("i"))
			{
				rsg.init(context);
			}
			else if (cmd.hasOption("u"))
			{
				rsg.update(context);
			}
			else if (cmd.hasOption("r"))
			{
				rsg.rebase(context);
			}
			else
			{
				HelpFormatter hf = new HelpFormatter();
				hf.printHelp("ResourceSyncGenerator", "Manage ResourceSync documents for DSpace", options, "");
			}
		}
		finally
		{
			context.abort();
		}
	}

	private Map<String, UrlManager> ums;
	public static final  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	public static final  SimpleDateFormat sdfChangeList = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
	private Context context;
	private boolean resourceDump = false;
	private boolean changeDump = false;
	private String outdir;
	private List<String> handles = null;
	private Date fromChangeDump = null; 
	private String siteHandle;
	
	public static List<String> buildHandleForResourceSync(Context context) throws SQLException {
		SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
		CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
		CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
		
		String capabilityList = ConfigurationManager.getProperty("resourcesync", "capabilitylists");
		String handleSite = siteService.findSite(context).getHandle();
		
		List<String> handles = new ArrayList<String>();
		if(capabilityList.equals("top"))
		{
			List<Community> communities = communityService.findAllTop(context);
			for (Community c : communities)
			{
				handles.add(c.getHandle());
			}
			handles.add(handleSite);
		}
		else if(capabilityList.equals("all"))
		{
			handles.add(handleSite);
			List<Community> communities = communityService.findAll(context);
			for (Community c : communities) 
			{
				handles.add(c.getHandle());
			}
			List<Collection> collections = collectionService.findAll(context);
			for (Collection c : collections) 
			{
				handles.add(c.getHandle());
			}
		}
		else if(capabilityList.equals("site"))
		{
			handles.add(handleSite);
		}
		else
		{

			String[] handle = capabilityList.split("\\s+");

			for (String h : handle) {
				DSpaceObject dso = HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, h);
				if (dso == null) 
				{
					log.error("The handle isn't valid "+handle);
				}
				else if (dso.getType() == Constants.ITEM) 
				{
					log.error("Can't use ResourceSync with handle of item "+h);
				}
				else 
				{
					handles.add(h);
				}
			}
		}
		return handles;
	}

	
	
	
	public ResourceSyncGenerator(Context context, String siteHandle, List<String> handles, Date fromChangeDump)
			throws IOException
	{
		this.siteHandle = siteHandle;
		this.handles = handles;
		this.ums = new HashMap<String, UrlManager>();
		for (String h : handles) 
		{
			this.ums.put(h, new UrlManager(siteHandle, h));	
		}
		this.context = context;
		this.resourceDump = ConfigurationManager.getBooleanProperty("resourcesync", "resourcedump.enable");

		this.outdir = ConfigurationManager.getProperty("resourcesync", "resourcesync.dir");
		if (this.outdir == null)
		{
			throw new IOException("No configuration for resourcesync.dir");
		}
		this.fromChangeDump = fromChangeDump;
	}

	//////////////////////////////////////////////////////////////////////////////
	// methods to be used to interact with the generator
	//////////////////////////////////////////////////////////////////////////////

	public void init(Context context)
			throws IOException, SQLException, ParseException
	{
		this.changeDump = false;
		// make sure that the directory exists, and that it is empty
		this.ensureResourceSyncDirectory();
		this.emptyResourceSyncDirectory();

		// generate the description index document
		this.generateResourceSyncDescriptionIndex(handles);
		for (String handle : this.handles) {

			// generate the resource list
			this.generateResourceList(handle);

			// generate the description document
			this.generateResourceSyncDescription(handle);

			// should we generate a resource dump?
			if (this.resourceDump)
			{
				this.generateResourceDump(handle);
			}
			// generate the capability list (with a resource list, without a change list, and maybe with a resource dump)
			this.generateCapabilityList(true, false, this.resourceDump, false,this.changeDump,handle);

			// generate the blank changelist as a placeholder for the next iteration
			this.generateBlankChangeList(handle);
		}
	}

	public void update(Context context)
			throws IOException, SQLException, ParseException
	{
		
		this.changeDump = true;

		// check the directory is there
		this.ensureResourceSyncDirectory();
		
		// generate the latest changelist
		//		String clFilename = this.generateLatestChangeList();

		// add to the change list archive
		String clFilename = null;
		HashMap<String,String> clFilenameList = new HashMap<String,String>();
		for (String handle : this.handles) 
		{
			
			List<ResourceSyncEvent> rseListFiltered = new ArrayList<ResourceSyncEvent>();
			rseListFiltered = getChange(handle);
			// generate the latest changelist
			clFilename = this.generateLatestChangeList(handle,rseListFiltered);
			clFilenameList.put(handle,clFilename);

			if (this.resourceDump)
			{
				this.generateChangeDump(handle,rseListFiltered);
				//this.generateResourceDump(handle);
			}


			// update the last modified date in the capability list (and add the
			// changelistarchive if necessary)
			this.updateCapabilityList(handle);
		}
		this.addChangeListToArchive(clFilenameList,handles);
	}

	public void rebase(Context context)
			throws IOException, SQLException, ParseException
	{
		this.changeDump = false;
		// make sure that the directory exists, and that it is empty
		this.ensureResourceSyncDirectory();
		this.emptyResourceSyncDirectory();

		// generate the description index document
		this.generateResourceSyncDescriptionIndex(handles);
		
		String clFilename = null;
		HashMap<String,String> clFilenameList = new HashMap<String,String>();
		
		for (String handle : this.handles) {

			// generate the resource list
			this.generateResourceList(handle);

			// generate the description document
			this.generateResourceSyncDescription(handle);

			List<ResourceSyncEvent> rseListFiltered = new ArrayList<ResourceSyncEvent>();
			rseListFiltered = getChange(handle);
			// generate the latest changelist
			clFilename = this.generateLatestChangeList(handle,rseListFiltered);
			clFilenameList.put(handle,clFilename);
			
			// should we generate a resource dump?
			if (this.resourceDump)
			{
				this.generateResourceDump(handle);
			}
			// generate the capability list (with a resource list, without a change list, and maybe with a resource dump)
			this.updateCapabilityList(handle);
		}
		this.addChangeListToArchive(clFilenameList,handles);
	}
	
	
	public List<ResourceSyncEvent> getChange(String handle) {
		Date from = null;
		try {
			if (fromChangeDump == null)
			{
				from = this.getLastChangeListDate(handle);
			}else
			{
				from = fromChangeDump;
			}
		} catch (ParseException e) {
        	log.error(e.getMessage(),e);
		}
		Date to = new Date();
		List<ResourceSyncEvent> rseList = new ArrayList<ResourceSyncEvent>();
		ResourceSyncAuditService auditService = new ResourceSyncAuditService();
		rseList = auditService.listEvents(from, to, handle);
		return rseList;
	}



	//////////////////////////////////////////////////////////////////////
	// file management utility methods
	//////////////////////////////////////////////////////////////////////

	private void ensureResourceSyncDirectory()
			throws IOException
	{
		// make sure our output directory exists
		this.ensureDirectory(this.outdir);
	}

	private void ensureDirectory(String dir)
			throws IOException
	{
		File od = new File(dir);
		if (!od.exists())
		{
			od.mkdir();
		}
		if (!od.isDirectory())
		{
			throw new IOException(dir + " exists, but is not a directory");
		}
	}

	private void emptyResourceSyncDirectory()
			throws IOException
	{
		this.emptyDirectory(this.outdir);
	}
	private void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if(files!=null) {
			for(File f: files) {
				if(f.isDirectory()) {
					deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}
	private void emptyDirectory(String outdir)
	{
		File out = new File(outdir);
		File[] files = out.listFiles();
		if (files == null)
		{
			return;
		}
		for (File f : files)
		{
			if(f.isDirectory()) {
				deleteFolder(f);
			} else {
				f.delete();            }
		}
	}

	private FileOutputStream getFileOutputStream(String filename)
			throws IOException
	{
		FileOutputStream fos = new FileOutputStream(new File(this.outdir+File.separator+filename));
		return fos;
	}

	private FileOutputStream getFileOutputStream(String filename, String handle)
			throws IOException
	{
		String rsdFile = getOutdir(handle) + File.separator + filename;
		FileOutputStream fos = new FileOutputStream(new File(rsdFile));
		return fos;
	}

	private String getOutdir(String handle) {
		if (handle != null && !(siteHandle.equals(handle))) {
			return this.outdir + File.separator + handle.replaceAll("/", "-");
		}
		return this.outdir;
	}


	private void deleteFile(String filename)
	{
		File old = new File(this.outdir + File.separator + filename);
		if (old.exists())
		{
			old.delete();
		}
	}
	private String getLastChangeDumpName(String handle)
			throws ParseException
	{
		String filename = null;
		Date from = new Date(0);
		File dir;
		if (handle != siteHandle)
		{
			dir = new File(this.outdir+File.separator+handle.replace("/", "-"));
		}
		else{
			dir = new File(this.outdir);
		}
		File[] files = dir.listFiles();
		if (files != null)
		{
			for (File f : dir.listFiles())
			{
				if (FileNames.isChangeDump(f))
				{
					String dr = FileNames.changeDumpDate(f);
					Date possibleFrom = ResourceSyncGenerator.sdfChangeList.parse(dr);
					if (possibleFrom.getTime() > from.getTime())
					{
						from = possibleFrom;
						filename = f.getName();
					}
				}
			}
		}
		return filename;
	}
	private String getLastChangeListName(String handle)
			throws ParseException
	{
		String filename = null;
		Date from = new Date(0);
		File dir;
		if (handle != siteHandle)
		{
			dir = new File(this.outdir+File.separator+handle.replace("/", "-"));
		}
		else{
			dir = new File(this.outdir);
		}
		File[] files = dir.listFiles();
		if (files != null)
		{
			for (File f : dir.listFiles())
			{
				if (FileNames.isChangeList(f))
				{
					String dr = FileNames.changeListDate(f);
					Date possibleFrom = ResourceSyncGenerator.sdfChangeList.parse(dr);
					if (possibleFrom.getTime() > from.getTime())
					{
						from = possibleFrom;
						filename = f.getName();
					}
				}
			}
		}
		return filename;
	}

	private Date getLastChangeListDate(String handle)
			throws ParseException
	{
		File dir = new File(getOutdir(handle));
		Date from = new Date(0);
		File[] files = dir.listFiles();
		if (files != null)
		{
			for (File f : dir.listFiles())
			{
				if (FileNames.isChangeList(f))
				{
					String dr = FileNames.changeListDate(f);
					Date possibleFrom = ResourceSyncGenerator.sdfChangeList.parse(dr);
					if (possibleFrom.getTime() > from.getTime())
					{
						from = possibleFrom;
					}
				}
			}
		}
		return from;
	}

	private boolean fileExists(String filename)
	{
		String path = this.outdir + File.separator + filename;
		File file = new File(path);
		return file.exists() && file.isFile();
	}

	////////////////////////////////////////////////////////////////////
	// private document generation methods
	////////////////////////////////////////////////////////////////////

	private void generateResourceSyncDescriptionIndex( List<String> handles)
			throws IOException
	{
		FileOutputStream fos = this.getFileOutputStream(FileNames.resourceSyncDocumentIndex);

		// no need for a DSpace-specific implementation, it is so simple
		ResourceSyncDescriptionIndex desc = new ResourceSyncDescriptionIndex(ResourceSync.CAPABILITY_RESOURCESYNC);
		for (String h: handles)
		{
			desc.addSourceDescription((this.ums.get(h).resourceSyncDescription()));
		}
		desc.serialise(fos);
		
		fos.close();
		
	}

	private void generateResourceSyncDescription(String handle)
			throws IOException
	{
		FileOutputStream fos = this.getFileOutputStream(FileNames.resourceSyncDocument,handle);

		// no need for a DSpace-specific implementation, it is so simple
		ResourceSyncDescription desc = new ResourceSyncDescription();
		desc.addCapabilityList(this.ums.get(handle).capabilityList());
		desc.serialise(fos);

		fos.close();
	}



	private void updateCapabilityList(String handle)
			throws IOException, ParseException
	{
		// just regenerate the capability list in its entirity
		this.generateCapabilityList(true, false, this.resourceDump, true,this.changeDump,handle);
	}

	private void generateCapabilityList(boolean resourceList, boolean changeListArchive, boolean resourceDump,
			boolean changeList,boolean changeDump,String handle)
					throws IOException, ParseException
	{
		// get the latest change list if there is one and we want one
		String changeListUrl = null;
		String changeDumpUrl = null;
		if (changeList)
		{
			String clFilename = this.getLastChangeListName(handle);
			changeListUrl = this.ums.get(handle).changeList(clFilename);
			
		}
		if (changeDump)
		{
			String cdFilename = this.getLastChangeDumpName(handle);
			changeDumpUrl = this.ums.get(handle).changeDump(cdFilename);
		}
		FileOutputStream fos = this.getFileOutputStream(FileNames.capabilityList,handle);

		DSpaceCapabilityList dcl = new DSpaceCapabilityList(this.context, resourceList, changeListArchive,
				resourceDump, changeList, changeDump, changeListUrl,changeDumpUrl,ums.get(handle));
		dcl.serialise(fos);

		fos.close();
	}

	private void generateResourceList(String handle)
			throws SQLException, IOException
	{
		String directoryName;
		String path;
		if (!handle.equals(siteHandle))
		{
			directoryName = handle.replace("/", "-");
			path = ConfigurationManager.getProperty("resourcesync", "resourcesync.dir");
			path = path.concat("/"+directoryName);		
			File directory = new File(path);
			directory.mkdir();
		}
		FileOutputStream fos = this.getFileOutputStream(File.separator+FileNames.resourceList,handle);

		DSpaceResourceList drl = new DSpaceResourceList(this.context);
		drl.serialise(fos,handle,this.ums.get(handle));

		fos.close();
	}
    
	private void generateResourceDump(String handle)
			throws IOException, SQLException
	{
		if (handle.equals(siteHandle))
		{
			this.deleteFile(FileNames.resourceDump);
			this.deleteFile(FileNames.resourceDumpZip);
		}
		DSpaceResourceDump drd = new DSpaceResourceDump(this.context);
		drd.serialise(getOutdir(handle), handle, ums.get(handle));
	}
	private void generateChangeDump(String handle,List<ResourceSyncEvent> rseList)
			throws IOException, SQLException
	{
		DSpaceChangeDump drd = new DSpaceChangeDump(this.context);
		drd.serialiseChangeDump(getOutdir(handle), ums.get(handle),rseList);
	}
	public void generateChangeDump(String handle,List<ResourceSyncEvent> rseList,OutputStream os)
			throws IOException, SQLException
	{
		DSpaceChangeDump drd = new DSpaceChangeDump(this.context);
		drd.serialiseChangeDump(getOutdir(handle), ums.get(handle),rseList,os);
	}

	private String generateLatestChangeList(String handle,List<ResourceSyncEvent> rseListFiltered)
			throws ParseException, IOException, SQLException
	{
		
		Date from = this.getLastChangeListDate(handle);
		Date to = new Date();
		String tr = sdfChangeList.format(to);
		String filename = FileNames.changeList(tr);
		FileOutputStream fos = this.getFileOutputStream(filename,handle);
		DSpaceChangeList dcl = new DSpaceChangeList(this.context, from, to,ums.get(handle)); //TO-DO
		dcl.serialiseForDump(fos,ums.get(handle),rseListFiltered);

		return filename;
	}

	private void generateBlankChangeList(String handle)
			throws IOException, SQLException, ParseException
	{

		Date to = new Date();
		String tr = ResourceSyncGenerator.sdfChangeList.format(to);

		FileOutputStream fos = this.getFileOutputStream(FileNames.changeList(tr),handle);

		// generate the changelist for the period (which is of 0 length)
		DSpaceChangeList dcl = new DSpaceChangeList(this.context, to, to,ums.get(handle));
		dcl.serialise(fos);
		fos.close();
	}

	private void addChangeListToArchive(HashMap<String,String> filename,List<String> handles)
			throws IOException, ParseException
	{
		
		DSpaceChangeListArchive dcla = new DSpaceChangeListArchive(this.context);
		dcla.setUm(ums.get(siteHandle));
		FileOutputStream fos = this.getFileOutputStream(FileNames.changeListArchive,siteHandle);
		
		for(String handle : handles)
		{
			// get the URL of the new changelist
			String loc = this.ums.get(handle).changeList(filename.get(handle));

			// get the date of the new changelist (it is encoded in the filename)
			String dr = FileNames.changeListDate(filename.get(handle));
			Date date = ResourceSyncGenerator.sdfChangeList.parse(dr);

			dcla.addChangeList(loc, date);
		}
		dcla.serialise(fos);

		fos.close();
	}
}
