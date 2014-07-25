/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.batchimport;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
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
    private static final Message T_submit_upload = message("xmlui.administrative.batchimport.MetadataImportMain.submit_upload");
    private static final Message T_trail = message("xmlui.administrative.batchimport.general.trail");

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

        Para file = div.addPara();
        file.addFile("file");

        Para actions = div.addPara();
        Button button = actions.addButton("submit_upload");
        button.setValue(T_submit_upload);

        div.addHidden("administrative-continue").setValue(knot.getId());
    }



}
