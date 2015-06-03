/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.browseArtifacts;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.utils.DSpace;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;

/**
 * To hack the http status code of our static pages. Just the code the pages are
 * handled via global-variables.xsl and page-structure.xsl
 */
public class StaticPage extends AbstractDSpaceTransformer {
	DSpace dspace = new DSpace();
	RequestService rs = dspace.getServiceManager().getServiceByName(
			RequestService.class.getName(), RequestService.class);
	ConfigurationService cs = dspace.getServiceManager().getServiceByName(
			ConfigurationService.class.getName(), ConfigurationService.class);
	private static final String themePath = "/webapps/xmlui/themes/UFAL/lib/html/";

	@Override
	public void addPageMeta(PageMeta pageMeta) throws SAXException,
			WingException, UIException, SQLException, IOException,
			AuthorizeException {
	}

	@Override
	public void addBody(Body body) throws SAXException, WingException,
			UIException, SQLException, IOException, AuthorizeException,
			ProcessingException {
		if (checkExists()) {
			body.addDivision("staticPage");
		} else {
			; // empty body ensure 404
		}
	}

	private boolean checkExists() throws IOException {
		HttpServletRequest request = rs.getCurrentRequest()
				.getHttpServletRequest();
		String uri = request.getRequestURI();
		uri = cs.getProperty("dspace.dir") + themePath
				+ uri.replaceFirst(".*page/", "") + ".xml";
		File staticPage = new File(uri);
		return staticPage.exists();
	}
}
