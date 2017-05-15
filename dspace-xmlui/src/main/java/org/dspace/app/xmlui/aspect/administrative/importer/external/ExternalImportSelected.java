/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.importer.external;

import java.io.*;
import java.sql.*;
import java.util.*;
import org.apache.avalon.framework.parameters.*;
import org.apache.cocoon.*;
import org.apache.cocoon.environment.*;
import org.dspace.app.util.*;
import org.dspace.app.xmlui.cocoon.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.*;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.xml.sax.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 06/10/15
 * Time: 10:04
 */
public class ExternalImportSelected extends AbstractDSpaceTransformer {

    private static final Message T_DSPACE_HOME = message("xmlui.general.dspace_home");
    private static final Message T_trail = message("xmlui.administrative.importer.external.external-import.trail");
    private static final Message T_head = message("xmlui.administrative.importer.external.external-import.head");
    private static final Message T_records_selected = message("xmlui.administrative.importer.external.external-import-selected.records-selected");
    private static final Message T_import_workspace = message("xmlui.administrative.importer.external.external-import-selected.import-action-workspace");
    private static final Message T_import_workflow = message("xmlui.administrative.importer.external.external-import-selected.import-action-workflow");
    private static final Message T_import_archive = message("xmlui.administrative.importer.external.external-import-selected.import-action-archive");
    private static final Message T_submit_cancel = message("xmlui.administrative.importer.external.external-import-selected.submit-cancel");
    private static final Message T_submit_import = message("xmlui.administrative.importer.external.external-import-selected.submit-import");
    protected static final Message T_action = message("xmlui.administrative.importer.external.external-import-selected.action");
    protected static final Message T_collection = message("xmlui.administrative.importer.external.external-import-selected.collection");

    public static final String IMPORT_BUTTON = "submit_import";
    public static final String CANCEL_BUTTON = "submit_cancel";

    private Request request;

    private HashMap<String,SessionRecord> selected;

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        pageMeta.addMetadata("title").addContent(T_head);

        pageMeta.addTrailLink(contextPath + "/", T_DSPACE_HOME);
        pageMeta.addTrailLink(null, T_trail);
    }


    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        request = ObjectModelHelper.getRequest(objectModel);

        selected = new HashMap<>();
        if(request.getSession().getAttribute("selected")!=null) {
            selected = (HashMap<String,SessionRecord>) request.getSession().getAttribute("selected");
        }
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {
        Division div = body.addInteractiveDivision("live-import-result", contextPath + "/admin/external-import/import", Division.METHOD_POST, "");
        div.setHead(T_head);

        div.addPara().addContent(T_records_selected.parameterize(selected.size()));

        for (SessionRecord sessionRecord : selected.values()) {
            Division selected = div.addDivision("selected","import-record");
            selected.addPara("","record-title").addContent(sessionRecord.getTitle());
            selected.addPara("","record-authors").addContent(sessionRecord.getAuthors());
        }

        div.addPara().addContent(T_action);

        Radio radio = div.addPara().addRadio("import-action");
        radio.addOption(true,"workspace",T_import_workspace);
        radio.addOption("workflow", T_import_workflow);
        radio.addOption("archive", T_import_archive);

        div.addPara().addContent(T_collection);

        Select select = div.addPara().addSelect("import-collection");
        java.util.List<Collection> collections = collectionService.findAuthorizedOptimized(context, Constants.ADD);
        CollectionDropDown.CollectionPathEntry[] collectionPaths = CollectionDropDown.annotateWithPaths(context, collections);
        for (CollectionDropDown.CollectionPathEntry entry : collectionPaths)
        {
            select.addOption(entry.collection.getHandle(), entry.path);
        }

        Para para = div.addDivision("navigation-buttons").addPara();
        para.addButton(CANCEL_BUTTON).setValue(T_submit_cancel);
        para.addButton(IMPORT_BUTTON).setValue(T_submit_import);
    }
}
