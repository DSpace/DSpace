/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmltest;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 * @author Scott Phillips
 */
public class Navigation extends AbstractDSpaceTransformer
{
    
   
    public void addOptions(Options options) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        List test = options.addList("XMLTest");
        test.setHead("XML Test");
        
        test.addItemXref(contextPath + "/xmltest/structural","Structural");
        test.addItemXref(contextPath + "/xmltest/HTML","HTML");
        List form = test.addList("FormTest");
        form.setHead("Forms");
        form.addItemXref(contextPath + "/xmltest/form/basic","Basic");
        form.addItemXref(contextPath + "/xmltest/form/inline","In line");
        form.addItemXref(contextPath + "/xmltest/form/advanced","Advanced");
    }
}
