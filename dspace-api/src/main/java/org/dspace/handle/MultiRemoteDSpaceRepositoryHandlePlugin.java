/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleStorage;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ScanCallback;
import net.handle.hdllib.Util;
import net.handle.util.StreamTable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.Iterator;

/**
 * Extension to the CNRI Handle Server that translates requests to resolve
 * handles into remote calls to the mini-DSpace Handle resolver JSON API. This
 * only provides some of the functionality (namely, the resolving of handles to
 * URLs) of the CNRI HandleStorage interface.
 * 
 * <p>
 * This class is intended to be embedded in the CNRI Handle Server. It conforms
 * to the HandleStorage interface that was delivered with Handle Server version
 * 6.2.0.
 * </p>
 * 
 * @author Andrea Bollini
 */
public class MultiRemoteDSpaceRepositoryHandlePlugin implements HandleStorage
{
    /**
     * Name of configuration file. This can be overwritten by setting a path
     * to the configuration file to use in the system property 
     * <code>dspace.handle.plugin.configuration</code>.
     */
    private static String CONFIG_FILE_NAME = "handle-dspace-plugin.cfg";
    /**
     * Every Property starting with this key will be used as DSpace endpoint
     * while resolving handles, f.e. http://localhost:8080/xmlui/handleresolver.
     */
    private static String PROPERTY_KEY = "dspace.handle.endpoint";
    
    /** log4j category */
    private static Logger log = Logger
            .getLogger(MultiRemoteDSpaceRepositoryHandlePlugin.class);
    
    // maps prefixes to URLs from DSpace instances
    private Map<String, String> prefixes;

    /**
     * Constructor
     */
    public MultiRemoteDSpaceRepositoryHandlePlugin()
    {
    }

    // //////////////////////////////////////
    // Non-Resolving methods -- unimplemented
    // //////////////////////////////////////

    /**
     * HandleStorage interface method - not implemented.
     */
    public void init(StreamTable st) throws Exception
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called init");
        }
        
        // initalize our prefix map
        this.prefixes = new HashMap<String, String>();

        // try to find our configuration
        Properties properties = loadProperties(CONFIG_FILE_NAME);
        
        // find urls of all configured dspace instances
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();)
        {
            String propertyName = (String) e.nextElement();
            if (propertyName.startsWith(this.PROPERTY_KEY))
            {
                // load the prefixes of this instance
                loadPrefixes(properties.getProperty(propertyName));
            }
        }
        
        // did we found any prefixes?
        if (this.prefixes.isEmpty())
        {
            throw new HandleException(HandleException.INTERNAL_ERROR, 
                    "Unable to find configuration or to reach any DSpace instance.");
        }
        
        if (log.isInfoEnabled())
        {
            for (Iterator<String> it = this.prefixes.keySet().iterator(); it.hasNext() ;)
            {
                String prefix = it.next();
                log.info("Loaded Prefix " + prefix + " from " + this.prefixes.get(prefix));
            }
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void setHaveNA(byte[] theHandle, boolean haveit)
            throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called setHaveNA (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void createHandle(byte[] theHandle, HandleValue[] values)
            throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called createHandle (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public boolean deleteHandle(byte[] theHandle) throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called deleteHandle (not implemented)");
        }

        return false;
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void updateValue(byte[] theHandle, HandleValue[] values)
            throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called updateValue (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void deleteAllRecords() throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called deleteAllRecords (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void checkpointDatabase() throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called checkpointDatabase (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void shutdown()
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called shutdown (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void scanHandles(ScanCallback callback) throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called scanHandles (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    public void scanNAs(ScanCallback callback) throws HandleException
    {
        // Not implemented
        if (log.isInfoEnabled())
        {
            log.info("Called scanNAs (not implemented)");
        }
    }

    // //////////////////////////////////////
    // Resolving methods
    // //////////////////////////////////////

    /**
     * Return the raw values for this handle. This implementation returns a
     * single URL value.
     * 
     * @param theHandle
     *            byte array representation of handle
     * @param indexList
     *            ignored
     * @param typeList
     *            ignored
     * @return A byte array with the raw data for this handle. Currently, this
     *         consists of a single URL value.
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    public byte[][] getRawHandleValues(byte[] theHandle, int[] indexList,
            byte[][] typeList) throws HandleException
    {
        if (log.isInfoEnabled())
        {
            log.info("Called getRawHandleValues");
        }

        if (theHandle == null)
        {
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }

        String handle = Util.decodeString(theHandle);
        String url = getRemoteDSpaceURL(handle);
        HandleValue value = new HandleValue();

        value.setIndex(100);
        value.setType(Util.encodeString("URL"));
        value.setData(Util.encodeString(url));
        value.setTTLType((byte) 0);
        value.setTTL(100);
        value.setTimestamp(100);
        value.setReferences(null);
        value.setAdminCanRead(true);
        value.setAdminCanWrite(false);
        value.setAnyoneCanRead(true);
        value.setAnyoneCanWrite(false);

        List<HandleValue> values = new LinkedList<HandleValue>();

        values.add(value);

        byte[][] rawValues = new byte[values.size()][];

        for (int i = 0; i < values.size(); i++)
        {
            HandleValue hvalue = values.get(i);

            rawValues[i] = new byte[Encoder.calcStorageSize(hvalue)];
            Encoder.encodeHandleValue(rawValues[i], 0, hvalue);
        }

        return rawValues;

    }

    private String getRemoteDSpaceURL(String handle) throws HandleException
    {
        if (log.isInfoEnabled())
        {
            log.info("Called getRemoteDSpaceURL("+handle+").");
        }
        
        InputStreamReader jsonStreamReader = null;
        String url = null;
        try
        {
            String prefix = handle.split("/")[0];
            String endpoint = this.prefixes.get(prefix);
            if (endpoint == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Cannot find endpoint for prefix " + prefix + ", throw HANDLE_DOES_NOT_EXIST.");
                }
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
            }

            String jsonurl = endpoint + "/resolve/" + handle;
            jsonStreamReader = new InputStreamReader(
                    new URL(jsonurl).openStream(), "UTF-8");
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonStreamReader);

            if (jsonElement == null || jsonElement.isJsonNull()
                    || jsonElement.getAsJsonArray().size() == 0
                    || jsonElement.getAsJsonArray().get(0).isJsonNull())
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Throw HandleException: HANDLE_DOES_NOT_EXIST.");
                }
                throw new HandleException(HandleException.HANDLE_DOES_NOT_EXIST);
            }

            url = jsonElement.getAsJsonArray().get(0).getAsString();
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception in getRawHandleValues", e);
            }

            // Stack loss as exception does not support cause
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (jsonStreamReader != null)
            {
                try
                {
                    jsonStreamReader.close();
                }
                catch (IOException e)
                {
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug(("getRemoteDspaceURL returns " + url));
        }
        return url;
    }

    /**
     * Return true if we have this handle in storage.
     * 
     * @param theHandle
     *            byte array representation of handle
     * @return True if we have this handle in storage
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    public boolean haveNA(byte[] theHandle) throws HandleException
    {
        if (log.isInfoEnabled())
        {
            log.info("Called haveNA");
        }
        /*
         * Naming authority Handles are in the form: 0.NA/1721.1234
         * 
         * 0.NA is basically the naming authority for naming authorities. For
         * this simple implementation, we will just check if the requestes
         * prefix is one that DSpace returns when we call
         * handleresolver/listprefixes.
         */
        // Which authority does the request pertain to? Remove the heading "0.NA/".
        String received = Util.decodeString(theHandle).substring("0.NA/".length());
        
        return this.prefixes.containsKey(received);
    }

    /**
     * Return all handles in local storage which start with the naming authority
     * handle.
     * 
     * @param theNAHandle
     *            byte array representation of naming authority handle
     * @return All handles in local storage which start with the naming
     *         authority handle.
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    public Enumeration getHandlesForNA(byte[] theNAHandle)
            throws HandleException
    {
        String naHandle = Util.decodeString(theNAHandle);

        if (log.isInfoEnabled())
        {
            log.info("Called getHandlesForNA for NA " + naHandle);
        }

        List<String> handles = getRemoteDSpaceHandles(naHandle);
        List<byte[]> results = new LinkedList<byte[]>();

        for (String handle : handles)
        {
            // Transforms to byte array
            results.add(Util.encodeString(handle));
        }

        return Collections.enumeration(results);

    }

    private List<String> getRemoteDSpaceHandles(String naHandle)
            throws HandleException
    {
        List<String> handles = new ArrayList<String>();

        String endpoint = this.prefixes.get(naHandle);
        if (null == endpoint)
        {
            // We don't know anything about this prefix, return an empty list.
            return handles;
        }

        InputStreamReader jsonStreamReader = null;
        try
        {
            String jsonurl = endpoint + "/listhandles/" + naHandle;
            jsonStreamReader = new InputStreamReader(
                    new URL(jsonurl).openStream(), "UTF-8");
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonStreamReader);

            if (jsonElement != null && jsonElement.getAsJsonArray().size() != 0)
            {
                for (int i = 0; i < jsonElement.getAsJsonArray().size(); i++)
                {
                    handles.add(jsonElement.getAsJsonArray().get(i)
                            .getAsString());
                }
            }
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Exception in getHandlesForNA", e);
            }

            // Stack loss as exception does not support cause
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }
        finally
        {
            if (jsonStreamReader != null)
            {
                try
                {
                    jsonStreamReader.close();
                }
                catch (IOException e)
                {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return handles;
    }

    private Properties loadProperties(String filename) throws IOException
    {
        InputStream is = findConfigFile(filename);
        Properties props = new Properties();
        
        InputStreamReader ir = null;
        // load the configuration
        try {
            ir = new InputStreamReader(is, "UTF-8");
            props.load(ir);
        } catch (UnsupportedEncodingException ex) {
            if (log.isInfoEnabled())
            {
                log.info("Caught an UnsupportedEncodingException while loading configuration: " + ex.getMessage());
            }
            throw new RuntimeException(ex);
        }
        catch (IOException ex)
        {
            if (log.isInfoEnabled())
            {
                log.info("Caught an IOException while loading configuration: " + ex.getMessage());
            }
            throw ex;
        }
        finally
        {
            if (ir != null)
            {
                try
                {
                    ir.close();
                }
                catch (IOException ex)
                {
                    // nothing to do.
                }
            }
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException ex)
                {
                    // nothing to do.
                }
            }
        }
        
        return props;
    }
    
    private InputStream findConfigFile(String filename)
    {
        InputStream is = null;
        
        // try to load config file as defined in system property:
        try
        {
            String configProperty = System.getProperty("dspace.handle.plugin.configuration");
            if (null != configProperty)
            {
                is = new FileInputStream(configProperty);
            }
        }
        catch (SecurityException se)
        {
            // A security manager may stop us from accessing the system properties.
            // This isn't really a fatal error though, so catch and ignore
            log.warn("Unable to access system properties, ignoring.", se);
        }
        catch (FileNotFoundException fne)
        {
            log.warn("Unable to find config file as defined by system property: "
                    + fne.getMessage());
        }
        
        if (null == is)
        {
            // try some default locations
            URL url = MultiRemoteDSpaceRepositoryHandlePlugin.class.getResource(
                "/" + filename);
            if (null == url)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Cannot find configuration by using getResource().");
                }
            } else {
                try
                {
                    is = new FileInputStream(url.getPath());
                    log.warn("Falling back to default locations, "
                            + "found configuration at: " + url.getPath());
                }
                catch (FileNotFoundException e)
                {
                    if (log.isInfoEnabled())
                    {
                        // didn't found fallback config, nothing to do about it.
                        log.info("Unable to open fallback configuration: " + e.getMessage());
                    }
                }
            }
        }
        
        if (null == is)
        {
            // try to find configuration in the current working directory
            // where the user started the handle server.
            try
            {
                is = new FileInputStream("./" + filename);
                if (log.isInfoEnabled())
                {
                    log.info("Loaded configuration from your current working "
                            + "directory where you started the handle server.");
                }
            }
            catch (FileNotFoundException ex)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Can't load config file: " + ex.getMessage());
                }
            }
        }
        
        if (is == null)
        {
            throw new IllegalStateException("Cannot find configuration.");
        }
        
        return is;
    }

    private void loadPrefixes(String endpoint)
    {
        URL url = null;
        try {
            url = new URL(endpoint + "/listprefixes");
        } catch (MalformedURLException ex) {
            log.error(endpoint + "is not a correct URL, will ignore this "
                    + "DSpace instance.", ex);
        }

        String[] prefixes;
        try {
            InputStreamReader jsonStreamReader = new InputStreamReader(url.openStream(), "UTF-8");
            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(jsonStreamReader);

            if (jsonElement != null && jsonElement.getAsJsonArray().size() != 0)
            {
                for (int i = 0; i < jsonElement.getAsJsonArray().size(); i++)
                {
                    String prefix = jsonElement.getAsJsonArray().get(i).getAsString();
                    this.prefixes.put(prefix, endpoint);
                    
                    if (log.isInfoEnabled())
                    {
                        log.info("Mapping " + prefix + " to instance at " + endpoint);
                    }
                }
            } else {
                log.warn("DSpace instance running at " + url + " returns empty prefix list.");
            }
        }
        catch (Exception ex)
        {
            log.warn("Error while loading prefixes from " + endpoint + ", ignoring.", ex);
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        MultiRemoteDSpaceRepositoryHandlePlugin multi = new MultiRemoteDSpaceRepositoryHandlePlugin();
        try
        {
            System.out.println(StringUtils.join(
                    multi.getRemoteDSpaceHandles("123456789"), ","));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(StringUtils.join(
                    multi.getRemoteDSpaceHandles("123456780"), ","));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456789/1"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456789/1111111"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456780/1"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
        try
        {
            System.out.println(multi.getRemoteDSpaceURL("123456780/1111111"));
        }
        catch (HandleException e)
        {
            e.printStackTrace();
        }
    }
}
