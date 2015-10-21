/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.ScienceDirect;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.fileaccess.factory.FileAccessServiceFactory;
import org.dspace.fileaccess.service.ItemMetadataService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 01 Oct 2015
 */
public class ElsevierEmbed extends AbstractDSpaceTransformer {

    /**
     * log4j logger
     */
    private static final Logger log = Logger.getLogger(ElsevierEmbed.class);

    protected final Message T_dspace_home = message("xmlui.general.dspace_home");
    protected final Message T_title = message("com.atmire.sciencedirect.embed.ElsevierEmbed.title");
    protected final Message T_trail = message("com.atmire.sciencedirect.embed.ElsevierEmbed.trail");
    protected final Message T_title_error = message("com.atmire.sciencedirect.embed.ElsevierEmbed.title_error");

    protected static ItemMetadataService itemMetadataService = FileAccessServiceFactory.getInstance().getItemMetadataService();

    /**
     * Initialize the page metadata & breadcrumb trail
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
        pageMeta.addMetadata("title").addContent(T_title);

        String handle = parameters.getParameter("handle", null);
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath);
        pageMeta.addTrailLink(contextPath + "/handle/", handle);
        pageMeta.addTrail().addContent(T_trail);

    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        String pii = parameters.getParameter("pii", null);
        Message errorMessage = validPage(dso);
        if (errorMessage == null) {
            Division division = body.addDivision("ElsevierEmbed", "ElsevierEmbed");
            division.setHead(T_title);

            List list = division.addList("embed-info");
            list.addItem().addHidden("pii").setValue(pii);
        } else {
            Division division = body.addDivision("general-message", "failure");
            division.setHead(T_title_error);
            division.addPara(errorMessage);
        }

    }

    private Message validPage(DSpaceObject dso) {
        Message errorMessage = null;

        boolean embedDisplay = ConfigurationManager.getBooleanProperty("elsevier-sciencedirect", "embed.display");
        if (!embedDisplay) {
            errorMessage = message("com.atmire.sciencedirect.embed.ElsevierEmbed.error.embed_page_disabled");
        } else if (!(dso instanceof Item)) {
            // the sitemap matchers already prevent this from happening
            errorMessage = message("com.atmire.sciencedirect.embed.ElsevierEmbed.error.not_an_item");
        } else {
            Item item = (Item) dso;

            String itemPII = itemMetadataService.getPII(item);
            String paramPii = parameters.getParameter("pii", null);

            if (paramPii == null || !paramPii.equals(itemPII)) {
                errorMessage = message("com.atmire.sciencedirect.embed.ElsevierEmbed.error.invalid_pii");
            }

        }
        return errorMessage;
    }
}