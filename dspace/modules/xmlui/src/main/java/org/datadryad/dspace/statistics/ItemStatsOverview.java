
package org.datadryad.dspace.statistics;

import java.io.IOException;
import java.io.Serializable;

import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;

import org.datadryad.dspace.statistics.*;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;

import org.xml.sax.SAXException;

@SuppressWarnings("deprecation")
public class ItemStatsOverview extends AbstractDSpaceTransformer implements
		CacheableProcessingComponent {

	private static final Logger LOGGER = Logger
			.getLogger(ItemStatsOverview.class);

	private static final String HAS_PART = "dc.relation.haspart";

	private SourceValidity myValidity;

	public void addPageMeta(PageMeta aPageMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

		if (dso.getType() == Constants.ITEM) {
			Item item = (Item) dso;
			String hdl = dso.getHandle();
			Item pkg = (Item) HandleManager.resolveToDataPackage(context, hdl);

			try {
				if (hdl == null) {
					LOGGER.error("Failed to find a handle for item"); // never?
				}
				else if (pkg == null) {
					LOGGER.warn("Package DSO for " + hdl + " wasn't found");
				}
				// if item is a data file item
				else if (!hdl.equals(pkg.getHandle())) {
					ItemFileStats fileStats = new ItemFileStats(context, item);
					int downloads = fileStats.getFileDownloads();
					int views = fileStats.getDataFileViews();

					aPageMeta.addMetadata("dryad", "downloads").addContent(
							downloads);

					aPageMeta.addMetadata("dryad", "pageviews").addContent(
							views);
				}
				// if item is a data package item
				else {
					ItemPkgStats pkgStats = new ItemPkgStats(context, item);
					int views = pkgStats.getDataFileViews();
					int index = 1;

					aPageMeta.addMetadata("dryad", "pageviews").addContent(
							views);

					// this is expensive... cache this page if we do this;
					// and, move this into item page when we integrate file/pkg
					for (DCValue metadata : item.getMetadata(HAS_PART)) {
						ItemFileStats fileStats;
						int skip = 0;

						if (metadata.value.startsWith("http://hdl.")) {
							skip = 22;
						}
						else if (metadata.value.indexOf("/handle/") != -1) {
							skip = metadata.value.indexOf("/handle/") + 8;
						}
						else {
							// DOI? We need to change the way we handle this?
						}

						String handle = metadata.value.substring(skip);
						dso = HandleManager.resolveToObject(context, handle);

						fileStats = new ItemFileStats(context, (Item) dso);

						aPageMeta.addMetadata("dryad", "file-dl-" + index)
								.addContent(fileStats.getFileDownloads());

						aPageMeta.addMetadata("dryad", "file-view-" + index)
								.addContent(fileStats.getDataFileViews());
						
						index += 1;
					}
				}
			}
			catch (Exception details) {
				LOGGER.warn("Couldn't get file download count", details);
			}
		}
	}

//	@Override
	public Serializable getKey() {
		try {
			DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

			if (dso == null) return "0";

			return HashUtil.hash(dso.getHandle());
		}
		catch (SQLException details) {
			return "0";
		}
	}

//	@Override
	public SourceValidity getValidity() {
		if (myValidity == null) {
			DSpaceValidity validity = new DSpaceValidity();

			try {
				validity.add(HandleUtil.obtainHandle(objectModel) + "-stats");
				validity.setAssumedValidityDelay(86400000);
				myValidity = validity.complete();
			}
			catch (Exception details) {
				LOGGER.warn(details);
			}
		}

		return myValidity;
	}

}
