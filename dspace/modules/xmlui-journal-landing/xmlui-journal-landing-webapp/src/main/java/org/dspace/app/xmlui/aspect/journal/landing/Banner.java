/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import org.apache.log4j.Logger;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
/*
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Table;
*/
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.avalon.framework.parameters.ParameterException;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.SAXException;

/**
 *
 * @author Nathan Day
 */
public class Banner extends AbstractDSpaceTransformer {
    
    private static final Logger log = Logger.getLogger(Banner.class);
    
    // 
    private static final Message ABC = message("");
    
    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        // ------------------
        // Journal X
        // 1 sentence scope
        // Publisher:
        // Society: 
        // Editorial review:
        // ------------------
        String journalName;
        try {
            journalName = this.parameters.getParameter(PARAM_JOURNAL_NAME);
        } catch (ParameterException ex) {
            log.error("Failed to retrieve journal metadata from parameters");
            return;
        }

        Division div = body.addDivision(DRI_LANDING_PAGE_BANNER_DIV);
        Para p = div.addPara(DRI_LANDING_PAGE_BANNER_PARA, null);
        p.addContent("The journal name is: " + journalName);
    }
}
