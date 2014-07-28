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
    private static final Message T_trail = message("xmlui.administrative.batchimport.general.trail");
    private static final Message T_title = message("xmlui.administrative.batchimport.general.title");
    private static final Message T_head1 = message("xmlui.administrative.batchimport.general.head1");

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
