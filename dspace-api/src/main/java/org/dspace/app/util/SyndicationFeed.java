/*
 * SyndicationFeed.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2009/10/19 21:51:54 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.module.DCModuleImpl;
import com.sun.syndication.feed.module.DCModule;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.FeedException;

import org.apache.log4j.Logger;

/**
 * Invoke ROME library to assemble a generic model of a syndication
 * for the given list of Items and scope.  Consults configuration for the
 * metadata bindings to feed elements.  Uses ROME's output drivers to
 * return any of the implemented formats, e.g. RSS 1.0, RSS 2.0, ATOM 1.0.
 *
 * The feed generator and OpenSearch call on this class so feed contents are
 * uniform for both.
 *
 * @author Larry Stone
 */
public class SyndicationFeed
{
    private static final Logger log = Logger.getLogger(SyndicationFeed.class);


    /** i18n key values */
    public static final String MSG_UNTITLED = "notitle";
    public static final String MSG_LOGO_TITLE = "logo.title";
    public static final String MSG_FEED_TITLE = "feed.title";
    public static final String MSG_FEED_DESCRIPTION = "general-feed.description";
    public static final String MSG_METADATA = "metadata.";
    public static final String MSG_UITYPE = "ui.type";

    // UI keywords
    public static final String UITYPE_XMLUI = "xmlui";
    public static final String UITYPE_JSPUI = "jspui";

    // default DC fields for entry
    private static String defaultTitleField = "dc.title";
    private static String defaultAuthorField = "dc.contributor.author";
    private static String defaultDateField = "dc.date.issued";
    private static String defaultDescriptionFields = "dc.description.abstract, dc.description, dc.title.alternative, dc.title";

    // metadata field for Item title in entry:
    private static String titleField =
        getDefaultedConfiguration("webui.feed.item.title", defaultTitleField);

    // metadata field for Item publication date in entry:
    private static String dateField =
        getDefaultedConfiguration("webui.feed.item.date", defaultDateField);

    // metadata field for Item description in entry:
    private static String descriptionFields[] =
        getDefaultedConfiguration("webui.feed.item.description", defaultDescriptionFields).split("\\s*,\\s*");

    private static String authorField =
        getDefaultedConfiguration("webui.feed.item.author", defaultAuthorField);

    // metadata field for Item dc:creator field in entry's DCModule (no default)
    private static String dcCreatorField = ConfigurationManager.getProperty("webui.feed.item.dc.creator");

    // metadata field for Item dc:date field in entry's DCModule (no default)
    private static String dcDateField = ConfigurationManager.getProperty("webui.feed.item.dc.date");

    // metadata field for Item dc:author field in entry's DCModule (no default)
    private static String dcDescriptionField = ConfigurationManager.getProperty("webui.feed.item.dc.description");


    // -------- Instance variables:

    // the feed object we are building
    private SyndFeed feed = null;

    // memory of UI that called us, "xmlui" or "jspui"
    // affects Bitstream retrieval URL and I18N keys
    private String uiType = null;

    /**
     * Constructor.
     * @param ui either "xmlui" or "jspui"
     */
    public SyndicationFeed(String ui)
    {
        feed = new SyndFeedImpl();
        uiType = ui;
    }

    /**
     * Returns list of metadata selectors used to compose the description element
     *
     * @return selector list - format 'schema.element[.qualifier]'
     */
    public static String[] getDescriptionSelectors()
    {
        return descriptionFields;
    }


    /**
     * Fills in the feed and entry-level metadata from DSpace objects.
     */
    public void populate(HttpServletRequest request, DSpaceObject dso,
                         DSpaceObject items[], Map<String, String> labels)
    {
        String logoURL = null;
        String objectURL = null;
        String defaultTitle = null;

        // dso is null for the whole site, or a search without scope
        if (dso == null)
        {
            defaultTitle = ConfigurationManager.getProperty("dspace.name");
            feed.setDescription(localize(labels, MSG_FEED_DESCRIPTION));
            objectURL = resolveURL(request, null);
            logoURL = ConfigurationManager.getProperty("webui.feed.logo.url");
        }
        else
        {
            Bitstream logo = null;
            if (dso.getType() == Constants.COLLECTION)
            {
                Collection col = (Collection)dso;
                defaultTitle = col.getMetadata("name");
                feed.setDescription(col.getMetadata("short_description"));
                logo = col.getLogo();
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                Community comm = (Community)dso;
                defaultTitle = comm.getMetadata("name");
                feed.setDescription(comm.getMetadata("short_description"));
                logo = comm.getLogo();
            }
            objectURL = resolveURL(request, dso);
            if (logo != null)
                logoURL = urlOfBitstream(request, logo);
        }
        feed.setTitle(labels.containsKey(MSG_FEED_TITLE) ?
                            localize(labels, MSG_FEED_TITLE) : defaultTitle);
        feed.setLink(objectURL);
        feed.setPublishedDate(new Date());
        feed.setUri(objectURL);

        // add logo if we found one:
        if (logoURL != null)
        {
            // we use the path to the logo for this, the logo itself cannot
            // be contained in the rdf. Not all RSS-viewers show this logo.
            SyndImage image = new SyndImageImpl();
            image.setLink(objectURL);
            image.setTitle(localize(labels, MSG_LOGO_TITLE));
            image.setUrl(logoURL);
            feed.setImage(image);
        }

        // add entries for items
        if (items != null)
        {
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            for (DSpaceObject itemDSO : items)
            {
                if (itemDSO.getType() != Constants.ITEM)
                    continue;
                Item item = (Item)itemDSO;
                boolean hasDate = false;
                SyndEntry entry = new SyndEntryImpl();
                entries.add(entry);
             
                String entryURL = resolveURL(request, item);
                entry.setLink(entryURL);
                entry.setUri(entryURL);
             
                String title = getOneDC(item, titleField);
                entry.setTitle(title == null ? localize(labels, MSG_UNTITLED) : title);
             
                // "published" date -- should be dc.date.issued
                String pubDate = getOneDC(item, dateField);
                if (pubDate != null)
                {
                    entry.setPublishedDate((new DCDate(pubDate)).toDate());
                    hasDate = true;
                }
                // date of last change to Item
                entry.setUpdatedDate(item.getLastModified());
             
                StringBuffer db = new StringBuffer();
                for (String df : descriptionFields)
                {
                    // Special Case: "(date)" in field name means render as date
                    boolean isDate = df.indexOf("(date)") > 0;
                    if (isDate)
                        df = df.replaceAll("\\(date\\)", "");
             
                    DCValue dcv[] = item.getMetadata(df);
                    if (dcv.length > 0)
                    {
                        String fieldLabel = labels.get(MSG_METADATA + df);
                        if (fieldLabel != null && fieldLabel.length()>0)
                            db.append(fieldLabel + ": ");
                        boolean first = true;
                        for (DCValue v : dcv)
                        {
                            if (first)
                                first = false;
                            else
                                db.append("; ");
                            db.append(isDate ? new DCDate(v.value).toString() : v.value);
                        }
                        db.append("\n");
                    }
                }
                if (db.length() > 0)
                {
                    SyndContent desc = new SyndContentImpl();
                    desc.setType("text/plain");
                    desc.setValue(db.toString());
                    entry.setDescription(desc);
                }

                // This gets the authors into an ATOM feed
                DCValue authors[] = item.getMetadata(authorField);
                if (authors.length > 0)
                {
                    List<SyndPerson> creators = new ArrayList<SyndPerson>();
                    for (DCValue author : authors)
                    {
                        SyndPerson sp = new SyndPersonImpl();
                        sp.setName(author.value);
                        creators.add(sp);
                    }
                    entry.setAuthors(creators);
                }

                // only add DC module if any DC fields are configured
                if (dcCreatorField != null || dcDateField != null ||
                    dcDescriptionField != null)
                {
                    DCModule dc = new DCModuleImpl();
                    if (dcCreatorField != null)
                    {
                        DCValue dcAuthors[] = item.getMetadata(dcCreatorField);
                        if (dcAuthors.length > 0)
                        {
                            List<String> creators = new ArrayList<String>();
                            for (DCValue author : dcAuthors)
                                creators.add(author.value);
                            dc.setCreators(creators);
                        }
                    }
                    if (dcDateField != null && !hasDate)
                    {
                        DCValue v[] = item.getMetadata(dcDateField);
                        if (v.length > 0)
                            dc.setDate((new DCDate(v[0].value)).toDate());
                    }
                    if (dcDescriptionField != null)
                    {
                        DCValue v[] = item.getMetadata(dcDescriptionField);
                        if (v.length > 0)
                        {
                            StringBuffer descs = new StringBuffer();
                            for (DCValue d : v)
                            {
                                if (descs.length() > 0)
                                    descs.append("\n\n");
                                descs.append(d.value);
                            }
                            dc.setDescription(descs.toString());
                        }
                    }
                    entry.getModules().add(dc);
                }
            }
            feed.setEntries(entries);
        }
    }

    /**
     * Sets the feed type for XML delivery, e.g. "rss_1.0", "atom_1.0"
     * Must match one of ROME's configured generators, see rome.properties
     * (currently rss_1.0, rss_2.0, atom_1.0, atom_0.3)
     */
    public void setType(String feedType)
    {
        feed.setFeedType(feedType);
        // XXX FIXME: workaround ROME 1.0 bug, it puts invalid image element in rss1.0
        if (feedType.equals("rss_1.0"))
            feed.setImage(null);
    }

    /**
     * @return the feed we built as DOM Document
     */
    public Document outputW3CDom()
        throws FeedException
    {
        try
        {
            SyndFeedOutput feedWriter = new SyndFeedOutput();
            return feedWriter.outputW3CDom(feed);
        }
        catch (FeedException e)
        {
            log.error(e);
            throw e;
        }
    }

    /**
     * @return the feed we built as serialized XML string
     */
    public String outputString()
        throws FeedException
    {
        SyndFeedOutput feedWriter = new SyndFeedOutput();
        return feedWriter.outputString(feed);
    }

    /**
     * send the output to designated Writer
     */
    public void output(java.io.Writer writer)
        throws FeedException, IOException
    {
        SyndFeedOutput feedWriter = new SyndFeedOutput();
        feedWriter.output(feed, writer);
    }

    /**
     * @add a ROME plugin module (e.g. for OpenSearch) at the feed level
     */
    public void addModule(Module m)
    {
        feed.getModules().add(m);
    }

    // utility to get config property with default value when not set.
    private static String getDefaultedConfiguration(String key, String dfl)
    {
        String result = ConfigurationManager.getProperty(key);
        return (result == null) ? dfl : result;
    }

    // returns absolute URL to download content of bitstream (which might not belong to any Item)
    private String urlOfBitstream(HttpServletRequest request, Bitstream logo)
    {
        String name = logo.getName();
        return resolveURL(request,null) +
                 (uiType.equalsIgnoreCase(UITYPE_XMLUI) ?"/bitstream/id/":"/retrieve/") +
                 logo.getID()+"/"+(name == null?"":name);
    }

    /**
     * Return a url to the DSpace object, either use the official
     * handle for the item or build a url based upon the current server.
     *
     * If the dspaceobject is null then a local url to the repository is generated.
     *
     * @param dso The object to refrence, null if to the repository.
     * @return
     */
    private String baseURL = null;  // cache the result for null

    private String resolveURL(HttpServletRequest request, DSpaceObject dso)
    {
        // If no object given then just link to the whole repository,
        // since no offical handle exists so we have to use local resolution.
        if (dso == null)
        {
            if (baseURL == null)
            {
                if (request == null)
                    baseURL = ConfigurationManager.getProperty("dspace.url");
                else
                {
                    baseURL = (request.isSecure()) ? "https://" : "http://";
                    baseURL += ConfigurationManager.getProperty("dspace.hostname");
                    baseURL += ":" + request.getServerPort();
                    baseURL += request.getContextPath();
                }
            }
            return baseURL;
        }

        // return a link to handle in repository
        else if (ConfigurationManager.getBooleanProperty("webui.feed.localresolve"))
        {
            return resolveURL(request, null) + "/handle/" + dso.getHandle();
        }

        // link to the Handle server or other persistent URL source
        else
        {
            return HandleManager.getCanonicalForm(dso.getHandle());
        }
    }

    // retrieve text for localization key, or mark untranslated
    private String localize(Map<String, String> labels, String s)
    {
        return labels.containsKey(s) ? labels.get(s) : ("Untranslated:"+s);
    }

    // spoonful of syntactic sugar when we only need first value
    private String getOneDC(Item item, String field)
    {
        DCValue dcv[] = item.getMetadata(field);
        return (dcv.length > 0) ? dcv[0].value : null;
    }
}

