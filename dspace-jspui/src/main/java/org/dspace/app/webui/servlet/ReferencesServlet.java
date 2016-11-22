/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.itemexport.ItemExport;
import org.dspace.app.itemexport.ItemExportException;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.FileNameDisseminator;
import org.dspace.content.integration.crosswalks.StreamGenericDisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.utils.DSpace;

/**
 * Servlet for export in references format
 * 
 * @author bollini
 * 
 * @version $Revision: 1.1 $
 */
public class ReferencesServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(ReferencesServlet.class);
    
    boolean exportBiblioEnabled =  ConfigurationManager.getBooleanProperty("exportcitation.list.enabled", false);

	boolean exportBiblioAll =  ConfigurationManager.getBooleanProperty("exportcitation.show.all", false);


    DSpace dspace = new DSpace();
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
		if (UIUtil.getBoolParameter(request, "refworks")) {
			log.info(LogManager.getHeader(context, "references",
					"refworks callback"));
		}
        doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        int[] item_ids = null;
        
        String prefix = request.getParameter("prefix");
        if(StringUtils.isNotEmpty(prefix)) {
            item_ids = UIUtil.getIntParameters(request, prefix+"item_id");
        }
        else {
            item_ids = UIUtil.getIntParameters(request, "item_id");            
        }
		
        String format = request.getParameter("format");
        boolean fulltext = UIUtil.getBoolParameter(request, "fulltext");
        boolean email = UIUtil.getBoolParameter(request, "email");

        if (fulltext && context.getCurrentUser() == null)
        {
            throw new AuthorizeException(
                    "Only logged user can export item citations with fulltext");
        }
		if (format == null || format.equals("") || item_ids == null
				|| item_ids.length == 0)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        else if (format.equalsIgnoreCase("refworks"))
        {
            // if request is export to RefWorks we use callback implementation
            // see http://www.refworks.com/DirectExport.htm
            StringBuffer redirectURLCallBack = new StringBuffer(
                    "http://www.refworks.com/express/ExpressImport.asp?vendor="
                            + URLEncoder.encode(ConfigurationManager
                                    .getProperty("dspace.name"))
                            + "&filter="
                            + URLEncoder.encode("RIS Format")
                            + "&encoding=65001&url="
                            + URLEncoder.encode(ConfigurationManager
                                    .getProperty("dspace.url")
									+ "/references?format=refman&refworks=true"));

			for (int id : item_ids)
            {
		        if(StringUtils.isNotEmpty(prefix)) {
		            redirectURLCallBack.append(URLEncoder.encode("&"+prefix+"item_id=" + id));
		        }
		        else {
		            redirectURLCallBack.append(URLEncoder.encode("&item_id=" + id));            
		        }
				
            }
            response.sendRedirect(redirectURLCallBack.toString());
            return;
        }
        else
        {
            List<Item> items = new LinkedList<Item>();
			for (int handle : item_ids)
            {
				Item dso = Item.find(context, handle);

                // Make sure we have valid item
				if (dso != null)
                {
					items.add(dso);
                }
            }
            try
            {
                process(context, request, response, items, format, fulltext,
                        email);
            }
            catch (Exception e)
            {
                log.error(LogManager.getHeader(context, "process_request",
                        "format=" + format + ", fulltext=" + fulltext
                                + ", email=" + email), e);
            }
        }
        context.abort();
    }

    private void process(Context context, HttpServletRequest request,
            HttpServletResponse response, final List<Item> items,
            final String format, final boolean fulltext, boolean email)
            throws Exception
    {
        boolean async = email || fulltext;
        final StreamDisseminationCrosswalk streamCrosswalkDefault = (StreamDisseminationCrosswalk) PluginManager
                .getNamedPlugin(StreamDisseminationCrosswalk.class, format);
        if (streamCrosswalkDefault == null)
        {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        final EPerson eperson = context.getCurrentUser();
        if (!async)
        {
            String mimeType = streamCrosswalkDefault.getMIMEType();
            // Set the response MIME type
            if (mimeType != null)
            {
                String ext = ConfigurationManager.getProperty("crosswalk.refer."+format+".file.ext");
                response.setContentType(mimeType);
                if(streamCrosswalkDefault instanceof FileNameDisseminator) {
                    response.setHeader("Content-Disposition",
                    "attachment;filename="+((FileNameDisseminator)streamCrosswalkDefault).getFileName()+"-"+format+(StringUtils.isNotBlank(ext)?"."+ext:""));
                }
                else {
                    response.setHeader("Content-Disposition",
                        "attachment;filename=references-" + format+(StringUtils.isNotBlank(ext)?"."+ext:""));
                }

            }
            OutputStream outputStream = response.getOutputStream();
            buildReferenceStream(context, items, format, outputStream,
                    streamCrosswalkDefault);
        }
        else
        {
            Thread go = new LocalThread(eperson, format, fulltext, items,
                    streamCrosswalkDefault);
            go.run();            
            JSPManager.showJSP(request, response, "message.jsp");
        }
    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send a success email once the export
     * archive is complete and ready for download
     * 
     * @param context
     *            - the current Context
     * @param eperson
     *            - eperson to send the email to
     * @param fileName
     *            - the file name to be downloaded. It is added to the url in
     *            the email
     * @throws MessagingException
     */
    public static void emailSuccessMessage(Context context, EPerson eperson,
            String fileName, File referencesFile) throws MessagingException
    {
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil
                    .getEmailFilename(supportedLocale,
                            "export_references_success"
                                    + (fileName != null ? "_fulltext" : "")));
            email.addRecipient(eperson.getEmail());
            email.addAttachment(referencesFile, "references");
            email.addArgument(ConfigurationManager.getProperty("dspace.url")
                    + "/exportdownload/" + fileName + "/" + fileName + ".zip");
            email.addArgument(ConfigurationManager
                    .getProperty("org.dspace.app.itemexport.life.span.hours"));

            email.send();
        }
        catch (Exception e)
        {
            log.warn(LogManager.getHeader(context, "emailSuccessMessage",
                    "cannot notify user of export"), e);
        }
    }

    /**
     * Since the archive is created in a new thread we are unable to communicate
     * with calling method about success or failure. We accomplis this
     * communication with email instead. Send an error email if the export
     * archive fails
     * 
     * @param eperson
     *            - EPerson to send the error message to
     * @param error
     *            - the error message
     * @throws MessagingException
     */
    public static void emailErrorMessage(EPerson eperson, String error)
            throws MessagingException
    {
        log.warn("An error occured during item export, the user will be notified. "
                + error);
        try
        {
            Locale supportedLocale = I18nUtil.getEPersonLocale(eperson);
            Email email = Email.getEmail(I18nUtil
                    .getEmailFilename(supportedLocale,
                            "export_references_error"));
            email.addRecipient(eperson.getEmail());
            email.addArgument(error);
            email.addArgument(ConfigurationManager.getProperty("dspace.url")
                    + "/feedback");

            email.send();
        }
        catch (Exception e)
        {
            log.warn("error during item export error notification", e);
        }
    }

    private String buildFulltextZipFile(Context context, List<Item> items,
            String format, File wkDir, File downloadDir, String fileName)
            throws Exception
    {
        EPerson eperson = context.getCurrentUser();
        // before we create a new export archive lets delete the 'expired'
        // archives
        ItemExport.deleteOldExportArchives(eperson.getID());
        long size = 0;
        for (Item item : items)
        {
            Bundle[] bnds = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
            for (Bundle bnd : bnds)
            {
                Bitstream[] bs = bnd.getBitstreams();
                for (Bitstream b : bs)
                {
                    if (AuthorizeManager.authorizeActionBoolean(context, b,
                            Constants.READ))
                    {
                        size += b.getSize();
                    }
                }
            }
        }

        if (size == 0)
        { // no fulltext available
            return null;
        }
        // check the size of all the bitstreams against the configuration file
        // entry if it exists
        String megaBytes = ConfigurationManager
                .getProperty("org.dspace.app.itemexport.max.size");
        if (megaBytes != null)
        {
            float maxSize = 0;
            try
            {
                maxSize = Float.parseFloat(megaBytes);
            }
            catch (Exception e)
            {
                // ignore...configuration entry may not be present
            }

            if (maxSize > 0)
            {
                if (maxSize < (size / 1048576.00))
                { // a megabyte
                    throw new ItemExportException(
                            ItemExportException.EXPORT_TOO_LARGE,
                            "The overall size of this export is too large.  Please contact your administrator for more information.");
                }
            }
        }

        for (Item item : items)
        {
            // now create a subdirectory
            File itemDir = new File(wkDir.getAbsolutePath() + File.separator
                    + item.getHandle().replaceAll("/", "_"));

            if (itemDir.exists())
            {
                throw new Exception("Directory " + itemDir.getAbsolutePath()
                        + " already exists!");
            }

            if (itemDir.mkdir())
            {
                Bundle[] bnds = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
                for (Bundle bnd : bnds)
                {
                    Bitstream[] bs = bnd.getBitstreams();
                    for (Bitstream b : bs)
                    {
                        int myPrefix = 1; // only used with name conflict
                        if (AuthorizeManager.authorizeActionBoolean(context, b,
                                Constants.READ))
                        {
                            InputStream is = b.retrieve();

                            boolean isDone = false; // done when bitstream is
                                                    // finally
                            // written
                            String myName = b.getName();
                            String oldName = myName;
                            while (!isDone)
                            {
                                if (myName.contains(File.separator))
                                {
                                    String dirs = myName.substring(0,
                                            myName.lastIndexOf(File.separator));
                                    File fdirs = new File(
                                            itemDir.getAbsoluteFile()
                                                    + File.separator + dirs);
                                    fdirs.mkdirs();
                                }

                                File fout = new File(itemDir, myName);

                                if (fout.createNewFile())
                                {
                                    FileOutputStream fos = new FileOutputStream(
                                            fout);
                                    Utils.bufferedCopy(is, fos);
                                    // close streams
                                    is.close();
                                    fos.close();
                                    isDone = true;
                                }
                                else
                                {
                                    myName = myPrefix + "_" + oldName; // keep
                                    // appending
                                    // numbers to the
                                    // filename until
                                    // unique
                                    myPrefix++;
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                throw new Exception("Error, can't make dir " + itemDir);
            }
        }
        // now zip up the export directory created above
        ItemExport.zip(wkDir.getAbsolutePath(), downloadDir.getAbsolutePath()
                + System.getProperty("file.separator") + fileName + ".zip");

        return fileName;
    }

    private void buildReferenceStream(Context context, List<Item> items,
            String format, OutputStream outputStream,
            StreamDisseminationCrosswalk streamCrosswalkDefault)
            throws IOException, SQLException, AuthorizeException
    {
        if (streamCrosswalkDefault instanceof StreamGenericDisseminationCrosswalk)
        {
            List<DSpaceObject> disseminate = new LinkedList<DSpaceObject>();
            for (Item item : items)
            {
				if (!exportBiblioEnabled || (context.getCurrentUser()==null  && !exportBiblioAll) )
                {
                    // the item is withdrawn we skip it
                    log.info(LogManager.getHeader(context, "references",
							"item_id=" + item.getID() + ",not authorized"));
                    continue;
                }
                disseminate.add(item);
            }
            try
            {
                ((StreamGenericDisseminationCrosswalk) streamCrosswalkDefault)
                        .disseminate(context, disseminate, outputStream);
            }
            catch (CrosswalkException e)
            {
                log.error(LogManager.getHeader(context, "references",
								e.getMessage()), e);
            }

        }
        else
        {
            for (Item item : items)
            {
				if (!exportBiblioEnabled || (context.getCurrentUser()==null  && !exportBiblioAll) )
                {
                    // the item is withdrawn we skip it
                    log.info(LogManager.getHeader(context, "references",
							"item_id=" + item.getID() + ",not authorized"));
                    continue;
                }

				String type = "";
                try {           
                    String formFileName = I18nUtil.getInputFormsFileName(I18nUtil.getDefaultLocale());
                    String col_handle = "";

                    Collection collection = item.getOwningCollection();

                    if (collection == null)
                    {
                        // set an empty handle so to get the default input set
                        col_handle = "";
                    }
                    else
                    {
                        col_handle = collection.getHandle();
                    }

                    // Read the input form file for the specific collection
                    DCInputsReader inputsReader = new DCInputsReader(formFileName);

                    DCInputSet inputSet = inputsReader.getInputs(col_handle);
                    type = inputSet.getFormName();
                } catch (Exception e1) {
					log.error(LogManager.getHeader(context, "references",
							"unable to determine type " + e1.getMessage()), e1);
				}
				log.info(LogManager.getHeader(context, "references", "item_id="
						+ item.getID()));

                StreamDisseminationCrosswalk streamCrosswalk = null;

                if (type != null)
                {
                    streamCrosswalk = (StreamDisseminationCrosswalk) PluginManager
                            .getNamedPlugin(StreamDisseminationCrosswalk.class,
                                    format + "-" + type);
                }

                if (streamCrosswalk == null)
                {
                    log.debug(LogManager
                            .getHeader(
                                    context,
                                    "references",
                                    "format= "
                                            + format
                                            + ", type="
                                            + type
                                            + " template not found using default for the specified format"));
                    streamCrosswalk = streamCrosswalkDefault;
                }

                try
                {
                    streamCrosswalk.disseminate(context, item, outputStream);
                }
                catch (CrosswalkException e)
                {
                    log.error(LogManager.getHeader(context, "references",
							"item_id=" + item.getID()), e);
                }
            }
        }
        outputStream.flush();
        outputStream.close();
    }

    private String extractResearcherId(HttpServletRequest request)
    {
        String path = request.getPathInfo().substring(1); // remove first /
        String[] splitted = path.split("/");
        request.setAttribute("authority", splitted[0]);
        return splitted[0];
    }

    private List<String> getFilters(String type)
    {
        List<String> filters = new ArrayList<String>();
        int idx = 1;
        while (ConfigurationManager
                .getProperty("researcherpage.publicationlist." + type
                        + ".filters." + idx) != null)
        {
            filters.add(ConfigurationManager
                    .getProperty("researcherpage.publicationlist." + type
                            + ".filters." + idx));
            idx++;
        }
        return filters;
    }

    private String getQuery(String type, String authority)
    {
        return MessageFormat.format(ConfigurationManager
                .getProperty("researcherpage.publicationlist." + type
                        + ".query"), authority);
    }
    
    /**
     * From the ItemExport class...
     * 
     * Create a dirname based on the date and eperson
     * 
     * @param eperson
     *            - eperson who requested export and will be able to download it
     * @param date
     *            - the date the export process was created
     * @return String representing the file name in the form of
     *         '<type>_yyy_MMM_dd_count_epersonID'
     * @throws Exception
     */
    public static String assembleFileName(String type, EPerson eperson,
            Date date) throws Exception
    {
        // to format the date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MMM_dd");
        String exportDir = ItemExport.getExportWorkDirectory();
        // used to avoid name collision
        int count = 1;
        boolean exists = true;
        String fileName = null;
        while (exists)
        {
            fileName = type + "_export_" + sdf.format(date) + "_" + count + "_"
                    + eperson.getID();
            exists = new File(exportDir
                    + System.getProperty("file.separator") + fileName)
                    .exists();
            count++;
        }
        return fileName;
    }


    private class LocalThread extends Thread
    {
        EPerson eperson;

        StreamDisseminationCrosswalk streamCrosswalkDefault;

        String format;

        List<Item> items;

        boolean fulltext;

        public LocalThread(EPerson eperson, String format, boolean fulltext,
                List<Item> items,
                StreamDisseminationCrosswalk streamCrosswalkDefault)
        {
            this.eperson = eperson;
            this.streamCrosswalkDefault = streamCrosswalkDefault;
            this.format = format;
            this.items = items;
            this.fulltext = fulltext;
        }

        public void run()
        {
            Context context = null;
            String filename = null;
            try
            {
                context = new Context();
                context.setCurrentUser(eperson);

                File wkDir = null;
                synchronized (LocalThread.class)
                {
                    filename = assembleFileName("references", eperson,
                            new Date());
                    String workDir = ItemExport.getExportWorkDirectory()
                            + System.getProperty("file.separator") + filename;
                    wkDir = new File(workDir);
                    if (!wkDir.exists())
                    {
                        wkDir.mkdirs();
                    }
                }
                String downloadDir = ItemExport
                    .getExportDownloadDirectory(eperson.getID());
                File dnDir = new File(downloadDir);
                if (!dnDir.exists())
                {
                    dnDir.mkdirs();
                }

                // use the download dir to send the references file directly in
                // the email outside the zip
                File referencesFile = new File(downloadDir, "references");
                OutputStream outputStream = new FileOutputStream(
                        referencesFile, false);
                buildReferenceStream(context, items, format, outputStream,
                        streamCrosswalkDefault);

                if (fulltext)
                {
                    filename = buildFulltextZipFile(context, items, format, wkDir, dnDir,
                            filename);
                }

                emailSuccessMessage(context, eperson, fulltext ? filename
                        : null, referencesFile);
            }
            catch (Exception e)
            {
                try
                {
                    emailErrorMessage(eperson, e.getMessage());
                }
                catch (MessagingException e1)
                {
                    // wont throw here
                }
                throw new RuntimeException(e);
            }
            finally
            {
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }
        }

    }
}