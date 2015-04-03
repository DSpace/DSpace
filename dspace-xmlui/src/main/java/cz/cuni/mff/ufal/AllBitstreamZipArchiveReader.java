/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.Deflater;

import javax.mail.internet.MimeUtility;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.BitstreamReader;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.tracker.TrackerFactory;
import cz.cuni.mff.ufal.tracker.TrackingSite;

public class AllBitstreamZipArchiveReader extends AbstractReader implements Recyclable {


    private static Logger log = cz.cuni.mff.ufal.Logger.getLogger(BitstreamReader.class);


    /**
     * How big of a buffer should we use when reading from the bitstream before
     * writting to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /** Limit of ZIP format **/
    protected static final long ZIP_LIMIT_SIZE = 4294967295l;

    /** Redirection possibilities */
    protected static final int NO_REDIRECTION = 1;
    protected static final int REDIRECT_TO_RESTRICTION_INFO = 2;
    protected static final int REDIRECT_TO_LICENSE_AGREEMENT = 3;

    /** Redirect target */
    protected int redirect;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    /** The bitstream's IDs */
    protected ArrayList<Integer> bitstreamIDs;

    /** The user ID */
    protected int userID;

    /** Item containing the Bitstreams */
    private Item item = null;

    /** The zip file */
    protected InputStream zipInputStream;

    /** The zip file size */
    protected long zipFileSize;


    protected boolean needsZip64 = false;


    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);

        long downloadAllLimitMaxFileSize = ConfigurationManager.getLongProperty("lr","lr.download.all.limit.max.file.size",-1);

        redirect = NO_REDIRECTION;

        try
        {
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);

            Context context = ContextUtil.obtainContext(objectModel);

            int itemID = par.getParameterAsInteger("itemID", -1);
            if(context.getCurrentUser() == null)
            {
                this.userID = 0;
            }
            else {
                this.userID = context.getCurrentUser().getID();
            }
            String handle = par.getParameter("handle", null);

            DSpaceObject dso = null;

            if (itemID > -1)
            {
                // Referenced by internal itemID
                item = Item.find(context, itemID);
            }
            else if (handle != null)
            {
                // Reference by an item's handle.
                dso = HandleManager.resolveToObject(context,handle);

                if (dso instanceof Item)
                {
                    item = (Item)dso;
                }
            }

            bitstreamIDs = new ArrayList<Integer>();
            long totalSize = 0l;

            Bundle[] originals = item.getBundles("ORIGINAL");
            for (Bundle original : originals)
            {
                Bitstream[] bss = original.getBitstreams();
                for (Bitstream bitstream : bss)
                {
                    //<UFAL>
                    // Is there a User logged in and does the user have access to read it?
                    try {
                        AuthorizeManager.authorizeAction(context, bitstream, Constants.READ);
                    }
                    catch (MissingLicenseAgreementException e)
                    {
                    	redirect = REDIRECT_TO_LICENSE_AGREEMENT;
                        BitstreamReader.redirectToLicenseAgreement(context, request, dso, item, bitstream, objectModel, false);
                        return;
                    }
                    catch (AuthorizeException e) {
                    	redirect = REDIRECT_TO_RESTRICTION_INFO;
                        BitstreamReader.redirectToRestrictionInfo(context, request, dso, item, bitstream, objectModel, false);
                        return;
                    }

                    if (item != null && item.isWithdrawn() && !AuthorizeManager.isAdmin(context))
                    {
                        log.info(LogManager.getHeader(context, "view_bitstream", "handle=" + item.getHandle() + ",withdrawn=true"));
                        BitstreamReader.redirectToRestrictionInfo(context, request, dso, item, bitstream, objectModel, false);
                        return;
                    }

                    totalSize += bitstream.getSize();
                    if(totalSize > ZIP_LIMIT_SIZE) {
                        needsZip64 = true;
                    }

                    if(downloadAllLimitMaxFileSize > -1 && totalSize >= downloadAllLimitMaxFileSize) {
                        throw new ProcessingException(String.format("Download of zip archive of data larger than %dB is forbidden.", downloadAllLimitMaxFileSize));
                    }

                    bitstreamIDs.add(bitstream.getID());
                    //</UFAL>
                }
            }

            if(ConfigurationManager.getBooleanProperty("lr", "lr.tracker.enabled")) {
                // Track the download for analytics platform
                TrackerFactory.createInstance(TrackingSite.BITSTREAM).trackPage(request, "Bitstream Download / Zip Archive");
            }

        }
        catch (SQLException sqle)
        {
            throw new ProcessingException("Unable to read bitstreams.", sqle);
        }
    }

    /** Adds suffix to filename before extension */
    protected String addSuffixToFilename(String filename, String suffix) {
        String suffixedFilename = FilenameUtils.getBaseName(filename) + "_" + suffix;
        String extension = FilenameUtils.getExtension(filename);
        if(!extension.isEmpty()) {
            suffixedFilename += "." + extension;
        }
        return suffixedFilename;
    }

    /** Creates unique filename based on map of counters of already used filenames */
    protected String createUniqueFilename(String filename, ConcurrentMap<String, AtomicInteger> usedFilenames) {
        String uniqueFilename = filename;
        usedFilenames.putIfAbsent(filename, new AtomicInteger(0));
        int occurence = usedFilenames.get(filename).incrementAndGet();
        if(occurence > 1) {
            uniqueFilename = addSuffixToFilename(filename, String.valueOf(occurence));
        }
        return uniqueFilename;
    }

    @Override
    public void generate() throws IOException, SAXException,
            ProcessingException {

		if(redirect != NO_REDIRECTION) {
			return;
		}

         String name = item.getName() + ".zip";

         try
            {

                // first write everything to a temp file
                String tempDir = ConfigurationManager.getProperty("upload.temp.dir");
                String fn = tempDir + File.separator + "SWORD." + item.getID() + "." + UUID.randomUUID().toString() + ".zip";
                OutputStream outStream = new FileOutputStream(new File(fn));
                ZipArchiveOutputStream zip = new ZipArchiveOutputStream(outStream);
                zip.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.ALWAYS);
                zip.setLevel(Deflater.NO_COMPRESSION);
                ConcurrentMap<String, AtomicInteger> usedFilenames = new ConcurrentHashMap<String, AtomicInteger>();

                Bundle[] originals = item.getBundles("ORIGINAL");
                if(needsZip64) {
                    zip.setUseZip64(Zip64Mode.Always);
                }
                for (Bundle original : originals)
                {
                    Bitstream[] bss = original.getBitstreams();
                    for (Bitstream bitstream : bss)
                    {
                        String filename = bitstream.getName();
                        String uniqueName = createUniqueFilename(filename, usedFilenames);
                        ZipArchiveEntry ze = new ZipArchiveEntry(uniqueName);
                        zip.putArchiveEntry(ze);
                        InputStream is = bitstream.retrieve();
                        Utils.copy(is, zip);
                        zip.closeArchiveEntry();
                        is.close();
                    }
                }
                zip.close();

                File file = new File(fn);
                zipFileSize = file.length();

                zipInputStream = new TempFileInputStream(file);
            } catch (AuthorizeException e) {
                log.error(e.getMessage(), e);
                throw new ProcessingException("You do not have permissions to access one or more files.");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new ProcessingException("Could not create ZIP, please download the files one by one. " +
                        "The error has been stored and will be solved by our technical team.");
            }

            // Log download statistics
            for (int bitstreamID : bitstreamIDs)
            {
                DSpaceApi.updateFileDownloadStatistics(userID, bitstreamID);
            }

            response.setDateHeader("Last-Modified", item.getLastModified().getTime());

            // Try and make the download file name formated for each browser.
            try {
                    String agent = request.getHeader("USER-AGENT");
                    if (agent != null && agent.contains("MSIE"))
                    {
                        name = URLEncoder.encode(name, "UTF8");
                    }
                    else if (agent != null && agent.contains("Mozilla"))
                    {
                        name = MimeUtility.encodeText(name, "UTF8", "B");
                    }
            }
            catch (UnsupportedEncodingException see)
            {
                name = "item_" + item.getID() + ".zip";
            }
            response.setHeader("Content-Disposition", "attachment;filename=" + name);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length = -1;

            try
            {
                response.setHeader("Content-Length", String.valueOf(this.zipFileSize));

                while ((length = this.zipInputStream.read(buffer)) > -1)
                {
                    out.write(buffer, 0, length);
                }
                out.flush();
            }
            finally
            {
                try
                {
                    if ( this.zipInputStream != null ) {
                        this.zipInputStream.close();
                    }
                    if ( out != null ) {
                        out.close();
                    }
                }
                catch (IOException ioe)
                {
                    // Closing the stream threw an IOException but do we want this to propagate up to Cocoon?
                    // No point since the user has already got the file contents.
                    log.warn("Caught IO exception when closing a stream: " + ioe.getMessage());
                }
            }


    }

    /**
     * Recycle
     */
    public void recycle() {
        this.response = null;
        this.request = null;
        this.zipFileSize = 0;
        this.zipInputStream = null;
        this.bitstreamIDs = null;
        this.userID = 0;
    }


}
