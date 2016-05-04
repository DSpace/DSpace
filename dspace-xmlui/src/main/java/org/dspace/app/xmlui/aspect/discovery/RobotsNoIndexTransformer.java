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
 * @author philip at atmire.com
 */
public class RobotsNoIndexTransformer extends AbstractDSpaceTransformer {

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        super.addPageMeta(pageMeta);

        pageMeta.addMetadata("ROBOTS").addContent("NOINDEX, FOLLOW");
    }


}
