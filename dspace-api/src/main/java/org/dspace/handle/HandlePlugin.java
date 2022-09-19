/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.dspace.handle.external.ExternalHandleConstants.DEFAULT_CANONICAL_HANDLE_PREFIX;
import static org.dspace.handle.external.ExternalHandleConstants.MAGIC_BEAN;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.cnri.util.StreamTable;
import net.handle.hdllib.Encoder;
import net.handle.hdllib.HandleException;
import net.handle.hdllib.HandleStorage;
import net.handle.hdllib.HandleValue;
import net.handle.hdllib.ScanCallback;
import net.handle.hdllib.Util;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.handle.service.HandleService;
import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.stereotype.Component;

/**
 * Extension to the CNRI Handle Server that translates requests to resolve
 * handles into DSpace API calls. The implementation simply stubs out most of
 * the methods, and delegates the rest to the
 * {@link HandleService}. This only provides some of the
 * functionality (namely, the resolving of handles to URLs) of the CNRI
 * HandleStorage interface.
 *
 * <p>
 * This class is intended to be embedded in the CNRI Handle Server. It conforms
 * to the HandleStorage interface that was delivered with Handle Server version
 * 6.2.0.
 * </p>
 *
 * @author Peter Breton
 * modified for LINDAT/CLARIN
 * @version $Revision$
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
@Component
public class HandlePlugin implements HandleStorage {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(HandlePlugin.class);

    /**
     * Repository name loaded from the configuration
     */
    private static String repositoryName;
    /**
     * Repository email loaded from the configuration
     */
    private static String repositoryEmail;
    /**
     * Canonical handle prefix loaded from the configuration
     */
    private static String canonicalHandlePrefix;

    /**
     * The DSpace service manager kernel
     **/
    private static transient DSpaceKernelImpl kernelImpl;

    /**
     * References to DSpace Services
     **/
    protected static HandleService handleService;
    protected static HandleClarinService handleClarinService;
    protected static ConfigurationService configurationService;
    protected static ItemService itemService;

    ////////////////////////////////////////
    // Non-Resolving methods -- unimplemented
    ////////////////////////////////////////

    /**
     * HandleStorage interface init method.
     * <p>
     * For DSpace, we have to startup the DSpace Kernel when HandlePlugin
     * initializes, as the HandlePlugin relies on HandleService (and other services)
     * which are loaded by the Kernel.
     *
     * @param st StreamTable
     * @throws Exception if DSpace Kernel fails to startup
     */
    @Override
    public void init(StreamTable st) throws Exception {
        if (log.isInfoEnabled()) {
            log.info("Called init (Starting DSpace Kernel)");
        }

        // Initialise the service manager kernel
        try {
            kernelImpl = DSpaceKernelInit.getKernel(null);
            if (!kernelImpl.isRunning()) {
                kernelImpl.start();
            }
        } catch (Exception e) {
            // Failed to start so destroy it and log and throw an exception
            try {
                kernelImpl.destroy();
            } catch (Exception e1) {
                // Nothing to do
            }
            String message = "Failed to startup DSpace Kernel: " + e.getMessage();
            System.err.println(message);
            e.printStackTrace();
            throw new IllegalStateException(message, e);
        }

    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void setHaveNA(byte[] theHandle, boolean haveit)
        throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called setHaveNA (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void createHandle(byte[] theHandle, HandleValue[] values)
        throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called createHandle (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public boolean deleteHandle(byte[] theHandle) throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called deleteHandle (not implemented)");
        }

        return false;
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void updateValue(byte[] theHandle, HandleValue[] values)
        throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called updateValue (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void deleteAllRecords() throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called deleteAllRecords (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void checkpointDatabase() throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called checkpointDatabase (not implemented)");
        }
    }

    /**
     * HandleStorage interface shutdown() method.
     * <P>
     * For DSpace, we need to destroy the kernel created in init().
     */
    @Override
    public void shutdown() {
        if (log.isInfoEnabled()) {
            log.info("Called shutdown (Destroying DSpace Kernel)");
        }

        // Destroy the DSpace kernel if it is still alive
        if (kernelImpl != null) {
            kernelImpl.destroy();
            kernelImpl = null;
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void scanHandles(ScanCallback callback) throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called scanHandles (not implemented)");
        }
    }

    /**
     * HandleStorage interface method - not implemented.
     */
    @Override
    public void scanNAs(ScanCallback callback) throws HandleException {
        // Not implemented
        if (log.isInfoEnabled()) {
            log.info("Called scanNAs (not implemented)");
        }
    }

    ////////////////////////////////////////
    // Resolving methods
    ////////////////////////////////////////

    /**
     * Return the raw values for this handle. This implementation returns a
     * single URL value.
     *
     * @param theHandle byte array representation of handle
     * @param indexList ignored
     * @param typeList  ignored
     * @return A byte array with the raw data for this handle. Currently, this
     * consists of a single URL value.
     * @throws HandleException If an error occurs while calling the Handle API.
     */
    @Override
    public byte[][] getRawHandleValues(byte[] theHandle, int[] indexList,
                                       byte[][] typeList) throws HandleException {
        if (log.isInfoEnabled()) {
            log.info("Called getRawHandleValues");
        }

        // Configuration, HandleClarin, Handle service
        loadServices();

        Context context = null;

        try {
            if (theHandle == null) {
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }

            String handle = Util.decodeString(theHandle);

            context = new Context();

            DSpaceObject dso = null;
            String url = handleClarinService.resolveToURL(context, handle);

            boolean resolveMetadata = configurationService.getBooleanProperty("lr.pid.resolvemetadata", true);
            if (resolveMetadata) {
                dso = handleClarinService.resolveToObject(context, handle);
            }

            if (Objects.isNull(url)) {
                // try with old prefix

                String[] handle_parts = handleClarinService.splitHandle(handle);

                String[] alternativePrefixes = PIDConfiguration.getAlternativePrefixes(handle_parts[0]);

                for ( String alternativePrefix : alternativePrefixes ) {
                    String alternativeHandle = handleClarinService.completeHandle(
                            alternativePrefix, handle_parts[1]);
                    url = handleClarinService.resolveToURL(context, alternativeHandle);
                    if (Objects.nonNull(url)) {
                        break;
                    }
                }

                // still no match
                if (Objects.isNull(url)) {
                    // <UFAL>
                    log.warn(String.format("Unable to resolve [%s]", handle));
                    // </UFAL>
                    return null;
                }
            }

            ResolvedHandle rh = null;
            if (url.startsWith(MAGIC_BEAN)) {
                String[] splits = url.split(MAGIC_BEAN,10);
                if (splits.length < 8) {
                    throw new RuntimeException("Cannot resolve external handle with magicLindat string, " +
                            "because the external handle do not have enough information.");
                }
                url = splits[splits.length - 1];
                // EMPTY, String title, String repository, String submitdate, String reportemail,
                // String dataset_name, String dataset_version, String query, token is splits[8] but don't show that
                rh = new ResolvedHandle(url, splits[1], splits[2], splits[3], splits[4], splits[5], splits[6],
                        splits[7]);
            } else {
                rh = new ResolvedHandle(url, dso);
            }
            log.info(String.format("Handle [%s] resolved to [%s]", handle, url));
            if (handleClarinService.isDead(context, handle)) {
                //dead_since
                String deadSince = handleClarinService.getDeadSince(context, handle);
                rh.setDead(handle, deadSince);
            }

            return rh.toRawValue();
        } catch (HandleException he) {
            throw he;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Exception in getRawHandleValues", e);
            }

            // Stack loss as exception does not support cause
            throw new HandleException(HandleException.INTERNAL_ERROR);
        } finally {
            if (Objects.nonNull(context)) {
                try {
                    context.complete();
                } catch (SQLException sqle) {
                    // ignore
                }
            }
        }
    }

    /**
     * Return true if we have this handle in storage.
     *
     * @param theHandle byte array representation of handle
     * @return True if we have this handle in storage
     * @throws HandleException If an error occurs while calling the Handle API.
     */
    @Override
    public boolean haveNA(byte[] theHandle) throws HandleException {
        if (log.isInfoEnabled()) {
            log.info("Called haveNA");
        }

        /*
         * Naming authority Handles are in the form: 0.NA/1721.1234
         *
         * 0.NA is basically the naming authority for naming authorities. For
         * this simple implementation, we will just check that the prefix
         * configured in dspace.cfg is the one in the request, returning true if
         * this is the case, false otherwise.
         *
         * FIXME: For more complex Handle situations, this will need enhancing.
         */

        // This parameter allows the dspace handle server to be capable of having multiple
        // name authorities assigned to it. So long as the handle table the alternative prefixes
        // defined the dspace will answer for those handles prefixes. This is not ideal and only
        // works if the dspace instances assumes control over all the items in a prefix, but it
        // does allow the admin to merge together two previously separate dspace instances each
        // with their own prefixes and have the one instance handle both prefixes. In this case
        // all new handle would be given a unified prefix but all old handles would still be
        // resolvable.
        if (configurationService.getBooleanProperty("handle.plugin.checknameauthority", true)) {
            // First, construct a string representing the naming authority Handle
            // we'd expect.
            String expected = "0.NA/" + handleService.getPrefix();

            // Which authority does the request pertain to?
            String received = Util.decodeString(theHandle);

            // Return true if they match
            return expected.equals(received);
        } else {
            return true;
        }
    }

    /**
     * Return all handles in local storage which start with the naming authority
     * handle.
     *
     * @param theNAHandle byte array representation of naming authority handle
     * @return All handles in local storage which start with the naming
     * authority handle.
     * @throws HandleException If an error occurs while calling the Handle API.
     */
    @Override
    public Enumeration getHandlesForNA(byte[] theNAHandle)
        throws HandleException {
        String naHandle = Util.decodeString(theNAHandle);

        if (log.isInfoEnabled()) {
            log.info("Called getHandlesForNA for NA " + naHandle);
        }

        Context context = null;

        try {
            context = new Context();

            List<String> handles = handleService.getHandlesForPrefix(context, naHandle);
            List<byte[]> results = new LinkedList<byte[]>();

            for (Iterator<String> iterator = handles.iterator(); iterator.hasNext(); ) {
                String handle = iterator.next();

                // Transforms to byte array
                results.add(Util.encodeString(handle));
            }

            return Collections.enumeration(results);
        } catch (SQLException sqle) {
            if (log.isDebugEnabled()) {
                log.debug("Exception in getHandlesForNA", sqle);
            }

            // Stack loss as exception does not support cause
            throw new HandleException(HandleException.INTERNAL_ERROR);
        } finally {
            if (context != null) {
                try {
                    context.complete();
                } catch (SQLException sqle) {
                    // ignore
                }
            }
        }
    }

    /**
     * Initialize Handle, Configuration and Item service
     */
    private static void loadServices() {
        // services are loaded
        if (Objects.nonNull(handleService) && Objects.nonNull(configurationService) &&
            Objects.nonNull(itemService) && Objects.nonNull(handleClarinService)) {
            return;
        }

        // Get a reference to the HandleService & ConfigurationService
        handleService = HandleServiceFactory.getInstance().getHandleService();
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        handleClarinService = ContentServiceFactory.getInstance().getHandleClarinService();
    }

    /**
     * Load the repository email from the configuration. The mail is in the property `help.mail`.
     * @return configured repository mail as String or return null if it is not configured
     */
    public static String getRepositoryEmail() {
        if (Objects.nonNull(repositoryEmail)) {
            return repositoryEmail;
        }

        // Handle and Configuration Service
        loadServices();

        // Cannot load services
        if (Objects.isNull(configurationService)) {
            return null;
        }

        String email = configurationService.getProperty(
                "help.mail");

        // the email is not configured
        if (Objects.isNull(email)) {
            repositoryEmail = null;
            return repositoryEmail;
        }

        repositoryEmail = email.trim();
        return repositoryEmail;
    }

    /**
     * Load the repository name from the configuration. The name is in the property `dspace.name`.
     * @return configured repository name as String or return null if it is not configured
     */
    public static String getRepositoryName() {
        if (Objects.nonNull(repositoryName)) {
            return repositoryName;
        }

        // Handle and Configuration Service
        loadServices();

        // Cannot load services
        if (Objects.isNull(configurationService)) {
            return null;
        }

        String name = configurationService.getProperty(
                "dspace.name");
        if (Objects.isNull(name)) {
            repositoryName = null;
            return repositoryName;
        }

        repositoryName = name.trim();
        return repositoryName;
    }

    /**
     * Load the canonical handle prefix from the configuration. The prefix is in the property `handle.canonical.prefix`.
     * @return canonical handle prefix as String or return DEFAULT_CANONICAL_HANDLE_PREFIX = `http://hdl.handle.net/`
     */
    public static String getCanonicalHandlePrefix() {
        if (Objects.nonNull(canonicalHandlePrefix)) {
            return canonicalHandlePrefix;
        }
        // Handle and Configuration Service
        loadServices();

        // Cannot load services
        if (Objects.isNull(configurationService)) {
            canonicalHandlePrefix = DEFAULT_CANONICAL_HANDLE_PREFIX;
        } else {
            canonicalHandlePrefix = configurationService.getProperty(
                    "handle.canonical.prefix", DEFAULT_CANONICAL_HANDLE_PREFIX);
        }

        return canonicalHandlePrefix;
    }

    public static Map<String, String> extractMetadata(DSpaceObject dso) {
        Map<String, String> map = new LinkedHashMap<>();
        if (Objects.isNull(dso)) {
            return map;
        }

        if (!(dso instanceof Item)) {
            return map;
        }
        // load ItemService
        loadServices();

        // load the DSpaceObject metadata
        List<MetadataValue> mds = itemService.getMetadataByMetadataString((Item) dso, "dc.title");
        if (CollectionUtils.isNotEmpty(mds)) {
            map.put(AbstractPIDService.HANDLE_FIELDS.TITLE.toString(), mds.get(0).getValue());
        }
        map.put(AbstractPIDService.HANDLE_FIELDS.REPOSITORY.toString(), getRepositoryName());
        mds = itemService.getMetadataByMetadataString((Item) dso, "dc.date.accessioned");
        if (CollectionUtils.isNotEmpty(mds)) {
            map.put(AbstractPIDService.HANDLE_FIELDS.SUBMITDATE.toString(), mds.get(0).getValue());
        }
        map.put(AbstractPIDService.HANDLE_FIELDS.REPORTEMAIL.toString(), getRepositoryEmail());
        return map;
    }
}

class ResolvedHandle {
    List<HandleValue> values;
    private int idx = -1;
    private int timestamp = 100;

    public ResolvedHandle(String url, String title, String repository, String submitdate, String reportemail,
                          String datasetName, String datasetVersion, String query) {
        init(url, title, repository, submitdate, reportemail, datasetName, datasetVersion, query);
    }


    public ResolvedHandle(String url, DSpaceObject dso) {
        String title = null;
        String repository = null;
        String submitdate = null;
        String reportemail = null;
        if (null != dso) {
            Map<String, String> map = HandlePlugin.extractMetadata(dso);
            String key
                    = AbstractPIDService.HANDLE_FIELDS.TITLE.toString();
            title = getOrDefault(map, key, "");

            key = AbstractPIDService.HANDLE_FIELDS.REPOSITORY.toString();
            repository = getOrDefault(map, key, "");

            key = AbstractPIDService.HANDLE_FIELDS.SUBMITDATE.toString();
            submitdate = getOrDefault(map, key, "");

            key = AbstractPIDService.HANDLE_FIELDS.REPORTEMAIL.toString();
            reportemail = getOrDefault(map, key, "");
        }
        init(url, title, repository, submitdate, reportemail);
    }

    private <K,V> V getOrDefault (Map<K,V> map, K key, V defaultValue) {
        if (map.containsKey(key)) {
            return map.get(key);
        } else {
            return defaultValue;
        }
    }

    private void init(String url, String title, String repository, String submitdate, String reportemail) {
        init(url, title, repository, submitdate, reportemail, null, null, null);
    }

    private void init(String url, String title, String repository, String submitdate, String reportemail,
                      String datasetName, String datasetVersion, String query) {
        idx = 11800;
        values = new LinkedList<>();
        //set timestamp, use submitdate for now
        if (submitdate != null) {
            try {
                long stamp = new DCDate(submitdate).toDate().getTime() / 1000;
                if (stamp < Integer.MAX_VALUE && stamp > Integer.MIN_VALUE) {
                    timestamp = (int) stamp;
                }
            } catch (Exception e) {
                //in case the submitdate is malformed, ie. some junk was in the url we split
                timestamp = 100;
            }
        }
        setResolvedUrl(url);
        String key;
        if (null != title) {
            key = AbstractPIDService.HANDLE_FIELDS.TITLE.toString();
            setValue(key, title);
        }

        if (null != repository) {
            key = AbstractPIDService.HANDLE_FIELDS.REPOSITORY.toString();
            setValue(key, repository);
        }

        if (null != submitdate) {
            key = AbstractPIDService.HANDLE_FIELDS.SUBMITDATE.toString();
            setValue(key, submitdate);
        }
        if (null != reportemail) {
            key = AbstractPIDService.HANDLE_FIELDS.REPORTEMAIL.toString();
            setValue(key, reportemail);
        }
        if (isNotBlank(datasetName)) {
            key = AbstractPIDService.HANDLE_FIELDS.DATASETNAME.toString();
            setValue(key, datasetName);
        }
        if (isNotBlank(datasetVersion)) {
            key = AbstractPIDService.HANDLE_FIELDS.DATASETVERSION.toString();
            setValue(key, datasetVersion);
        }
        if (isNotBlank(query)) {
            key = AbstractPIDService.HANDLE_FIELDS.QUERY.toString();
            setValue(key, query);
        }
    }

    private void setResolvedUrl(String url) {
        HandleValue value = new HandleValue();
        value.setIndex(100);
        value.setType(Util.encodeString("URL"));
        value.setData(Util.encodeString(url));
        value.setTTLType((byte) 0);
        value.setTTL(100);
        value.setTimestamp(timestamp);
        value.setReferences(null);
        value.setAdminCanRead(true);
        value.setAdminCanWrite(false);
        value.setAnyoneCanRead(true);
        value.setAnyoneCanWrite(false);
        values.add(value);
    }

    private void setValue(String key, String val) {
        HandleValue hv = new HandleValue();
        hv.setIndex(idx++);
        hv.setType(Util.encodeString(key));
        hv.setData(Util.encodeString(val));
        hv.setTTLType((byte) 0);
        hv.setTTL(100);
        hv.setTimestamp(timestamp);
        hv.setReferences(null);
        hv.setAdminCanRead(true);
        hv.setAdminCanWrite(false);
        hv.setAnyoneCanRead(true);
        hv.setAnyoneCanWrite(false);
        values.add(hv);
    }

    public byte[][] toRawValue() throws HandleException {
        byte[][] rawValues = new byte[values.size()][];

        for (int i = 0; i < values.size(); i++) {
            HandleValue hvalue = values.get(i);

            rawValues[i] = new byte[Encoder.calcStorageSize(hvalue)];
            Encoder.encodeHandleValue(rawValues[i], 0, hvalue);
        }
        return rawValues;
    }

    public void setDead(String handle, String deadSince) {
        //find URL field
        for (HandleValue hv : values) {
            if (hv.hasType(Util.encodeString("URL"))) {
                //duplicate old url as last working URL
                HandleValue deadURL = hv.duplicate();
                deadURL.setType(Util.encodeString("ORIG_URL"));
                deadURL.setIndex(idx++);
                values.add(deadURL);
                //change url to our display page
                hv.setData(Util.encodeString("http://hdl.handle.net/11346/SHORTREF-PR6O#hdl=" + handle));
                break;
            }
        }
        if (deadSince != null) {
            setValue("DEAD_SINCE", deadSince);
        }
    }
}