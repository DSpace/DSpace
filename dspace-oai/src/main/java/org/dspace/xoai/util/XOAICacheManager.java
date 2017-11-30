/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.xoai.data.DSpaceDatabaseItem;

import com.lyncode.xoai.dataprovider.OAIDataProvider;
import com.lyncode.xoai.dataprovider.OAIRequestParameters;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.exceptions.MetadataBindException;
import com.lyncode.xoai.dataprovider.exceptions.OAIException;
import com.lyncode.xoai.dataprovider.util.Base64Utils;
import com.lyncode.xoai.dataprovider.util.MarshallingUtils;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class XOAICacheManager
{
    private static Logger log = LogManager.getLogger(XOAICacheManager.class);

    private static final String ITEMDIR = File.separator + "items";

    private static final String REQUESTDIR = File.separator + "requests";

    private static final String DATEFILE = File.separator + "date.file";

    private static String baseDir = null;

    private static String getBaseDir()
    {
        if (baseDir == null)
        {
            String dir = ConfigurationManager.getProperty("oai", "cache.dir");
            baseDir = dir;
        }
        return baseDir;
    }

    private static File getCachedResponseFile(String id)
    {
        File dir = new File(getBaseDir() + REQUESTDIR);
        if (!dir.exists())
            dir.mkdirs();

        String name = File.separator + Base64Utils.encode(id);
        return new File(getBaseDir() + REQUESTDIR + name);
    }

    private static File getMetadataCache(Item item)
    {
        File dir = new File(getBaseDir() + ITEMDIR);
        if (!dir.exists())
            dir.mkdirs();

        String name = File.separator + item.getHandle().replace('/', '_');
        return new File(getBaseDir() + ITEMDIR + name);
    }

    public static void compileItem(DSpaceDatabaseItem item)
    {
        File metadataCache = getMetadataCache(item.getItem());
        if (metadataCache.exists())
            metadataCache.delete();
        getMetadata(item);
    }

    public static String getCompiledMetadata(DSpaceDatabaseItem item)
            throws MetadataBindException
    {
        log.debug("Trying to find compiled item");
        File metadataCache = getMetadataCache(item.getItem());
        Metadata metadata;
        String compiled;
        if (!metadataCache.exists())
        {
            log.debug("This is not a compiled item");
            // generate cache
            metadata = ItemUtils.retrieveMetadata(item.getItem());
            FileOutputStream output;
            try
            {
                output = new FileOutputStream(metadataCache);
                MarshallingUtils.writeMetadata(output, metadata);
            }
            catch (FileNotFoundException e)
            {
                log.warn(
                        "Could not open file for writing: "
                                + metadataCache.getPath(), e);
            }
            catch (MetadataBindException e)
            {
                log.warn("Unable to export in-memory metadata into file: "
                        + metadataCache.getPath(), e);
            }
        }
        log.debug("This is a compiled item!");
        // Read compiled file
        FileInputStream input;
        try
        {
            input = new FileInputStream(metadataCache);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Utils.bufferedCopy(input, output);
            input.close();
            output.close();
            compiled = output.toString();
        }
        catch (Exception e)
        {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            log.warn(e.getMessage(), e);
            MarshallingUtils.writeMetadata(output,
                    ItemUtils.retrieveMetadata(item.getItem()));
            compiled = output.toString();
        }
        return compiled;
    }

    public static Metadata getMetadata(DSpaceDatabaseItem item)
    {
        log.debug("Trying to find compiled item");
        File metadataCache = getMetadataCache(item.getItem());
        Metadata metadata;
        if (!metadataCache.exists())
        {
            log.debug("This is not a compiled item");
            // generate cache
            metadata = ItemUtils.retrieveMetadata(item.getItem());
            FileOutputStream output;
            try
            {
                output = new FileOutputStream(metadataCache);
                MarshallingUtils.writeMetadata(output, metadata);
            }
            catch (FileNotFoundException e)
            {
                log.warn(
                        "Could not open file for writing: "
                                + metadataCache.getPath(), e);
            }
            catch (MetadataBindException e)
            {
                log.warn("Unable to export in-memory metadata into file: "
                        + metadataCache.getPath(), e);
            }
        }
        else
        {
            log.debug("This is a compiled item!");
            // Read compiled file
            FileInputStream input;
            try
            {
                input = new FileInputStream(metadataCache);
                metadata = MarshallingUtils.readMetadata(input);
                input.close();
            }
            catch (Exception e)
            {
                log.warn(e.getMessage(), e);
                metadata = ItemUtils.retrieveMetadata(item.getItem());
            }
        }
        return metadata;
    }

    private static String getStaticHead()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + ((XOAIManager.getManager().hasStyleSheet()) ? ("<?xml-stylesheet type=\"text/xsl\" href=\""
                        + XOAIManager.getManager().getStyleSheet() + "\"?>")
                        : "")
                + "<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">";
    }

    public static void handle(String identification,
            OAIDataProvider dataProvider, OAIRequestParameters parameters,
            OutputStream out) throws IOException
    {
        
        boolean caching = ConfigurationManager.getBooleanProperty("oai", "cache.enabled", true);
        File cachedResponse = getCachedResponseFile(identification);
        if (!caching || !cachedResponse.exists())
        {
            log.debug("[XOAI] Result not cached");
            try
            {
                // XOAI response facade
                // This in-memory buffer will be used to store the XOAI response
                ByteArrayOutputStream intermediate = new ByteArrayOutputStream();
                dataProvider.handle(parameters, intermediate);
                String xoaiResponse = intermediate.toString();

                // Cutting the header (to allow one to change the response time)
                String end = "</responseDate>";
                int pos = xoaiResponse.indexOf(end);
                if (pos > 0)
                    xoaiResponse = xoaiResponse.substring(pos + (end.length()));

                // Storing in a file
                FileOutputStream output = new FileOutputStream(cachedResponse);
                output.write(xoaiResponse.getBytes());
                output.flush();
                output.close();
            }
            catch (OAIException e)
            {
                // try to remove the file (If an error occurs, it must not show
                // empty pages)
                if (cachedResponse.exists())
                    cachedResponse.delete();
                log.error(e.getMessage(), e);
            }
        }
        else
            log.debug("[OAI 2.0] Cached Result");

        // The cached file is written, now one start by adding the header
        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'");
        out.write((getStaticHead() + "<responseDate>"
                + format.format(new Date()) + "</responseDate>").getBytes());

        // Now just simply copy the compiled file
        FileInputStream in = new FileInputStream(cachedResponse);
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
        {
            out.write(buf, 0, len);
        }
        in.close();
    }

    private static final SimpleDateFormat format = new SimpleDateFormat();

    public static Date getLastCompilationDate()
    {
        FileInputStream fstream;

        try
        {
            fstream = new FileInputStream(getBaseDir() + DATEFILE);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            try
            {
                Date d = format.parse(br.readLine());
                return d;
            }
            catch (Exception e)
            {
                log.debug(e.getMessage(), e);
                try
                {
                    fstream.close();
                    return null;
                }
                catch (Exception e1)
                {
                    log.debug(e1.getMessage(), e1);
                    return null;
                }
            }
        }
        catch (FileNotFoundException e)
        {
            log.debug(e.getMessage(), e);
            return null;
        }
    }

    public static void main(String... args)
    {
        FileOutputStream fstream;
        try
        {
            fstream = new FileOutputStream("test");
            // Get the object of DataInputStream
            fstream.write(format.format(new Date()).getBytes());
            fstream.flush();
            fstream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println(format.format(new Date()));
    }

    public static void setLastCompilationDate(Date date)
    {
        FileOutputStream fstream;
        try
        {
            fstream = new FileOutputStream(getBaseDir() + DATEFILE);
            // Get the object of DataInputStream
            fstream.write(format.format(date).getBytes());
            fstream.flush();
            fstream.close();
        }
        catch (Exception e)
        {
            log.debug("Error writing the date");
        }
    }

    public static void deleteCachedResponses()
    {
        File directory = new File(getBaseDir() + REQUESTDIR);
        if (directory.exists())
        {
            // Get all files in directory
            File[] files = directory.listFiles();
            for (File file : files)
            {
                // Delete each file
                file.delete();
            }
        }
    }

    public static void deleteCompiledItems()
    {
        (new File(getBaseDir() + DATEFILE)).delete();
        File directory = new File(getBaseDir() + ITEMDIR);
        if (directory.exists())
        {
            // Get all files in directory
            File[] files = directory.listFiles();
            for (File file : files)
            {
                // Delete each file
                file.delete();
            }
        }
    }
}
