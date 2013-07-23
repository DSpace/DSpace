 /**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.xmlui.aspect.swordclient;

import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.FlowResult;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.core.Context;
import org.dspace.sword.client.exceptions.HttpException;
import org.dspace.sword.client.DSpaceSwordClient;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.client.SWORDClientException;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * User: Robin Taylor
 * Date: 08/02/11
 * Time: 21:41
 */
public class SelectTargetAction
{
    private static final Message T_url_error = new Message("default", "xmlui.swordclient.SelectTargetAction.url_error");
    private static final Message T_serviceDoc_error = new Message("default", "xmlui.swordclient.SelectTargetAction.serviceDoc_error");

    private static Logger log = Logger.getLogger(SelectTargetAction.class);


    public FlowResult processSelectTarget(Context context, Request request, DSpaceSwordClient DSClient)
    {
        FlowResult result = new FlowResult();
        result.setContinue(false);

        // Get all our request parameters
        String url = request.getParameter("url").trim();
        String otherUrl = request.getParameter("otherUrl").trim();
        String username = request.getParameter("username").trim();
        String password = request.getParameter("password").trim();
        String onBehalfOf = request.getParameter("onBehalfOf").trim();

        // If we have errors, the form needs to be resubmitted to fix those problems

        String chosenUrl = "";

        if (!StringUtils.isEmpty(otherUrl))
        {
            // If otherUrl has been entered then it will take precedence.
            try
            {
                new URL(otherUrl);
                chosenUrl = otherUrl;
            }
            catch (MalformedURLException e)
            {
                result.addError("otherUrl");
            }
        }
        else
        {
            if (!StringUtils.isEmpty(url))
            {
                chosenUrl = url;
            }
            else
            {
                result.addError("url");
            }
        }

        if (StringUtils.isEmpty(username))
        {
            result.addError("username");
        }
        if (StringUtils.isEmpty(password))
        {
            result.addError("password");
        }

        // No errors, the input parameters look healthy.
        if (result.getErrors() == null)
        {
            try
            {
                DSClient.setRemoteServer(chosenUrl);
                DSClient.setCredentials(username, password, onBehalfOf);
                ServiceDocument serviceDoc = DSClient.getServiceDocument();
                result.setParameter("serviceDoc", serviceDoc);
                result.setContinue(true);
                result.setOutcome(true);
            }
            catch (MalformedURLException e)
            {
                log.error("Malformed URL : " + chosenUrl);
                result.setOutcome(false);
			    result.setMessage(T_url_error);
            }
            catch (HttpException e)
            {
                log.error("HttpException encountered", e);
                result.setOutcome(false);
			    result.setMessage(T_serviceDoc_error.parameterize(e.getMessage()));
            }
            catch (SWORDClientException e)
            {
                log.error("SwordClientException : " + e.getMessage(), e);
                result.setOutcome(false);
			    result.setMessage(T_serviceDoc_error.parameterize(e.getMessage()));
            }

        }

        return result;
    }

    public FlowResult processSelectSubTarget(Context context, Request request, DSpaceSwordClient DSClient)
       {
           FlowResult result = new FlowResult();
           result.setContinue(false);


           // Get all our request parameters.
           String url = request.getParameter("sub-service").trim();

           log.info("target selected is : " + url);

           if (StringUtils.isEmpty(url))
           {
               // Note : this shouldn't ever happen since the user doesn't enter it manually.
               result.addError("sub-service");
           }

           // No errors, the input parameters look healthy.
           if (result.getErrors() == null)
           {
               try
               {
                   DSClient.setRemoteServer(url);
                   ServiceDocument serviceDoc = DSClient.getServiceDocument();
                   result.setParameter("serviceDoc", serviceDoc);
                   result.setOutcome(true);
               }
               catch (MalformedURLException e)
               {
                   log.error("Malformed URL : " + url);
                   result.setOutcome(false);
                   result.setMessage(T_url_error);
               }
               catch (HttpException e)
               {
                   log.error("HttpException encountered", e);
                   result.setOutcome(false);
                   result.setMessage(T_serviceDoc_error.parameterize(e.getMessage()));
               }
               catch (SWORDClientException e)
               {
                   log.error("SwordClientException : " + e.getMessage(), e);
                   result.setOutcome(false);
                   result.setMessage(T_serviceDoc_error.parameterize(e.getMessage()));
               }

           }

           return result;
       }


}
