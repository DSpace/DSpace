/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rdf.conversion;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.FileUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.Site;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.rdf.RDFConfiguration;
import org.dspace.rdf.RDFUtil;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class SimpleDSORelationsConverterPlugin
implements ConverterPlugin
{
    public static final String SIMPLE_RELATIONS_PREFIXES_KEY = "rdf.simplerelations.prefixes";
    public static final String SIMPLE_RELATIONS_SITE2COMMUNITY_KEY = "rdf.simplerelations.site2community";
    public static final String SIMPLE_RELATIONS_COMMUNITY2SITE_KEY = "rdf.simplerelations.community2site";
    public static final String SIMPLE_RELATIONS_COMMUNITY2SUBCOMMUNITY_KEY= "rdf.simplerelations.community2subcommunity";
    public static final String SIMPLE_RELATIONS_SUBCOMMUNITY2COMMUNITY_KEY= "rdf.simplerelations.subcommunity2community";
    public static final String SIMPLE_RELATIONS_COMMUNITY2COLLECTION_KEY = "rdf.simplerelations.community2collection";
    public static final String SIMPLE_RELATIONS_COLLECTION2COMMUNITY_KEY = "rdf.simplerelations.collection2community";
    public static final String SIMPLE_RELATIONS_COLLECTION2ITEM_KEY = "rdf.simplerelations.collection2item";
    public static final String SIMPLE_RELATIONS_ITEM2COLLECTION_KEY = "rdf.simplerelations.item2collection";
    public static final String SIMPLE_RELATIONS_ITEM2BITSTREAM_KEY = "rdf.simplerelations.item2bitstream";

    
    private static final Logger log = Logger.getLogger(SimpleDSORelationsConverterPlugin.class);
    protected ConfigurationService configurationService;
    
    protected String[] site2community;
    protected String[] community2site;
    protected String[] community2subcommunity;
    protected String[] subcommunity2community;
    protected String[] community2collection;
    protected String[] collection2community;
    protected String[] collection2item;
    protected String[] item2collection;
    protected String[] item2bitstream;

    public SimpleDSORelationsConverterPlugin()
    {
        site2community = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_SITE2COMMUNITY_KEY);
        community2site = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_COMMUNITY2SITE_KEY);
        community2subcommunity = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_COMMUNITY2SUBCOMMUNITY_KEY);
        subcommunity2community = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_SUBCOMMUNITY2COMMUNITY_KEY);
        community2collection = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_COMMUNITY2COLLECTION_KEY);
        collection2community = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_COLLECTION2COMMUNITY_KEY);
        collection2item = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_COLLECTION2ITEM_KEY);
        item2collection = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_ITEM2COLLECTION_KEY);
        item2bitstream = RDFConfiguration.loadConfigurationArray(SIMPLE_RELATIONS_ITEM2BITSTREAM_KEY);
        
        if (site2community == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between the repository "
                    + "the repository (SITE) and the top communities.");
        }
        if (community2site == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "the top communities and the repository (SITE).");
        }
        if (community2subcommunity == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "communities and subcommunities.");
        }
        if (subcommunity2community == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "subcommunities and communities.");
        }
        if (community2collection == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "communities and collections.");
        }
        if (collection2community == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "collections and communities.");
        }
        if (collection2item == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "collections and items");
        }
        if (item2collection == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "items and collections");
        }
        if (item2bitstream == null)
        {
            log.warn("SimpleDSORelationsConverterPlugin was unable to load "
                    + "configuration to convert relation between "
                    + "items and bitstreams.");
        }
    }
    
    /**
     * Loads the prefixes that should be used by the 
     * SimpleDSORelationsConverterPlugin. Please remember to close the model 
     * returned by this method.
     * @return A model containing the content of the file used to configure the
     * RDF-Prefixes that should be used by this plugin.
     */
    protected Model getPrefixes()
    {
        Model m = ModelFactory.createDefaultModel();
        String prefixesPath = configurationService
                .getProperty(SIMPLE_RELATIONS_PREFIXES_KEY);
        if (!StringUtils.isEmpty(prefixesPath))
        {
            InputStream is = FileManager.get().open(prefixesPath);
            if (is == null)
            {
                log.warn("Cannot find file '" + prefixesPath + "', ignoring...");
            } else {
                m.read(is, null, FileUtils.guessLang(prefixesPath));
                try {
                    is.close();
                }
                catch (IOException ex)
                {
                    // nothing to do here.
                }
            }
        } else {
            log.warn("Configuration does not contain path to prefixes file for "
                    + "SimpleDSORelationsConverterPlugin. Will proceed without "
                    + "prefixes.");
        }
        return m;
    }
    
    @Override
    public void setConfigurationService(ConfigurationService configurationService)
    {
        this.configurationService = configurationService;
    }

    @Override
    public Model convert(Context context, DSpaceObject dso)
            throws SQLException
    {
        
        switch(dso.getType())
        {
            case (Constants.SITE) :
            {
                return convertSite(context, (Site) dso);
            }
            case (Constants.COMMUNITY) :
            {
                return convertCommunity(context, (Community) dso);
            }
            case (Constants.COLLECTION) :
            {
                return convertCollection(context, (Collection) dso);
            }
            case (Constants.ITEM) :
            {
                return convertItem(context, (Item) dso);
            }
        }
        return null;
    }
    
    public Model convertSite(Context context, Site site)
            throws SQLException
    {
        if (site2community == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from the repository (SITE) to the top level "
                    + "communities is disabled. Won't link from the repostitory "
                    + "(SITE) to the top level communities.");
            return null;
        }
        
        Model m = ModelFactory.createDefaultModel();
        Model prefixes = this.getPrefixes();
        m.setNsPrefixes(prefixes);
        prefixes.close();
        
        String myId = RDFUtil.generateIdentifier(context, site);
        if (myId == null)
        {
            return null;
        }

        Community[] topLevelCommies = Community.findAllTop(context);
        for (Community community : topLevelCommies)
        {
            if (!RDFUtil.isPublicBoolean(context, community))
            {
                continue;
            }
            String id = RDFUtil.generateIdentifier(context, community);
            if (id == null)
            {
                continue;
            }
            for (String link : site2community)
            {
                m.add(m.createResource(myId),
                        m.createProperty(link),
                        m.createResource(id));
            }
        }
        
        if (m.isEmpty())
        {
            log.info("There were no public sub communities we could link to.");
            m.close();
            return null;
        }
        return m;
    }
    
    public Model convertCommunity(Context context, Community community)
            throws SQLException
    {
        if (community2site == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from the top level communities to the repository "
                    + "(SITE) is disabled. Won't link from the top level "
                    + "communities to the repository (SITE).");
        }
        if (community2subcommunity == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from communities to subcommunities was disabled. "
                    + "Won't link from communities to subcommunities.");
        }
        if (subcommunity2community == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from subcommunities to communities was disabled. "
                    + "Won't link from subcommunities to communities.");
        }
        if (community2collection == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from communities to collections was disabled. "
                    + "Won't link from collections to subcommunities.");
        }
        if (community2site == null && community2subcommunity == null
                && subcommunity2community == null && community2collection == null)
        {
            return null;
        }
        
        Model m = ModelFactory.createDefaultModel();
        Model prefixes = this.getPrefixes();
        m.setNsPrefixes(prefixes);
        prefixes.close();
        
        String myId = RDFUtil.generateIdentifier(context, community);
        if (myId == null)
        {
            return null;
        }
        
        // add all parents
        DSpaceObject[] parents = community.getAllParents();
        // check whether this is a top level community
        if (parents.length == 0)
        {
            parents = new DSpaceObject[] {Site.find(context, Site.SITE_ID)};
        }
        for (DSpaceObject parent : parents)
        {
            if (!RDFUtil.isPublicBoolean(context, parent))
            {
                continue;
            }
            
            String id = RDFUtil.generateIdentifier(context, parent);
            if (id != null)
            {
                if (parent instanceof Site)
                {
                    for (String link : community2site)
                    {
                        m.add(m.createResource(myId),
                                m.createProperty(link),
                                m.createResource(id));
                    }
                }
                else if (parent instanceof Community)
                {
                    for (String link : subcommunity2community)
                    {
                        m.add(m.createResource(myId),
                                m.createProperty(link),
                                m.createResource(id));
                    }
                }
            }
        }
        
        // add all subcommunities
        for (Community sub : community.getSubcommunities())
        {
            if (!RDFUtil.isPublicBoolean(context, sub))
            {
                continue;
            }
            String id = RDFUtil.generateIdentifier(context, sub);
            if (id == null)
            {
                continue;
            }
            for (String link : community2subcommunity)
            {
                m.add(m.createResource(myId),
                        m.createProperty(link),
                        m.createResource(id));
            }
        }
        // add all collections.
        for (Collection col : community.getAllCollections())
        {
            if (!RDFUtil.isPublicBoolean(context, col))
            {
                continue;
            }
            String id = RDFUtil.generateIdentifier(context, col);
            if (id == null)
            {
                continue;
            }
            for (String link : community2collection)
            {
                m.add(m.createResource(myId),
                        m.createProperty(link),
                        m.createResource(id));
            }
        }
        
        if (m.isEmpty())
        {
            m.close();
            return null;
        }
        return m;
    }
    
    public Model convertCollection(Context context, Collection collection)
            throws SQLException
    {
        if (collection2community == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from collections to communities was disabled. "
                    + "Won't link from collections to communities.");
        }
        if (collection2item == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from collections to items was disabled. "
                    + "Won't link from collections to items.");
        }
        if (collection2community == null && collection2item == null)
        {
            return null;
        }
        
        Model m = ModelFactory.createDefaultModel();
        Model prefixes = this.getPrefixes();
        m.setNsPrefixes(prefixes);
        prefixes.close();
        
        String myId = RDFUtil.generateIdentifier(context, collection);
        if (myId == null)
        {
            return null;
        }
        
        // add all parents
        DSpaceObject[] parents = collection.getCommunities();
        for (DSpaceObject parent : parents)
        {
            if (!RDFUtil.isPublicBoolean(context, parent))
            {
                continue;
            }
            
            String id = RDFUtil.generateIdentifier(context, parent);
            if (id != null)
            {
                for (String link : collection2community)
                {
                    m.add(m.createResource(myId),
                            m.createProperty(link),
                            m.createResource(id));
                }
            }
        }
        
        // add all items
        ItemIterator items = collection.getAllItems();
        while (items.hasNext())
        {
            String id = RDFUtil.generateIdentifier(context, items.next());
            if (id != null)
            {
                for (String link : collection2item)
                {
                    m.add(m.createResource(myId),
                            m.createProperty(link),
                            m.createResource(id));
                }
            }
        }
        
        if (m.isEmpty())
        {
            m.close();
            return null;
        }
        return m;
    }
    
    public Model convertItem(Context context, Item item)
            throws SQLException
    {
        if (item2collection == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from items to collections was disabled. "
                    + "Won't link from items to collections.");
        }
        if (item2bitstream == null)
        {
            log.info("Either there was a problem loading the configuration or "
                    + "linking from items to bitstreams was disabled. "
                    + "Won't link from items to bitstreams.");
        }
        if (item2collection == null && item2bitstream == null)
        {
            return null;
        }
        
        Model m = ModelFactory.createDefaultModel();
        Model prefixes = this.getPrefixes();
        m.setNsPrefixes(prefixes);
        prefixes.close();
        
        String myId = RDFUtil.generateIdentifier(context, item);
        if (myId == null)
        {
            return null;
        }
        
        // add all parents
        Collection[] collections = item.getCollections();
        for (DSpaceObject parent : collections)
        {
            if (!RDFUtil.isPublicBoolean(context, parent))
            {
                continue;
            }
            
            String id = RDFUtil.generateIdentifier(context, parent);
            if (id != null)
            {
                for (String link : item2collection)
                {
                    m.add(m.createResource(myId),
                            m.createProperty(link),
                            m.createResource(id));
                }
            }
        }
        
        // add all items
        for(Bundle bundle : item.getBundles())
        {
            // currently link only the original files
            // TODO: Discuss if LICENSEs, THUMBNAILs and/or extracted TEXTs
            // should be linked/exported as well (and if sutch a feature should 
            // be configurable).
            if (bundle.getName().equals("ORIGINAL"))
            {
                for (Bitstream bs : bundle.getBitstreams())
                {
                    if (RDFUtil.isPublicBoolean(context, bs))
                    {
                        String url = bitstreamURI(bs);
                        if (url != null)
                        {
                            for (String link : item2bitstream)
                            {
                                m.add(m.createResource(myId),
                                    m.createProperty(link),
                                    m.createResource(url));
                            }
                        }
                    }
                }
            }
        }
        
        if (m.isEmpty())
        {
            m.close();
            return null;
        }
        return m;
    }
    
    /**
     * This methods generataes a link to the provieded Bitstream.
     * As bitstreams currently don't get Persistent Identifier in DSpace, we have
     * to link them using a link to the repository. This link should work with
     * JSPUI and XMLUI (at least it does in DSpace 4.x).
     * @param bitstream Bitstream for which a URL should be generated.
     * @return The link to the URL or null if the Bistream is is a Community or 
     * Collection logo.
     * @throws SQLException 
     */
    public String bitstreamURI(Bitstream bitstream)
            throws SQLException
    {
        DSpaceObject parent = bitstream.getParentObject();

        if (!(parent instanceof Item))
        {
            // Bitstream is a community or collection logo.
            // we currently ignore those
            return null;
        }
        String dspaceURL = configurationService.getProperty("dspace.url");
        String link = "";
        try
        {
            // this currently (DSpace 4.1) works with xmlui and jspui.
            link = dspaceURL + "/bitstream/" + parent.getHandle() + "/" 
                + bitstream.getSequenceID() + "/" 
                + Util.encodeBitstreamName(bitstream.getName(), Constants.DEFAULT_ENCODING);
        }
        catch (UnsupportedEncodingException ex)
        {
            throw new RuntimeException("DSpace's default encoding is not supported.", ex);
        }
        return link;
    }

    @Override
    public boolean supports(int type)
    {
        switch (type)
        {
            case (Constants.COLLECTION) :
                return true;
            case (Constants.COMMUNITY) :
                return true;
            case (Constants.ITEM) :
                return true;
            case (Constants.SITE) :
                return true;
            default :
                return false;
        }
    }
}
