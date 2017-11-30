/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.core.Context;
import org.dspace.sword.client.*;
import org.dspace.sword.client.exceptions.HttpException;
import org.dspace.sword.client.exceptions.InvalidHandleException;
import org.dspace.sword.client.exceptions.PackageFormatException;
import org.dspace.sword.client.exceptions.PackagerException;
import org.purl.sword.client.SWORDClientException;

/**
 * User: Robin Taylor
 * Date: 27/03/11
 * Time: 15:47
 */
public class DepositAction {

    private static final Message T_success = new Message("default", "xmlui.swordclient.DepositAction.success");
    private static final Message T_package_format_error = new Message("default", "xmlui.swordclient.DepositAction.package_format_error");
    private static final Message T_invalid_handle = new Message("default", "xmlui.swordclient.DepositAction.invalid_handle");
    private static final Message T_package_error = new Message("default", "xmlui.swordclient.DepositAction.package_error");
    private static final Message T_error = new Message("default", "xmlui.swordclient.DepositAction.error");

    private static Logger log = Logger.getLogger(DepositAction.class);

    public FlowResult processDeposit(Context context, String handle, DSpaceSwordClient DSClient)
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        try
        {
            DSClient.deposit(context, handle);
            result.setContinue(true);
            result.setOutcome(true);
            result.setMessage(T_success);
        }
        catch (PackageFormatException e)
        {
            log.error("Package Format Exception", e);
            result.setOutcome(false);
            result.setMessage(T_package_format_error);
        }
        catch (InvalidHandleException e)
        {
            log.error("Invalid handle Exception", e);
            result.setOutcome(false);
            result.setMessage(T_invalid_handle);
        }
        catch (PackagerException e)
        {
            log.error("Packager Exception", e);
            result.setOutcome(false);
            result.setMessage(T_package_error);
        }
        catch (SWORDClientException e)
        {
            log.error("SWORDClientException encountered " + e.getMessage(), e);
            result.setOutcome(false);
            result.setMessage(T_error.parameterize(e.getMessage()));
        }
        catch (HttpException e)
        {
            log.error("HttpException encountered " + e.getMessage(), e);
            result.setOutcome(false);
            result.setMessage(T_error.parameterize(e.getMessage()));
        }


        return result;
    }
}
