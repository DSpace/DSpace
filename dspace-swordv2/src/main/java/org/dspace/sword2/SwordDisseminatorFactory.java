/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;
import org.swordapp.server.UriRegistry;

import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.factory.CoreServiceFactory;

public class SwordDisseminatorFactory
{
    public static SwordContentDisseminator getContentInstance(
            Map<Float, List<String>> accept, String acceptPackaging)
            throws DSpaceSwordException, SwordError
    {
        try
        {
            SwordContentDisseminator disseminator = null;

            // first try to load disseminators based on content type
            if (accept != null)
            {
                for (Float q : accept.keySet())
                {
                    for (String format : accept.get(q))
                    {
                        format = format.replace(";",
                                "_"); // clean up the string for the plugin manager
                        format = format.replace("=",
                                "_"); // clean up the string for the plugin manager
                        disseminator = (SwordContentDisseminator) CoreServiceFactory.getInstance().getPluginService()
                                .getNamedPlugin(SwordContentDisseminator.class, format);
                        if (disseminator == null)
                        {
                            continue;
                        }
                        else
                        {
                            // if we find a disseminator which says it does this format, then find out if it
                            // will do the packaging
                            if (!disseminator
                                    .disseminatesPackage(acceptPackaging))
                            {
                                disseminator = null;
                                continue;
                            }
                            else
                            {
                                disseminator.setContentType(format);
                                break;
                            }
                        }
                    }
                }
            }

            // if we have not yet found a disseminator, try looking it up by packaging type
            if (disseminator == null)
            {
                if (acceptPackaging != null)
                {
                    acceptPackaging = acceptPackaging.replace(";",
                            "_"); // clean up the string for the plugin manager
                    acceptPackaging = acceptPackaging.replace("=",
                            "_"); // clean up the string for the plugin manager
                    disseminator = (SwordContentDisseminator) CoreServiceFactory.getInstance().getPluginService()
                            .getNamedPlugin(SwordContentDisseminator.class,
                                    acceptPackaging);
                    if (disseminator != null)
                    {
                        if (accept != null)
                        {
                            // Find first accept format that this disseminator works with
                            String disseminateFormat = null;
                            for (Float q : accept.keySet())
                            {
                                for (String format : accept.get(q))
                                {
                                    if (disseminator.disseminatesContentType(format))
                                    {
                                        disseminateFormat = format;
                                        break;
                                    }
                                }
                            }

                            if (StringUtils.isNotEmpty(disseminateFormat))
                            {
                                disseminator.setContentType(disseminateFormat);
                            }
                            else
                            {
                                // No matching disseminator found
                                disseminator = null;
                            }
                        }
                    }
                }
            }

            if (disseminator == null)
            {
                throw new SwordError(UriRegistry.ERROR_CONTENT, 406,
                        "No plugin can disseminate the requested formats");
            }

            disseminator.setPackaging(acceptPackaging);
            return disseminator;
        }
        catch (SwordServerException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    public static SwordStatementDisseminator getStatementInstance(
            Map<Float, List<String>> accept)
            throws DSpaceSwordException, SwordError
    {
        SwordStatementDisseminator disseminator = null;

        // first try to load disseminators based on content type
        if (accept != null)
        {
            for (Float q : accept.keySet())
            {
                for (String format : accept.get(q))
                {
                    format = format.replace(";",
                            "_"); // clean up the string for the plugin manager
                    format = format.replace("=",
                            "_"); // clean up the string for the plugin manager
                    disseminator = (SwordStatementDisseminator) CoreServiceFactory.getInstance().getPluginService()
                            .getNamedPlugin(SwordStatementDisseminator.class, format);
                    if (disseminator != null)
                    {
                        break;
                    }
                }
            }
        }

        if (disseminator == null)
        {
            throw new SwordError(UriRegistry.ERROR_CONTENT, 406,
                    "No plugin can disseminate the requested formats");
        }

        return disseminator;
    }

    public static SwordEntryDisseminator getEntryInstance()
            throws DSpaceSwordException, SwordError
    {
        SwordEntryDisseminator disseminator = (SwordEntryDisseminator) CoreServiceFactory.getInstance().getPluginService()
                .getSinglePlugin(SwordEntryDisseminator.class);
        if (disseminator == null)
        {
            throw new SwordError(DSpaceUriRegistry.REPOSITORY_ERROR,
                    "No disseminator configured for handling sword entry documents");
        }
        return disseminator;
    }
}
