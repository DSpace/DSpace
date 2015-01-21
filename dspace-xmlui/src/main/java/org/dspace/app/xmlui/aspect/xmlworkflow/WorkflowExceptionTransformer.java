/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;
import org.apache.avalon.framework.parameters.ParameterException;

import java.sql.SQLException;
import java.io.IOException;

/**
 * A transformer that is called when the workflow encounteres an exception
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowExceptionTransformer extends AbstractDSpaceTransformer {
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        String error = "";
        try {
            error = parameters.getParameter("error");
        } catch (ParameterException e) {
           //Should not happen.
            assert false;
        }
        Division div = body.addDivision("error");
        Table table = div.addTable("table0",2,1);
        table.addRow().addCell().addContent("An error has occurred:");
        table.addRow().addCell().addContent(error);

    }
}
