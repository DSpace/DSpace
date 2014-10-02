/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;
import com.sun.syndication.feed.synd.SyndPerson;
import com.sun.syndication.feed.synd.SyndPersonImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.module.DCModuleImpl;
import com.sun.syndication.feed.module.DCModule;
import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.module.itunes.*;
import com.sun.syndication.feed.module.itunes.types.Duration;
import com.sun.syndication.io.SyndFeedOutput;
import com.sun.syndication.io.FeedException;

import org.apache.log4j.Logger;
import org.dspace.content.Bundle;

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
    private static String defaultExternalMedia = "dc.source.uri";

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

    // metadata field for Podcast external media source url
    private static String externalSourceField = getDefaultedConfiguration("webui.feed.podcast.sourceuri", defaultExternalMedia);

    // metadata field for Item dc:creator field in entry's DCModule (no default)
    private static String dcCreatorField = ConfigurationManager.getProperty("webui.feed.item.dc.creator");

    // metadata field for Item dc:date field in entry's DCModule (no default)
    private static String dcDateField = ConfigurationManager.getProperty("webui.feed.item.dc.date");

    // metadata field for Item dc:author field in entry's DCModule (no default)
    private static String dcDescriptionField = ConfigurationManager.getProperty("webui.feed.item.dc.description");

    // List of available mimetypes that we'll add to podcast feed. Multiple values separated by commas
    private static String podcastableMIMETypes = getDefaultedConfiguration("webui.feed.podcast.mimetypes", "audio/x-mpeg");

    // -------- Instance variables:

    // the feed object we are building
    private SyndFeed feed = null;

    // memory of UI that called us, "xmlui" or "jspui"
    // affects Bitstream retrieval URL and I18N keys
    private String uiType = null;

    private HttpServletRequest request = null;

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
        return (String[]) ArrayUtils.clone(descriptionFields);
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
        boolean podcastFeed = false;
        this.request = request;

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
                String cols = ConfigurationManager.getProperty("webui.feed.podcast.collections");
                if(cols != null && cols.length() > 1 && cols.contains(col.getHandle()) ) {
                    podcastFeed = true;
                }
            }
            else if (dso.getType() == Constants.COMMUNITY)
            {
                Community comm = (Community)dso;
                defaultTitle = comm.getMetadata("name");
                feed.setDescription(comm.getMetadata("short_description"));
                logo = comm.getLogo();
                String comms = ConfigurationManager.getProperty("webui.feed.podcast.communities");
                if(comms != null && comms.length() > 1 && comms.contains(comm.getHandle()) ){
                    podcastFeed = true;
                }
            }
            objectURL = resolveURL(request, dso);
            if (logo != null)
            {
                logoURL = urlOfBitstream(request, logo);
            }
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
            if (StringUtils.isNotBlank(feed.getTitle())) {
                image.setTitle(feed.getTitle());
            } else {
                image.setTitle(localize(labels, MSG_LOGO_TITLE));
            }
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
                {
                    continue;
                }
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
                    {
                        df = df.replaceAll("\\(date\\)", "");
                    }
             
                    Metadatum dcv[] = item.getMetadataByMetadataString(df);
                    if (dcv.length > 0)
                    {
                        String fieldLabel = labels.get(MSG_METADATA + df);
                        if (fieldLabel != null && fieldLabel.length()>0)
                        {
                            db.append(fieldLabel).append(": ");
                        }
                        boolean first = true;
                        for (Metadatum v : dcv)
                        {
                            if (first)
                            {
                                first = false;
                            }
                            else
                            {
                                db.append("; ");
                            }
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
                Metadatum authors[] = item.getMetadataByMetadataString(authorField);
                if (authors.length > 0)
                {
                    List<SyndPerson> creators = new ArrayList<SyndPerson>();
                    for (Metadatum author : authors)
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
                        Metadatum dcAuthors[] = item.getMetadataByMetadataString(dcCreatorField);
                        if (dcAuthors.length > 0)
                        {
                            List<String> creators = new ArrayList<String>();
                            for (Metadatum author : dcAuthors)
                            {
                                creators.add(author.value);
                            }
                            dc.setCreators(creators);
                        }
                    }
                    if (dcDateField != null && !hasDate)
                    {
                        Metadatum v[] = item.getMetadataByMetadataString(dcDateField);
                        if (v.length > 0)
                        {
                            dc.setDate((new DCDate(v[0].value)).toDate());
                        }
                    }
                    if (dcDescriptionField != null)
                    {
                        Metadatum v[] = item.getMetadataByMetadataString(dcDescriptionField);
                        if (v.length > 0)
                        {
                            StringBuffer descs = new StringBuffer();
                            for (Metadatum d : v)
                            {
                                if (descs.length() > 0)
                                {
                                    descs.append("\n\n");
                                }
                                descs.append(d.value);
                            }
                            dc.setDescription(descs.toString());
                        }
                    }
                    entry.getModules().add(dc);
                }

                //iTunes Podcast Support - START
                if (podcastFeed)
                {
                    // Add enclosure(s)
                    List<SyndEnclosure> enclosures = new ArrayList();
                    try {
                        Bundle[] bunds = item.getBundles("ORIGINAL");
                        if (bunds[0] != null) {
                            Bitstream[] bits = bunds[0].getBitstreams();
                            for (int i = 0; (i < bits.length); i++) {
                                String mime = bits[i].getFormat().getMIMEType();
                                if(podcastableMIMETypes.contains(mime)) {
                                    SyndEnclosure enc = new SyndEnclosureImpl();
                                    enc.setType(bits[i].getFormat().getMIMEType());
                                    enc.setLength(bits[i].getSize());
                                    enc.setUrl(urlOfBitstream(request, bits[i]));
                                    enclosures.add(enc);
                                } else {
                                    continue;
                                }
                            }
                        }
                        //Also try to add an external value from dc.identifier.other
                        // We are assuming that if this is set, then it is a media file
                        Metadatum[] externalMedia = item.getMetadataByMetadataString(externalSourceField);
                        if(externalMedia.length > 0)
                        {
                            for(int i = 0; i< externalMedia.length; i++)
                            {
                                SyndEnclosure enc = new SyndEnclosureImpl();
                                enc.setType("audio/x-mpeg");        //We can't determine MIME of external file, so just picking one.
                                enc.setLength(1);
                                enc.setUrl(externalMedia[i].value);
                                enclosures.add(enc);
                            }
                        }

                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    entry.setEnclosures(enclosures);

                    // Get iTunes specific fields: author, subtitle, summary, duration, keywords
                    EntryInformation itunes = new EntryInformationImpl();

                    String author = getOneDC(item, authorField);
                    if (author != null && author.length() > 0) {
                        itunes.setAuthor(author);                               // <itunes:author>
                    }

                    itunes.setSubtitle(title == null ? localize(labels, MSG_UNTITLED) : title); // <itunes:subtitle>

                    if (db.length() > 0) {
                        itunes.setSummary(db.toString());                       // <itunes:summary>
                    }

                    String extent = getOneDC(item, "dc.format.extent");         // assumed that user will enter this field with length of song in seconds
                    if (extent != null && extent.length() > 0) {
                        extent = extent.split(" ")[0];
                        Integer duration = Integer.parseInt(extent);
                        itunes.setDuration(new Duration(duration));             // <itunes:duration>
                    }

                    String subject = getOneDC(item, "dc.subject");
                    if (subject != null && subject.length() > 0) {
                        String[] subjects = new String[1];
                        subjects[0] = subject;
                        itunes.setKeywords(subjects);                           // <itunes:keywords>
                    }

                    entry.getModules().add(itunes);
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
        if ("rss_1.0".equals(feedType))
        {
            feed.setImage(null);
        }
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
     * Add a ROME plugin module (e.g. for OpenSearch) at the feed level.
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
     * @param dso The object to reference, null if to the repository.
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
                {
                    baseURL = ConfigurationManager.getProperty("dspace.url");
                }
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
        Metadatum dcv[] = item.getMetadataByMetadataString(field);
        return (dcv.length > 0) ? dcv[0].value : null;
    }
}

