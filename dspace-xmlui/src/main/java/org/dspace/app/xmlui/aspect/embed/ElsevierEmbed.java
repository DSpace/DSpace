/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.embed;

import java.io.*;
import java.sql.*;
import org.apache.cocoon.environment.*;
import org.apache.log4j.*;
import org.dspace.app.xmlui.cocoon.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.*;
import org.dspace.content.*;
import org.dspace.content.Item;
import org.dspace.core.*;
import org.dspace.fileaccess.factory.*;
import org.dspace.fileaccess.service.*;
import org.dspace.fileaccess.service.ItemMetadataService.*;
import org.xml.sax.*;

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
    protected final Message T_title = message("xmlui.aspect.embed.ElsevierEmbed.title");
    protected final Message T_trail = message("xmlui.aspect.embed.ElsevierEmbed.trail");
    protected final Message T_item_trail = message("xmlui.ArtifactBrowser.ItemViewer.trail");
    protected final Message T_title_error = message("xmlui.aspect.embed.ElsevierEmbed.title_error");

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
        pageMeta.addTrailLink(contextPath + "/handle/" + handle, T_item_trail);
        pageMeta.addTrail().addContent(T_trail);

    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        String embeddedType = ObjectModelHelper.getRequest(objectModel).getParameter("embeddedType");
        String identifier = parameters.getParameter("identifier", null);
        Message errorMessage = validPage(dso);
        if (errorMessage == null) {
            Division division = body.addDivision("ElsevierEmbed", "ElsevierEmbed");
            division.setHead(T_title);

            List list = division.addList("embed-info");
            list.addItem().addHidden("identifier").setValue(identifier);
            list.addItem().addHidden("embeddedType").setValue(embeddedType);
        } else {
            Division division = body.addDivision("general-message", "failure");
            division.setHead(T_title_error);
            division.addPara(errorMessage);
        }
    }

    private Message validPage(DSpaceObject dso) {
        Message errorMessage = null;

        boolean embedDisplay = ConfigurationManager.getBooleanProperty("external-sources.elsevier.embed.display");
        if (!embedDisplay) {
            errorMessage = message("xmlui.aspect.embed.ElsevierEmbed.error.embed_page_disabled");
        } else if (!(dso instanceof Item)) {
            // the sitemap matchers already prevent this from happening
            errorMessage = message("xmlui.aspect.embed.ElsevierEmbed.error.not_an_item");
        } else {
            Item item = (Item) dso;
            String embeddedType = ObjectModelHelper.getRequest(objectModel).getParameter("embeddedType");

            String identifier = parameters.getParameter("identifier", null);
            IdentifierTypes identifierType=null;
            try{
                identifierType = IdentifierTypes.valueOf(embeddedType.toUpperCase());
            }catch (IllegalArgumentException e){
                errorMessage = message("mlui.aspect.embed.ElsevierEmbed.error.invalid_type");
            }
            if (identifierType == null || identifier == null || !identifier.equals(identifierType.getIdentifier(item))) {
                errorMessage = message("xmlui.aspect.embed.ElsevierEmbed.error.invalid_identifier");
            }

        }
        return errorMessage;
    }
}