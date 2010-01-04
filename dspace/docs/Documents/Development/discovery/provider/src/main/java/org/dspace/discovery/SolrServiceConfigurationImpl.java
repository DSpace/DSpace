package org.dspace.discovery;

import org.apache.commons.collections.ExtendedProperties;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.io.IOException;

/**
 * User: mdiggory
 * Date: Dec 6, 2009
 * Time: 12:49:13 PM
 */
public class SolrServiceConfigurationImpl {


    ExtendedProperties props = ExtendedProperties
                           .convertProperties(ConfigurationManager.getProperties());

    public SolrServiceConfigurationImpl() throws IOException {

        File config = new File(props.getProperty("dspace.dir")  + "/config/dspace-solr-search.cfg");
        if (config.exists()) {
            props.combine(new ExtendedProperties(config.getAbsolutePath()));
        } else {
            ExtendedProperties defaults = new ExtendedProperties();
            defaults.load(SolrServiceImpl.class.getResourceAsStream("dspace-solr-search.cfg"));
            props.combine(defaults);
        }

    }
}
