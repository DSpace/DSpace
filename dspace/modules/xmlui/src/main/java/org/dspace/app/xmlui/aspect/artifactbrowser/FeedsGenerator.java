package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;


/**
 * This class adds the global feed links in the pageMeta, based on those formats allowed in the config
 * @author nestor
 */
public class FeedsGenerator extends AbstractDSpaceTransformer implements CacheableProcessingComponent {

    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
		String formats = ConfigurationManager.getProperty("webui.feed.formats");
		if ( formats != null ) {
			for (String format : formats.split(",")){
				// Remove the protocol number, i.e. just list 'rss' or' atom'
				String[] parts = format.split("_");
				if (parts.length < 1) {
		            continue;
		        }
				
				String feedFormat = parts[0].trim()+"+xml";
					
				String feedURL = contextPath+"/feed/"+format.trim()+"/site";
				pageMeta.addMetadata("feed_site", feedFormat).addContent(feedURL);
			}
		}
	}

	@Override
	public Serializable getKey() {
		return "1";
	}

	@Override
	public SourceValidity getValidity() {
		return NOPValidity.SHARED_INSTANCE;
	}

}
