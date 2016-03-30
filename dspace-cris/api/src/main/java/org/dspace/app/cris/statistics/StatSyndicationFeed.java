/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.app.cris.integration.statistics.AStatComponentService;
import org.dspace.app.cris.model.StatSubscription;
import org.dspace.core.ConfigurationManager;
import org.w3c.dom.Document;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

/**
 * Invoke ROME library to assemble a generic model of a syndication for the
 * given list of statistics data. Consults configuration for the metadata
 * bindings to feed elements. Uses ROME's output drivers to return any of the
 * implemented formats, e.g. RSS 1.0, RSS 2.0, ATOM 1.0.
 * 
 * Based on the org.dspace.app.util.SyndicationFeed class.
 * 
 * @author Andrea Bollini
 */
public class StatSyndicationFeed
{
    private static final Logger log = Logger
            .getLogger(StatSyndicationFeed.class);

    private static final String MSG_FEED_TITLE = "feed.stats.general-title";

    private static final String MSG_FEED_DESCRIPTION = "feed.stats.general-description";

    // private static final String MSG_LOGO_TITLE = "feed.stats.logo-title";

    private static final String MSG_ENTRY_TITLE = "feed.stats.entry-title";

    private static final String MSG_ENTRY_DESCRIPTION = "feed.stats.entry-description";

    /** i18n key values */

    // -------- Instance variables:

    // the feed object we are building
    private SyndFeed feed = null;

    /**
     * Constructor.
     */
    public StatSyndicationFeed()
    {
        feed = new SyndFeedImpl();
    }

    /**
     * Fills in the feed and entry-level metadata from DSpace objects.
     */
    public void populate(HttpServletRequest request, SummaryStatBean bean)
    {
        Locale locale = request.getLocale();
        ResourceBundle msgs = ResourceBundle.getBundle("Messages", locale);

        int type = bean.getType();
        int freq = bean.getFreq();

        // String logoURL = ConfigurationManager
        // .getProperty("webui.feed.stats.logo.url");
        String objectURL = bean.getObjectURL();

        feed.setTitle(MessageFormat.format(
                localize(msgs, MSG_FEED_TITLE, freq, type, null, null),
                bean.getObjectName()));
        feed.setDescription(MessageFormat.format(
                localize(msgs, MSG_FEED_DESCRIPTION, freq, type, null, null),
                bean.getObjectName()));
        feed.setLink(objectURL);
        feed.setPublishedDate(new Date());
        feed.setUri(objectURL);

        // // add logo if we found one:
        // if (logoURL != null)
        // {
        // // we use the path to the logo for this, the logo itself cannot
        // // be contained in the rdf. Not all RSS-viewers show this logo.
        // SyndImage image = new SyndImageImpl();
        // image.setLink(objectURL);
        // image.setTitle(localize(labels, MSG_LOGO_TITLE, freq, type));
        // image.setUrl(logoURL);
        // feed.setImage(image);
        // }

        // add entries for items
        if (bean.getData() != null)
        {
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            for (SummaryStatBean.StatDataBean dataBean : bean.getData())
            {
                Date date = dataBean.getDate();

                SyndEntry entry = new SyndEntryImpl();
                entries.add(entry);

                // all the feed has the same url... details on the statistics
                // page
                entry.setLink(bean.getStatURL() + "&freq=" + bean.getFreq()
                        + "&date=" + date.getTime());
                // entry.setUri();

                entry.setTitle(MessageFormat
                        .format(localize(msgs, MSG_ENTRY_TITLE, freq, type,
                                null, null), bean.getObjectName(), dataBean
                                .getDate()));

                Calendar c1 = Calendar.getInstance();
                c1.setTime(date);
                if (bean.getFreq() != StatSubscription.FREQUENCY_MONTHLY)
                {
                    c1.add(Calendar.DAY_OF_MONTH, 1);
                }
                else
                {
                    c1.add(Calendar.MONTH, 1);
                }
                date = c1.getTime();
                entry.setPublishedDate(date);
                // date of last change to Item
                entry.setUpdatedDate(dataBean.getDate());

                SyndContent desc = new SyndContentImpl();
                desc.setType("text/html");

                String tmpMessage = "";

                tmpMessage += MessageFormat
                        .format(localize(msgs, MSG_ENTRY_DESCRIPTION, freq,
                                type, AStatComponentService._SELECTED_OBJECT,
                                "view"), dataBean.getPeriodSelectedView(),
                                dataBean.getTotalSelectedView(), AStatComponentService._SELECTED_OBJECT);

                if (dataBean.isShowSelectedObjectDownload())
                {
                    tmpMessage += MessageFormat.format(
                            localize(msgs, MSG_ENTRY_DESCRIPTION, freq, type,
                                    AStatComponentService._SELECTED_OBJECT,
                                    "download"), dataBean
                                    .getPeriodSelectedDownload(), dataBean
                                    .getTotalSelectedDownload(), AStatComponentService._SELECTED_OBJECT);
                }

                for (String key : dataBean.getPeriodAndTotalTopView().keySet())
                {
                    if (dataBean.getPeriodAndTotalTopView().get(key) != null)
                    {
                        if (dataBean.getPeriodAndTotalTopView().get(key).size() > 0)
                        {
                            tmpMessage += MessageFormat.format(
                                    localize(msgs, MSG_ENTRY_DESCRIPTION, freq,
                                            type, key, "view"), dataBean
                                            .getPeriodAndTotalTopView()
                                            .get(key).get(0), dataBean
                                            .getPeriodAndTotalTopView()
                                            .get(key).get(1), key);
                        }
                        if (dataBean.getPeriodAndTotalTopDownload().get(key) != null)
                        {
                            if (dataBean.getPeriodAndTotalTopDownload()
                                    .get(key).size() > 0)
                            {
                                tmpMessage += MessageFormat.format(
                                        localize(msgs, MSG_ENTRY_DESCRIPTION,
                                                freq, type, key, "download"),
                                        dataBean.getPeriodAndTotalTopDownload()
                                                .get(key).get(0), dataBean
                                                .getPeriodAndTotalTopDownload()
                                                .get(key).get(1), key);
                            }
                        }

                    }

                }

                String message = MessageFormat.format(
                        localize(msgs, MSG_ENTRY_DESCRIPTION, freq, type, null,
                                null), bean.getObjectName(),
                        dataBean.getDate(), tmpMessage);

                desc.setValue(message);
                entry.setDescription(desc);

                entry.setAuthor(ConfigurationManager
                        .getProperty("webui.feed.stats.authors"));
            }
            feed.setEntries(entries);
        }
    }

    /**
     * Sets the feed type for XML delivery, e.g. "rss_1.0", "atom_1.0" Must
     * match one of ROME's configured generators, see rome.properties (currently
     * rss_1.0, rss_2.0, atom_1.0, atom_0.3)
     */
    public void setType(String feedType)
    {
        feed.setFeedType(feedType);
        // XXX FIXME: workaround ROME 1.0 bug, it puts invalid image element in
        // rss1.0
        if (feedType.equals("rss_1.0"))
            feed.setImage(null);
    }

    /**
     * @return the feed we built as DOM Document
     */
    public Document outputW3CDom() throws FeedException
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
    public String outputString() throws FeedException
    {
        SyndFeedOutput feedWriter = new SyndFeedOutput();
        return feedWriter.outputString(feed);
    }

    /**
     * send the output to designated Writer
     */
    public void output(java.io.Writer writer) throws FeedException, IOException
    {
        SyndFeedOutput feedWriter = new SyndFeedOutput();
        feedWriter.output(feed, writer);
    }

    // retrieve text for localization key (use freq to customize the message for
    // a specific resource type)
    private String localize(ResourceBundle labels, String s, int freq,
            int type, String component, String mode)
    {
        String fulli18n = s + ".freq" + freq + ".type" + type;
        String notypei18n = s + ".freq" + freq;
        String defaulttypei18n = s;
        if (component != null)
        {
            fulli18n += "." + component;
            notypei18n += "." + component;
            if (mode != null)
            {
                fulli18n += "." + mode;
                notypei18n += "." + mode;
                defaulttypei18n += "." + mode;
            }
        }

        if (labels.containsKey(fulli18n))
            return labels.getString(fulli18n);
        else if (labels.containsKey(notypei18n))
            return labels.getString(notypei18n);
        else if (labels.containsKey(defaulttypei18n))
            return labels.getString(defaulttypei18n);
        else
            return labels.getString(s);
    }
}
