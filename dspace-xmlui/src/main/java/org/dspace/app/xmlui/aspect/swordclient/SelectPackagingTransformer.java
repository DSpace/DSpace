/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.sword.client.ServiceDocumentHelper;
import org.purl.sword.base.Collection;
import org.purl.sword.base.ServiceDocument;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * User: Robin Taylor
 * Date: 21-Sep-2010
 * Time: 13:44:28
 */
public class SelectPackagingTransformer extends AbstractDSpaceTransformer
{
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.swordclient.SelectCollection.title");
    private static final Message T_SwordCopy_trail = message("xmlui.swordclient.general.SwordCopy_trail");
    private static final Message T_trail = message("xmlui.swordclient.SelectCollection.trail");
    private static final Message T_main_head = message("xmlui.swordclient.general.main_head");

    private static final Message T_collection_head = message("xmlui.swordclient.SelectPackagingAction.head");
    private static final Message T_collection_title = message("xmlui.swordclient.SelectPackagingAction.title");
    private static final Message T_collection_policy = message("xmlui.swordclient.SelectPackagingAction.policy");
    private static final Message T_collection_mediation = message("xmlui.swordclient.SelectPackagingAction.mediation");
    private static final Message T_collection_file_types = message("xmlui.swordclient.SelectPackagingAction.file_types");
    private static final Message T_collection_package_formats = message("xmlui.swordclient.SelectPackagingAction.package_formats");

    private static final Message T_submit_next = message("xmlui.general.next");
    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static Logger log = Logger.getLogger(SelectPackagingTransformer.class);

    /**
     * Add a page title and trail links
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        pageMeta.addTrail().addContent(T_SwordCopy_trail);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        String handle = parameters.getParameter("handle", null);

        Request request = ObjectModelHelper.getRequest(objectModel);

        ServiceDocument serviceDoc = (ServiceDocument) request.getAttribute("serviceDoc");
        String location = (String) request.getAttribute("location");
        String[] fileTypes = (String[]) request.getAttribute("fileTypes");
        String[] packageFormats = (String[]) request.getAttribute("packageFormats");

        Collection collection = ServiceDocumentHelper.getCollection(serviceDoc, location);

        Division main = body.addInteractiveDivision("confirm-collection", contextPath + "/swordclient", Division.METHOD_POST, "");
        main.setHead(T_main_head.parameterize(handle));

        List collectionList = main.addList("collectionList", List.TYPE_FORM);

        collectionList.setHead(T_collection_head.parameterize(location));
        collectionList.addItem().addContent(T_collection_title.parameterize(collection.getTitle()));
        collectionList.addItem().addContent(T_collection_policy.parameterize(collection.getCollectionPolicy()));
        collectionList.addItem().addContent(T_collection_mediation.parameterize(Boolean.toString(collection.getMediation())));

        Select fileType = collectionList.addItem().addSelect("fileType");
        for (String ft : fileTypes) {
            fileType.addOption(false, ft, ft);
        }
        fileType.setLabel(T_collection_file_types);

        Select packageFormat = collectionList.addItem().addSelect("packageFormat");
        for (String pf : packageFormats) {
            packageFormat.addOption(false, pf, pf);
        }
        packageFormat.setLabel(T_collection_package_formats);

        Para buttonList = main.addPara();
        buttonList.addButton("submit_next").setValue(T_submit_next);
        buttonList.addButton("submit_cancel").setValue(T_submit_cancel);

        main.addHidden("swordclient-continue").setValue(knot.getId());

    }

}
