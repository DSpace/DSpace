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
public class SelectCollectionTransformer extends AbstractDSpaceTransformer {

    private static Logger log = Logger.getLogger(SelectCollectionTransformer.class);

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_title = message("xmlui.swordclient.SelectCollection.title");
    private static final Message T_SwordCopy_trail = message("xmlui.swordclient.general.SwordCopy_trail");
    private static final Message T_trail = message("xmlui.swordclient.SelectCollection.trail");
    private static final Message T_main_head = message("xmlui.swordclient.general.main_head");

    private static final Message T_collection_head = message("xmlui.swordclient.SelectCollection.collection_head");
    private static final Message T_collection_title = message("xmlui.swordclient.SelectCollection.collection_title");
    private static final Message T_collection_policy = message("xmlui.swordclient.SelectCollection.collection policy");
    private static final Message T_collection_mediation = message("xmlui.swordclient.SelectCollection.collection_mediation");
    private static final Message T_collection_file_types = message("xmlui.swordclient.SelectCollection.collection_file_types");
    private static final Message T_collection_package_formats = message("xmlui.swordclient.SelectCollection.collection_package_formats");
    private static final Message T_collection_deposit_button = message("xmlui.swordclient.SelectCollection.collection_deposit_button");
    private static final Message T_sub_service_target = message("xmlui.swordclient.SelectCollection.sub_service_target");
    private static final Message T_sub_service_target_button = message("xmlui.swordclient.SelectCollection.sub_service_target_button");



    private static final Message T_submit_cancel = message("xmlui.general.cancel");

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

        Division main = body.addDivision("main");
        main.setHead(T_main_head.parameterize(handle));

        Request request = ObjectModelHelper.getRequest(objectModel);
        ServiceDocument serviceDoc = (ServiceDocument) request.getAttribute("serviceDoc");

        java.util.List<Collection> collections = ServiceDocumentHelper.getCollections(serviceDoc);
        for (Collection collection : collections)
        {
            Division collectionDiv = main.addInteractiveDivision("collection", contextPath + "/swordclient", Division.METHOD_POST, "");

            // Add a header for each individual collection.
            collectionDiv.setHead(T_collection_head.parameterize(collection.getLocation()));

            // Now add another list of the remaining collection parameters.
            List paramsList = collectionDiv.addList(collection + "Params", List.TYPE_BULLETED);

            paramsList.addItem().addContent(T_collection_title.parameterize(collection.getTitle()));
            paramsList.addItem().addContent(T_collection_policy.parameterize(collection.getCollectionPolicy()));
            paramsList.addItem().addContent(T_collection_mediation.parameterize(Boolean.toString(collection.getMediation())));

            String[] fileTypes = collection.getAccepts();
            String fileTypesString = arrayToString(fileTypes);
            paramsList.addItem().addContent(T_collection_file_types.parameterize(fileTypesString));

            String[] packageFormats = ServiceDocumentHelper.getPackageFormats(collection);
            String packageFormatsString = arrayToString(packageFormats);
            paramsList.addItem().addContent(T_collection_package_formats.parameterize(packageFormatsString));

            // Assuming there are available file types and package formats then add a deposit button.
            if ((fileTypes.length > 0 ) && (packageFormats.length > 0))
            {
                collectionDiv.addPara().addButton("deposit").setValue(T_collection_deposit_button);
                collectionDiv.addHidden("location").setValue(collection.getLocation());
                collectionDiv.addHidden("swordclient-continue").setValue(knot.getId());
            }

            // If the collection contains a reference to  a 'sub service' then allow the user to select
            // the service doc for that sub service.
            if ((collection.getService() != null) && (collection.getService().length() > 0))
            {
                collectionDiv.addPara(T_sub_service_target + collection.getService());
                collectionDiv.addPara().addButton("sub-service").setValue(T_sub_service_target_button);
                collectionDiv.addHidden("sub-service").setValue(collection.getService());
                collectionDiv.addHidden("swordclient-continue").setValue(knot.getId());
            }

        }

        Division buttons = main.addInteractiveDivision("buttons", contextPath + "/swordclient", Division.METHOD_POST, "");

        Para buttonList = buttons.addPara();
        buttonList.addButton("submit_cancel").setValue(T_submit_cancel);
        buttons.addHidden("swordclient-continue").setValue(knot.getId());

    }


    private String arrayToString(String[] strings)
    {
        if (strings.length == 0)
        {
            return "none";
        }
        else
        {
            StringBuffer text = new StringBuffer("");
            for (String string : strings)
            {
                text.append(string).append(", ");
            }
            text.delete(text.length() - 2, text.length() - 1);
            return text.toString();
        }
    }

}
