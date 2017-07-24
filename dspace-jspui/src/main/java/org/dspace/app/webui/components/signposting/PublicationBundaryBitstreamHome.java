/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components.signposting;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.plugin.PluginException;
import org.dspace.plugin.signposting.BitstreamSignPostingProcessor;

/**
 * @author Pascarelli Luigi Andrea
 */
public class PublicationBundaryBitstreamHome
        implements BitstreamSignPostingProcessor
{

    /** log4j category */
    private static Logger log = Logger
            .getLogger(PublicationBundaryBitstreamHome.class);

    private String relation = "collection";

    @Override
    public void process(Context context, HttpServletRequest request,
            HttpServletResponse response, Bitstream bitstream)
            throws PluginException, AuthorizeException
    {

        try
        {
            DSpaceObject dso = bitstream.getParentObject();
            if (dso != null)
            {
                String value = ConfigurationManager.getProperty("dspace.url");
                String handle = dso.getHandle();
                value = value + "/handle/";
                value = value + UIUtil.encodeBitstreamName(handle,
                        Constants.DEFAULT_ENCODING);
                response.addHeader("Link", value + "; rel=\"" + getRelation()
                        + "\"");
            }
        }
        catch (Exception ex)
        {
            log.error("Problem to add signposting pattern on bitstream", ex);
        }
    }

    public String getRelation()
    {
        return relation;
    }

    public void setRelation(String relation)
    {
        this.relation = relation;
    }

}
