/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.metadataimport;

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
 * Web interface to Bulk Metadata Import app.
 * ported from org.dspace.app.webui.servlet.MetadataImportServlet
 *
 * Initial select file / upload CSV form
 *
 * @author Kim Shepherd
 */

public class MetadataImportMain extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_title = message("xmlui.administrative.metadataimport.general.title");
	private static final Message T_head1 = message("xmlui.administrative.metadataimport.general.head1");
        private static final Message T_submit_upload = message("xmlui.administrative.metadataimport.MetadataImportMain.submit_upload");
        private static final Message T_trail = message("xmlui.administrative.metadataimport.general.trail");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	
	public void addBody(Body body) throws SAXException, WingException, SQLException
	{

		// DIVISION: metadata-import
		Division div = body.addInteractiveDivision("metadata-import",contextPath + "/admin/metadataimport", Division.METHOD_MULTIPART,"primary administrative");
		div.setHead(T_head1);

                Para file = div.addPara();
                file.addFile("file");

                Para actions = div.addPara();
                Button button = actions.addButton("submit_upload");
                button.setValue(T_submit_upload);

		div.addHidden("administrative-continue").setValue(knot.getId());
	}
	
	

}
