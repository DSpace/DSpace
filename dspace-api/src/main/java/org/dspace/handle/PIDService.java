/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

/* Created for LINDAT/CLARIAH-CZ (UFAL) */
/**
 * Service for PID.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

public class PIDService {
    public static final String SERVICE_TYPE_EPIC = "epic";

    public static final String SERVICE_TYPE_EPIC2 = "epic2";

    private static AbstractPIDService pidService = null;

    private static ConfigurationService configurationService = new DSpace().getConfigurationService();

    private PIDService() {

    }

    private static void initialize() throws Exception {
        if (Objects.nonNull(pidService)) {
            return;
        }
        String serviceType = getServiceType();
        String pidServiceClass = null;
        if (serviceType.equals(PIDService.SERVICE_TYPE_EPIC2)) {
            pidServiceClass = "org.dspace.handle.PIDServiceEPICv2";
        } else {
            throw new IllegalArgumentException("Illegal pid.service type");
        }
        try {
            pidService = (AbstractPIDService)Class.forName(pidServiceClass).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    public static String getServiceType() {
        return configurationService.getProperty("lr.pid.service.type", "lr.pid.service.type");
    }

    /**
     *
     * @param PID
     * @return URL assigned to the PID
     * @throws Exception
     */
    public static String resolvePID(String PID) throws Exception {
        initialize();
        return pidService.resolvePID(PID);
    }

    public static String modifyPID(String PID, String URL, Map<String, String> additionalFields) throws Exception {
        initialize();
        Map<String, String> handleFields = new LinkedHashMap<String, String>();
        handleFields.put(AbstractPIDService.HANDLE_FIELDS.URL.toString(), URL);
        if (null != additionalFields) {
            handleFields.putAll(additionalFields);
        }
        return pidService.modifyPID(PID, handleFields);
    }

    public static String createPID(String URL, String prefix) throws Exception {
        initialize();
        Map<String, String> handleFields = new HashMap<String, String>();
        handleFields.put(AbstractPIDService.HANDLE_FIELDS.URL.toString(), URL);
        return pidService.createPID(handleFields, prefix);
    }

    public static String createCustomPID(String URL, String prefix, String suffix) throws Exception {
        initialize();
        Map<String, String> handleFields = new HashMap<String, String>();
        handleFields.put(AbstractPIDService.HANDLE_FIELDS.URL.toString(), URL);
        return pidService.createCustomPID(handleFields, prefix, suffix);
    }

    public static String findHandle(String URL, String prefix) throws Exception {
        initialize();
        Map<String, String> handleFields = new HashMap<String, String>();
        handleFields.put(AbstractPIDService.HANDLE_FIELDS.URL.toString(), URL);
        return pidService.findHandle(handleFields, prefix);
    }

    public static boolean supportsCustomPIDs() throws Exception {
        initialize();
        return pidService.supportsCustomPIDs();
    }

    public static String who_am_i(String encoding) throws Exception {
        initialize();
        return pidService.whoAmI(encoding);
    }

    public static String deletePID(String PID) throws Exception {
        initialize();
        return pidService.deletePID(PID);
    }

    public static String test_pid(String PID) throws Exception {
        who_am_i(null);
        // 1. search for pid
        // 2. modify it
        resolvePID(PID);
        Random randomGenerator = new Random();
        int randomInt = randomGenerator.nextInt(10000);
        String url = String.format("http://only.testing.mff.cuni.cz/%d", randomInt);
        modifyPID(PID, url, null);
        String resolved = resolvePID(PID);
        if ( resolved.equals(url) ) {
            return "testing succesful";
        } else {
            return "testing seemed ok but resolving did not return the expected result";
        }
    }
}
