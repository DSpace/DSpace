package org.datadryad.dspace.xmlui.selector;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.selection.Selector;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;


import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/5/11
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class WithdrawnSelector extends AbstractLogEnabled implements Selector{

    private static Logger log = Logger.getLogger(WithdrawnSelector.class);

    public boolean select(String expression, Map objectModel,Parameters parameters){
        try{
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if(dso instanceof Item){
                if( ((Item)dso).isWithdrawn())
                    return true;
            }
            return false;

        }
        catch (Exception e){
            // Log it and returned no match.
            log.error("Error selecting based on authentication status: "+ e.getMessage());
            return false;
        }
    }
}





