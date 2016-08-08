/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.batchimport;

import java.sql.SQLException;

import org.dspace.app.util.CollectionDropDown;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

/**
 * Web interface to BatchImport app.
 *
 * @author Peter Dietz
 */

public class BatchImportMain extends AbstractDSpaceTransformer {

    /** Language strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_title = message("xmlui.administrative.batchimport.general.title");
    private static final Message T_head1 = message("xmlui.administrative.batchimport.general.head1");
    private static final Message T_submit_upload = message("xmlui.administrative.batchimport.BatchmportMain.submit_upload");
    private static final Message T_trail = message("xmlui.administrative.batchimport.general.trail");

    private static final Message T_select_collection = message("xmlui.administrative.batchimport.general.select_collection");
    private static final Message T_collection = message("xmlui.administrative.batchimport.general.collection");
    private static final Message T_collection_help = message("xmlui.administrative.batchimport.general.collection_help");
    private static final Message T_collection_default = message("xmlui.administrative.batchimport.general.collection_default");

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws SAXException, WingException, SQLException
    {

        // DIVISION: batch-import
        Division div = body.addInteractiveDivision("batch-import",contextPath + "/admin/batchimport", Division.METHOD_MULTIPART,"primary administrative");
        div.setHead(T_head1);

        //Choose Destination Collection
        java.util.List<Collection> collections = collectionService.findAuthorized(context, null, Constants.ADD);

        List list = div.addList("select-collection", List.TYPE_FORM);
        list.setHead(T_select_collection);
        Select select = list.addItem().addSelect("collectionHandle");
        select.setAutofocus("autofocus");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);

        select.addOption("",T_collection_default);
        for (Collection collection : collections)
        {
            select.addOption(collection.getHandle(), CollectionDropDown.collectionPath(context, collection));
        }

        //Zip File Upload
        Para file = div.addPara();
        file.addFile("file");

        Para actions = div.addPara();
        Button button = actions.addButton("submit_upload");
        button.setValue(T_submit_upload);

        div.addHidden("administrative-continue").setValue(knot.getId());
    }



}
