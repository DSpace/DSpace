package org.dspace.app.xmlui.aspect.submission.workflow.actions.processingaction;

import java.io.IOException;
import java.sql.SQLException;
import org.dspace.app.xmlui.aspect.submission.workflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;



public class AfterPublicationActionXMLUI extends AbstractXMLUIAction {
    // messages
    private static final Message T_after_blackout_help =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.after_blackout_help");
    private static final Message T_after_blackout_submit =
            message("xmlui.Submission.workflow.AfterPublicationActionXMLUI.after_blackout_submit");
    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}