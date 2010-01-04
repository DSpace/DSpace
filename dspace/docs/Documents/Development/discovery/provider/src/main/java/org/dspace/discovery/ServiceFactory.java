package org.dspace.discovery;

import org.apache.commons.collections.ExtendedProperties;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: mdiggory
 * Date: Oct 19, 2009
 * Time: 4:53:35 AM
 */
public class ServiceFactory {


    private static SolrServiceImpl instance = null;


    /**
     * Retrieve the singleton instance of this service. (temporary for 1.5.2 use Spring / Service in future)
     *
     * @return
     */
    public static SearchService getSearchService() {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    public static IndexingService getIndexingService()
    {
        if (instance == null) {
            initialize();
        }
        return instance;
    }

    private static void initialize() {
        instance = new SolrServiceImpl();

        instance.setConfig(getConfiguration());

        
                   ExtendedProperties props = ExtendedProperties
                           .convertProperties(ConfigurationManager.getProperties());

                   try {
                       File config = new File(props.getProperty("dspace.dir")
                               + "/config/dspace-solr-search.cfg");
                       if (config.exists()) {
                           props.combine(new ExtendedProperties(config.getAbsolutePath()));
                       } else {
                           ExtendedProperties defaults = new ExtendedProperties();
                           defaults
                                   .load(SolrServiceImpl.class
                                           .getResourceAsStream("dspace-solr-search.cfg"));
                           props.combine(defaults);
                       }


                   instance.setSolrService(props.getString("solr.search.server"));

                   instance.setFacetFields(props.getStringArray("solr.search.facets"));

                   } catch (Exception e) {
                        throw new RuntimeException(e.getMessage(),e);
                   }

    }




    public static List<IndexConfig> getConfiguration() {

        ArrayList<IndexConfig> config = new ArrayList<IndexConfig>();

        //config.add(new IndexConfig("title", "dc", "title", Item.ANY, "text"));
        //config.add(new IndexConfig("author", "dc", "contributor", Item.ANY, "text"));
        //config.add(new IndexConfig("author", "dc", "creator", Item.ANY, "text"));
        //config.add(new IndexConfig("keyword", "dc", "subject", Item.ANY, "text"));

        //config.add(new IndexConfig("advisor", "dc", "creator", Item.ANY, "text"));
        //config.add(new IndexConfig("author", "dc", "description", "statementofresponsibility", "text"));
        //config.add(new IndexConfig("abstract", "dc", "description", "abstract", "text"));
        //config.add(new IndexConfig("abstract", "dc", "description", "tableofcontents", "text"));
        //config.add(new IndexConfig("series", "dc", "relation", "ispartofseries", "text"));
        //config.add(new IndexConfig("mimetype", "dc", "format", "mimetype", "text"));
        //config.add(new IndexConfig("sponsor", "dc", "description", "sponsorship", "text"));
        //config.add(new IndexConfig("identifier", "dc", "identifier", Item.ANY, "text"));


        // read in search.index.1, search.index.2....
        for (int i = 1; ConfigurationManager.getProperty("search.index." + i) != null; i++)
        {
                String index = ConfigurationManager.getProperty("search.index." + i);

                String indexName, schema, element, qualifier = null, type = "text";

                String[] configLine = index.split(":");

                indexName = configLine[0];

                // Get the schema, element and qualifier for the index
                // TODO: Should check valid schema, element, qualifier?
                String[] parts = configLine[1].split("\\.");

                switch (parts.length)
                {
                case 3:
                    qualifier = parts[2];
                case 2:
                    schema  = parts[0];
                    element = parts[1];
                    break;
                default:
                    //log.warn("Malformed configuration line: search.index." + i);
                    // FIXME: Can't proceed here, no suitable exception to throw
                    throw new RuntimeException(
                            "Malformed configuration line: search.index." + i);
                }

                if (configLine.length > 2)
                {
                    type = configLine[2];
                }

            config.add(
                    new IndexConfig(indexName, schema, element, qualifier, type));

                  //  ConfigurationManager.getProperty("search.index." + i));
        }




        return config;

    }


}
