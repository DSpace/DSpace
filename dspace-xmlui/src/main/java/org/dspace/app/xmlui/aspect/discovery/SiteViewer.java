/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.xml.sax.SAXException;

/**
 * Adds feed links on the DSpace home page
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SiteViewer extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */

    public static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey()
    {
       return "1";
    }

    /**
     * Generate the cache validity object.
     */
    public SourceValidity getValidity()
    {
        return NOPValidity.SHARED_INSTANCE;
    }

    /**
     * Add a page title and trail links.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
    	pageMeta.addMetadata("title").addContent(T_dspace_home);
    	pageMeta.addTrailLink(contextPath, T_dspace_home);

        // Add RSS links if available
        String[] formats = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("webui.feed.formats");
		if ( formats != null )
		{
			for (String format : formats)
			{
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1)
                {
                    continue;
                }

				String feedFormat = parts[0].trim()+"+xml";

				String feedURL = contextPath+"/feed/"+format.trim()+"/site";
				pageMeta.addMetadata("feed", feedFormat).addContent(feedURL);
			}
		}
    }
}