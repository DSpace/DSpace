/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.xmlworkflow;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.state.actions.ActionInterface;

/**
 * Factory class for the xmlui user interface transformers
 *
 * @author Bram De Schouwer (bram.deschouwer at dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class WorkflowXMLUIFactory {

    private static final String UI_IDENTIFIER_SUFFIX = "_xmlui";


    /**
     * Retrieves the actionInterface for the given action id
     * @param id the action id
     * @return the action interface
     */
    public static ActionInterface getActionInterface(String id){
       return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(id + UI_IDENTIFIER_SUFFIX, ActionInterface.class);
    }

}
