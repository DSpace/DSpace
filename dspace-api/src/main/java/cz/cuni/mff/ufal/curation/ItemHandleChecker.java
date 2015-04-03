/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import cz.cuni.mff.ufal.curation.LinkChecker;
import org.dspace.curate.Curator;

import cz.cuni.mff.ufal.DSpaceApi;

@SuppressWarnings("deprecation")
public class ItemHandleChecker extends LinkChecker
{

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(Curator.class);

    private Item currentItem = null;

    @Override
    protected List<String> getURLs(Item item)
    {
        // get the handle URL associated with Item
        Metadatum[] handles = item.getMetadata("dc", "identifier", "uri",
                Item.ANY);
        ArrayList<String> theURLs = new ArrayList<String>();
        for (Metadatum url : handles)
        {
            theURLs.add(url.value);
        }
        currentItem = item;
        return theURLs;
    }

    /**
     * Check the URL and perform appropriate reporting
     * 
     * @param url
     *            The URL to check
     * @return If the URL was OK or not
     */
    protected boolean checkURL(String url, StringBuilder results)
    {
        // Link check the URL
        ResponseStatus responseStatus = new ResponseStatus(0);

        // should we ignore it
        if (isIgnoredURL(url))
        {
            responseStatus.setCode(-1);
            checked_results.put(url, responseStatus);
        }

        // have we already processed it
        if (checked_results.containsKey(url))
        {
            responseStatus = checked_results.get(url);
        }
        else
        {
            responseStatus = getResponseStatus(url);
            responseStatus = fixHandleURL(url, responseStatus);
            checked_results.put(url, responseStatus);
        }

        return processResult(url, responseStatus, results);
    }

    /**
     * Tries to fix handle URL if needed and allowed
     * 
     * @param url
     * @param responseStatus
     * @return
     */
    protected ResponseStatus fixHandleURL(String url,
            ResponseStatus responseStatus)
    {
        ResponseStatus newResponseStatus = new ResponseStatus(responseStatus);
        try
        {
            if (responseStatus.isRedirection())
            {

                String handle = currentItem.getHandle();

                if (handle == null)
                {
                    throw new RuntimeException("Item handle not found");
                }

                if (isHandleURL(url))
                {
                    if (url.endsWith(handle))
                    {
                        // if handle URL has proper handle
                        if (!responseStatus.getRedirectionURL()
                                .endsWith(handle))
                        {
                            // if the redirection URL is wrong try to correct it
                            // only attempt correction if pid url ends with
                            // handle but redirection url does not
                            if (isCorrectionAllowed())
                            {
                                correctHandle(handle);
                                newResponseStatus = super
                                        .getResponseStatus(url);
                            }
                            else
                            {
                                throw new RuntimeException(
                                        "Correction needed for invalid redirection : "
                                                + url
                                                + " to "
                                                + responseStatus
                                                        .getRedirectionURL());
                            }
                        }

                    }
                    else
                    {
                        throw new RuntimeException(
                                "Invalid dc.identifier.uri : " + url
                                        + " not pointing to handle " + handle);
                    }
                }
                else
                {
                    throw new RuntimeException(
                            "Invalid format of dc.identifier.uri : " + url);
                }
            }
        }
        catch (IOException ioe)
        {
            // Must be a bad URL
            log.error("Bad link: " + ioe.getMessage());
            newResponseStatus.setCode(0);
            return newResponseStatus;
        }
        catch (RuntimeException re)
        {
            // Must be a bad URL
            log.error(String.format("Error while checking link [%s]: [%s]",
                    url, re.getMessage()));
            newResponseStatus.setCode(0);
            return newResponseStatus;
        }

        return newResponseStatus;

    }

    /**
     * Checks whether correction of handles during checking dc.identifier.uri is
     * allowed
     * 
     * @return
     */
    private boolean isCorrectionAllowed()
    {
        boolean res = false;
        String doCorrection = ConfigurationManager.getProperty("lr",
                "lr.curation.handle.correction");
        res = Boolean.parseBoolean(doCorrection);
        return res;
    }

    /**
     * Performs registration of the current item URL in handle system
     * 
     * @param handle
     *            Handle of the item
     */
    private void correctHandle(String handle) throws IOException
    {
        log.info("Trying to correct PID " + handle);
        DSpaceApi.handle_HandleManager_registerFinalHandleURL(log, handle);
    }

}
