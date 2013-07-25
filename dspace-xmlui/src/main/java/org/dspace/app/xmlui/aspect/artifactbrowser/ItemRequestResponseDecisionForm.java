/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.TextArea;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.xml.sax.SAXException;

/**
 * Display to the user a simple decision form to select sending or not a file to
 * requester.
 * 
 * Original Concept, JSPUI version:    Universidade do Minho   at www.uminho.pt
 * Sponsorship of XMLUI version:    Instituto Oceanográfico de España at www.ieo.es
 * 
 * @author Adán Román Ruiz at arvo.es (added request item support)
 */
public class ItemRequestResponseDecisionForm extends AbstractDSpaceTransformer
		implements CacheableProcessingComponent {
	/** Language Strings */
	private static final Message T_title = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.title");

	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_trail = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.trail");

	private static final Message T_head = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.head");

	private static final Message T_para1 = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.para1");

	private static final Message T_para2 = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.para2");

	private static final Message T_send = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.send");

	private static final Message T_dontSend = message("xmlui.ArtifactBrowser.ItemRequestResponseDecisionForm.dontSend");

	/**
	 * Generate the unique caching key. This key must be unique inside the space
	 * of this component.
	 */
	public Serializable getKey() {

		String title = parameters.getParameter("title","");
		return HashUtil.hash(title);
	}

	/**
	 * Generate the cache validity object.
	 */
	public SourceValidity getValidity() {
		return NOPValidity.SHARED_INSTANCE;
	}

	public void addPageMeta(PageMeta pageMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException {

		Request request = ObjectModelHelper.getRequest(objectModel);
		String title = parameters.getParameter("title","");
		// Build the item viewer division.
		Division itemRequest = body.addInteractiveDivision("itemRequest-form",
				request.getRequestURI(), Division.METHOD_POST, "primary");
		itemRequest.setHead(T_head);

		itemRequest.addPara(T_para1.parameterize(title));
		itemRequest.addPara(T_para2);

		List form = itemRequest.addList("form", List.TYPE_FORM);

		form.addItem().addButton("send").setValue(T_send);
		form.addItem().addButton("dontSend").setValue(T_dontSend);

	}
}
