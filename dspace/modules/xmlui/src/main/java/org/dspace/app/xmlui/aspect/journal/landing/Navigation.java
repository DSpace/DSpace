
package org.dspace.app.xmlui.aspect.journal.landing;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;

import java.sql.SQLException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;
import java.io.IOException;

/**
 *
 * @author Nathan Day
 */
public class Navigation extends AbstractDSpaceTransformer {
    
    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException 
    {
        super.addPageMeta(pageMeta);
    }

    @Override
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        super.addOptions(options);
        options.addList("DryadSubmitData");
        options.addList("DryadSearch");
        options.addList("DryadConnect");
        options.addList("DryadMail");
    }
}
