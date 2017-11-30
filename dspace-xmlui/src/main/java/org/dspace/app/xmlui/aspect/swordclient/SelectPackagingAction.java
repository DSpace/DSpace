/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.core.Context;
import org.dspace.sword.client.DSpaceSwordClient;
import org.dspace.sword.client.exceptions.PackageFormatException;

/**
 * User: Robin Taylor
 * Date: 21/03/11
 * Time: 22:12
 */
public class SelectPackagingAction
{
    private static final Message T_packageFormat_error = new Message("default", "xmlui.swordclient.SelectPackagingFormat.packageFormat_error");

    public FlowResult processSelectPackaging(Context context, Request request, DSpaceSwordClient DSClient)
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        // Get all our request parameters
        String fileType = request.getParameter("fileType");
        String packageFormat = request.getParameter("packageFormat");

        DSClient.setFileType(fileType);

        try
        {
            DSClient.setPackageFormat(packageFormat);
            result.setContinue(true);
            result.setOutcome(true);
        }
        catch (PackageFormatException e)
        {
            // This exception should never actually happen since the user selects from a drop down list but...
            result.setOutcome(false);
            result.setMessage(T_packageFormat_error);
        }


        return result;
    }
}
