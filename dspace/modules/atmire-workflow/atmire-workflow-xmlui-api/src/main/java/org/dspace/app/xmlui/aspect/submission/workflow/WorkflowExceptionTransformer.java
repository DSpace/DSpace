package org.dspace.app.xmlui.aspect.submission.workflow;

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
 * @author Bram De Schouwer
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
        table.addRow().addCell().addContent("An error has occured:");
        table.addRow().addCell().addContent(error);

    }
}
