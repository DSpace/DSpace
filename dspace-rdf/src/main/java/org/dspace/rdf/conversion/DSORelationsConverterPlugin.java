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
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
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
import org.dspace.rdf.RDFUtil;
import org.dspace.services.ConfigurationService;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class DSORelationsConverterPlugin
implements ConverterPlugin
{
    protected ConfigurationService configurationService;

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
        Model m = ModelFactory.createDefaultModel();
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
            m.add(m.createResource(myId),
                    m.createProperty("http://purl.org/dc/terms/hasPart"),
                    m.createResource(id));
        }
        
        if (m.isEmpty())
        {
            m.close();
            return null;
        }
        m.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        return m;
    }
    
    public Model convertCommunity(Context context, Community community)
            throws SQLException
    {
        Model m = ModelFactory.createDefaultModel();
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
                m.add(m.createResource(myId),
                        m.createProperty("http://purl.org/dc/terms/isPartOf"),
                        m.createResource(id));
            }
        }
        
        // add all subcommunities and collections.
        LinkedList<DSpaceObject> subDSOs = new LinkedList<>();
        subDSOs.addAll(Arrays.asList(community.getSubcommunities()));
        subDSOs.addAll(Arrays.asList(community.getAllCollections()));
        for (DSpaceObject dso : subDSOs)
        {
            if (!RDFUtil.isPublicBoolean(context, dso))
            {
                continue;
            }
                        
            String id = RDFUtil.generateIdentifier(context, dso);
            if (id != null)
            {
                m.add(m.createResource(myId),
                        m.createProperty("http://purl.org/dc/terms/hasPart"),
                        m.createResource(id));
            }
        }
        
        if (m.isEmpty())
        {
            m.close();
            return null;
        }
        m.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        return m;
    }
    
    public Model convertCollection(Context context, Collection collection)
            throws SQLException
    {
        Model m = ModelFactory.createDefaultModel();
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
                m.add(m.createResource(myId),
                        m.createProperty("http://purl.org/dc/terms/isPartOf"),
                        m.createResource(id));
            }
        }
        
        // add all items
        ItemIterator items = collection.getAllItems();
        while (items.hasNext())
        {
            String id = RDFUtil.generateIdentifier(context, items.next());
            if (id != null)
            {
                m.add(m.createResource(myId),
                        m.createProperty("http://purl.org/dc/terms/hasPart"),
                        m.createResource(id));
            }
        }
        
        if (m.isEmpty())
        {
            m.close();
            return null;
        }
        m.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        return m;
    }
    
    public Model convertItem(Context context, Item item)
            throws SQLException
    {
        Model m = ModelFactory.createDefaultModel();
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
                m.add(m.createResource(myId),
                        m.createProperty("http://purl.org/dc/terms/isPartOf"),
                        m.createResource(id));
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
                            // TODO: user other property here? perhaps we should 
                            // use something like the following?
                            // http://prismstandard.org/namespaces/basic/3.0/url
                            m.add(m.createResource(myId),
                                m.createProperty("http://purl.org/dc/terms/hasPart"),
                                m.createResource(url));
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
        m.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        return m;
    }
    
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
