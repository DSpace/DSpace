/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.batchimport;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.DCValue;
import org.xml.sax.SAXException;

import org.dspace.app.bulkedit.BulkEditChange;

/**
 * Web interface to Batch Import app.
 *
 * Display form for user to review changes and confirm
 *
 * @author Peter Dietz
 */

public class BatchImportUpload extends AbstractDSpaceTransformer {

    /** Language strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_return = message("xmlui.general.return");
    private static final Message T_trail = message("xmlui.administrative.batchimport.general.trail");
    private static final Message T_no_changes = message("xmlui.administrative.batchimport.general.no_changes");
    private static final Message T_new_item = message("xmlui.administrative.batchimport.general.new_item");
    private static final Message T_title = message("xmlui.administrative.batchimport.general.title");
    private static final Message T_head1 = message("xmlui.administrative.batchimport.general.head1");

    private static final Message T_para = message("xmlui.administrative.batchimport.BatchImportUpload.hint");
    private static final Message T_submit_confirm = message("xmlui.administrative.batchimport.BatchImportUpload.submit_confirm");
    private static final Message T_changes_pending = message("xmlui.administrative.batchimport.BatchImportUpload.changes_pending");
    private static final Message T_item_addition = message("xmlui.administrative.batchimport.BatchImportUpload.item_add");
    private static final Message T_item_deletion = message("xmlui.administrative.batchimport.BatchImportUpload.item_remove");
    private static final Message T_collection_newowner = message("xmlui.administrative.batchimport.BatchImportUpload.collection_newowner");
    private static final Message T_collection_oldowner = message("xmlui.administrative.batchimport.BatchImportUpload.collection_oldowner");
    private static final Message T_collection_mapped = message("xmlui.administrative.batchimport.BatchImportUpload.collection_mapped");
    private static final Message T_collection_unmapped = message("xmlui.administrative.batchimport.BatchImportUpload.collection_unmapped");
    private static final Message T_item_delete = message("xmlui.administrative.batchimport.BatchImportUpload.item_delete");
    private static final Message T_item_withdraw = message("xmlui.administrative.batchimport.BatchImportUpload.item_withdraw");
    private static final Message T_item_reinstate = message("xmlui.administrative.batchimport.BatchImportUpload.item_reinstate");

    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }


    public void addBody(Body body) throws SAXException, WingException, SQLException
    {
        // Get list of changes
        Request request = ObjectModelHelper.getRequest(objectModel);

        // DIVISION: batch-import
        Division div = body.addInteractiveDivision("batch-import",contextPath + "/admin/batchimport", Division.METHOD_MULTIPART,"primary administrative");
        div.setHead(T_head1);


        div.addHidden("administrative-continue").setValue(knot.getId());
    }

}
