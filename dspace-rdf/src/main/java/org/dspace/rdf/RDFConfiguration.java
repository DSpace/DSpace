/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.Constants;
import org.dspace.rdf.conversion.RDFConverter;
import org.dspace.rdf.storage.RDFStorage;
import org.dspace.rdf.storage.URIGenerator;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class RDFConfiguration {
    private static final Logger log = Logger.getLogger(RDFConfiguration.class);

    /**
     * Property key to load the public address of the SPARQL endpoint.
     */
    public static final String SPARQL_ENDPOINT_KEY = "rdf.public.sparql.endpoint";

    /**
     * Property key to load the class to use as URIGenerator.
     */
    public static final String URIGENERATOR_KEY = "rdf.URIGenerator";
    /** 
     * Property key to load the class to use as RDFConverter.
     */
    public static final String RDFCONVERTER_KEY = "rdf.converter";
    /**
     * Property key to load the list of plugins for the RDFConverter.
     */
    public static final String CONVERTER_PLUGINS_KEY = "rdf.converter.plugins";
    /**
     * Key of the Property to load the types of DSpaceObjects that should get
     * converted.
     */
    public static final String CONVERTER_DSOTYPES_KEY = "rdf.converter.DSOtypes";
    /**
     * Property key to load the class to use as RDFStorage.
     */
    public static final String RDFSTORAGE_KEY = "rdf.storage";
    /**
     * Property key to load the address of the SPARQL 1.1 GRAPH STORE HTTP 
     * PROTOCOL endpoint.
     */
    public static final String STORAGE_GRAPHSTORE_ENDPOINT_KEY = 
            "rdf.storage.graphstore.endpoint";
    /**
     * Property key to load whether HTTP authentication for the 
     * graph store endpoint is required.
     */
    public static final String STORAGE_GRAPHSTORE_AUTHENTICATION_KEY =
            "rdf.storage.graphstore.authentication";
    /**
     * Property key to load the username if authentication for the graph store 
     * endpoint is required.
     */
    public static final String STORAGE_GRAPHSTORE_LOGIN_KEY = "rdf.storage.graphstore.login";
    /**
     * Property key to load the password if authentication for the graph store 
     * endpoint is required.
     */
    public static final String STORAGE_GRAPHSTORE_PASSWORD_KEY = "rdf.storage.graphstore.password";

    /**
     * Property key to load the address of the SPARQL endpoint to use within 
     * DSpace. If the property is empty or does not exist, the public SPARQL 
     * endpoint will be used.
     */
    public static final String STORAGE_SPARQL_ENDPOINT_KEY = "rdf.storage.sparql.endpoint";
    /**
     * Property key to load whether HTTP authentication for the internal SPARQL
     * endpoint is required.
     */
    public  static final String STORAGE_SPARQL_AUTHENTICATION_KEY = "rdf.storage.sparql.authentication";
    /**
     * Property key to load the username if authentication for the internal 
     * SPARQL endpoint is required.
     */
    public static final String STORAGE_SPARQL_LOGIN_KEY = "rdf.storage.sparql.login";
    /**
     * Property key to load the password if authentication for the internal
     * SPARQL endpoint is required.
     */
    public static final String STORAGE_SPARQL_PASSWORD_KEY = "rdf.storage.sparql.password";
    
    /**
     * Property key to load the URL of the dspace-rdf module. This is necessary
     * to create links from the jspui or xmlui to RDF representation of 
     * DSpaceObjects.
     */
    public static final String CONTEXT_PATH_KEY = "rdf.contextPath";
    
    public static final String CONTENT_NEGOTIATION_KEY = "rdf.contentNegotiation.enable";
    
    private static URIGenerator generator;
    private static RDFStorage storage;
    private static RDFConverter converter;

    public static String[] getConverterPlugins()
    {
        String pluginNames = (new DSpace()).getConfigurationService().getProperty(
                CONVERTER_PLUGINS_KEY);
        if (StringUtils.isEmpty(pluginNames))
        {
            return null;
        }        
        return pluginNames.split(",\\s*");
    }
    
    public static String[] getDSOTypesToConvert()
    {
        String dsoTypes = (new DSpace()).getConfigurationService().getProperty(
                CONVERTER_DSOTYPES_KEY);
        if (StringUtils.isEmpty(dsoTypes))
        {
            log.warn("Property rdf." + CONVERTER_DSOTYPES_KEY + " was not found "
                    + "or is empty. Will convert all type of DSpace Objects.");
            return Constants.typeText;
        }
        return dsoTypes.split(",\\s*");
    }
    
    public static boolean isConvertType(int type)
    {
        for (String typeName : getDSOTypesToConvert())
        {
            if (Constants.getTypeID(typeName) == type) return true;
        }
        return false;
    }
    
    public static boolean isConvertType(String type)
    {
        for (String typeName : getDSOTypesToConvert())
        {
            if (typeName.equalsIgnoreCase(type)) return true;
        }
        return false;
    }
    
    public static boolean isContentNegotiationEnabled()
    {
        ConfigurationService configurationService = 
                new DSpace().getConfigurationService();
        return configurationService.getPropertyAsType(CONTENT_NEGOTIATION_KEY, 
                false);
    }
    
    public static String getPublicSparqlEndpointAddress()
    {
        ConfigurationService configurationService = 
                    new DSpace().getConfigurationService();
        return configurationService.getProperty(SPARQL_ENDPOINT_KEY);
    }
    
    public static String getInternalSparqlEndpointAddress()
    {
        ConfigurationService configurationService = 
                new DSpace().getConfigurationService();
        String internalSparqlEndpoint =
                configurationService.getProperty(STORAGE_SPARQL_ENDPOINT_KEY);
        String externalSparqlEndpoint = 
                configurationService.getProperty(SPARQL_ENDPOINT_KEY);
        return StringUtils.isEmpty(internalSparqlEndpoint) ? 
                externalSparqlEndpoint : internalSparqlEndpoint;

    }
    
    public static String getDSpaceRDFModuleURI()
    {
        ConfigurationService configurationService = 
                    new DSpace().getConfigurationService();
        return configurationService.getProperty(CONTEXT_PATH_KEY);
    }
        
    protected static RDFConverter getRDFConverter()
    {
        if (converter == null)
        {
            ConfigurationService configurationService =
                    new DSpace().getConfigurationService();
            converter = (RDFConverter) initializeClass(configurationService, 
                    RDFCONVERTER_KEY, "RDFConverter");
        }
        return converter;
    }
    
    /*
     * Initialize the URIGenerator configured in dsapce config (see 
     * {@link #URIGENERATOR_KEY URIGENERATOR_KEY}).
     * The URIGenerator should be configurable, using the DSpace configuration
     * and not using spring to avoid xml configuration. This method loads and
     * initialize the configured URIGenerator. It is static so that the
     * RDFizer must not be initialized to generate the identifier for a DSO.
     */
    protected static URIGenerator getURIGenerator()
    {
        if (generator == null)
        {
            ConfigurationService configurationService = 
                    new DSpace().getConfigurationService();
            generator = (URIGenerator) initializeClass(configurationService, 
                    URIGENERATOR_KEY, "URIGenerator");
        }
        return generator;
    }
    
    /*
     * Initialize the RDFStorage configured in dsapce config (see 
     * {@link #RDFSTORAGE_KEY RDFSTORAGE_KEY}).
     * The storage class should be configurable, using the DSpace configuration
     * and not using spring to avoid xml configuration. This method loads and
     * initialize the configured RDFStorage class. It is static so that the
     * RDFizer must not be initialized to load RDF data.
     */
    protected static RDFStorage getRDFStorage()
    {
        if (storage == null)
        {
            ConfigurationService configurationService = 
                    new DSpace().getConfigurationService();
            storage = (RDFStorage) initializeClass(configurationService,
                            RDFSTORAGE_KEY, "RDFStorage");
        }
        return storage;
    }
    
    /*
     * This method must by static, so we can use it from 
     * RDFizer.generateIdentifier and RDFizer.generateGraphName. Cause this
     * method is static we cannot use the configurationService initilised in
     * the class constructor.
     * This method loads from DSpace configuration which class to use and 
     * initalizes it.
     */
    private static Object initializeClass(ConfigurationService configurationService,
            String propertyName,
            String objectName)
    {
        String className = configurationService.getProperty(propertyName);
        if (StringUtils.isEmpty(className))
        {
            log.error("Cannot load " + objectName + "! Property " + propertyName
                    + " not found or empty!");
            throw new RuntimeException("Cannot load " + objectName 
                    + ", property not found or not configured!");
        }

        Object instantiatedObject = null;
        try
        {
            Class objectClass = Class.forName(className);
            instantiatedObject = objectClass.newInstance();
        } catch (ClassNotFoundException ex) {
            log.error("Cannot find class '" + className + "' for " + objectName 
                    + ". " + "Please check your configuration.", ex);
            throw new RuntimeException("Cannot find class for " + objectName
                    + " (" + className + ").", ex);
        } catch (InstantiationException ex) {
            log.error("Cannot instantiate " + objectName + " (class " 
                    + className + ").", ex);
            throw new RuntimeException("Cannot instantiate " + objectName 
                    + " (class " + className + ").", ex);
        } catch (IllegalAccessException ex) {
            log.error("IllegalAccessException thrown while instantiating the "
                    + objectName + " (class " + className + ").", ex);
            throw new RuntimeException("IllegalAccessException thrown while "
                    + "instantiating the " + objectName + " (class " 
                    + className + ").", ex);
        }

        return instantiatedObject;
    }

}
