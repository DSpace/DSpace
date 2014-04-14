/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.browseArtifacts;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Implements a Cocoon transformer for easily defining a static XMLUI page without the need to write any Java code
 * 
 * For info on usage, see https://wiki.duraspace.org/display/DSPACE/Manakin+theme+tutorial
 * 
 * @author Peter Dietz (pdietz84@gmail.com)
 */
public class StaticPage extends AbstractDSpaceTransformer {
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        pageMeta.addMetadata("title").addContent("StaticPageTitle");
        pageMeta.addTrail().addContent("StaticTrail");
    }

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException, ProcessingException {
        Division division = body.addDivision("staticpage");
        division.setHead("StaticPageBodyHead");
        division.addPara("StaticPageBodyPara");
    }
}
