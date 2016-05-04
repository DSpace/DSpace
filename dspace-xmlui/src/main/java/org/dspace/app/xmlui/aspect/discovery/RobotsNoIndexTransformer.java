/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.*;
import java.sql.*;
import org.dspace.app.xmlui.cocoon.*;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.*;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.*;
import org.xml.sax.*;

/**
 * Robots can not be blocked from dynamic URLs using robots.txt. This transformer is used instead to add
 * a meta tag that tells robots not to index the content of a page.
 * Information about this meta tag can be found here: http://www.robotstxt.org/meta.html
 *
 * @author philip at atmire.com
 */
public class RobotsNoIndexTransformer extends AbstractDSpaceTransformer {

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);

        pageMeta.addMetadata("ROBOTS").addContent("NOINDEX, FOLLOW");
    }


}
