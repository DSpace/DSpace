/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.curation;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

import cz.cuni.mff.ufal.AllCertsTrustManager;

/**
 * A basic link checker that is designed to be extended. By default this link
 * checker will check that all links stored in anyschema.anyelement.uri metadata
 * fields return a 20x status code.
 *
 * This link checker can be enhanced by extending this class, and overriding the
 * getURLs and checkURL methods.
 *
 * based on class by Stuart Lewis modified for LINDAT/CLARIN
 */

public class LinkChecker extends AbstractCurationTask
{

    protected static final String LR_CONFIG_MODULE = "lr";

    protected static final String LINK_CHECKER_USER_AGENT_KEY = "lr.link.checker.user.agent";
    protected static final String LINK_CHECKER_USER_AGENT = "DSpace Link Validator";
    protected String cachedUserAgent;

    protected static final String LINK_CHECKER_CONNECT_TIMEOUT_KEY = "lr.link.checker.connect.timeout";
    protected static final int LINK_CHECKER_CONNECT_TIMEOUT = 2000;
    protected Integer cachedConnectTimeout;

    protected static final String LINK_CHECKER_READ_TIMEOUT_KEY = "lr.link.checker.read.timeout";
    protected static final int LINK_CHECKER_READ_TIMEOUT = 3000;
    protected Integer cachedReadTimeout;

    // The status of the link checking of this item
    protected int status = Curator.CURATE_UNSET;

    // The results of link checking this item
    private List<String> results = null;

    // The log4j logger for this class
    private static Logger log = Logger.getLogger(LinkChecker.class);

    // already visited urls in this run
    protected Map<String, ResponseStatus> checked_results = null;

    protected String[] ignore_urls = null;

    protected int errors = 0;

    @Override
    public void init(Curator curator, String taskId) throws IOException
    {
        super.init(curator, taskId);
        checked_results = new HashMap<String, ResponseStatus>();
        errors = 0;
        // any ignoreable urls?
        String ignores = ConfigurationManager.getProperty("curate",
                "checklinks.ignore");
        if (null != ignores && 0 < ignores.length())
        {
            ignore_urls = ignores.split(",");
        }
    }

    /**
     * Helper class for HTTP Response information
     *
     * @author Michal Jos√≠fko
     *
     */
    protected class ResponseStatus
    {

        int code;

        int redirectionCode;

        String redirectionURL;

        public ResponseStatus(int code)
        {
            setCode(code);
        }

        public ResponseStatus(ResponseStatus responseStatus)
        {
            setCode(responseStatus.getCode());
            setRedirectionCode(responseStatus.getRedirectionCode());
            setRedirectionURL(responseStatus.getRedirectionURL());
        }

        public int getCode()
        {
            return code;
        }

        public void setCode(int code)
        {
            this.code = code;
        }

        public int getRedirectionCode()
        {
            return redirectionCode;
        }

        public void setRedirectionCode(int redirectionCode)
        {
            this.redirectionCode = redirectionCode;
        }

        public String getRedirectionURL()
        {
            return redirectionURL;
        }

        public void setRedirectionURL(String redirectionURL)
        {
            this.redirectionURL = redirectionURL;
        }

        public boolean isRedirection()
        {
            return (code >= 300 && code < 400);
        }

    }

    /**
     * Perform the link checking.
     *
     * @param dso
     *            The DSpaaceObject to be checked
     * @return The curation task status of the checking
     * @throws java.io.IOException
     *             THrown if something went wrong
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
        // should we continue?
        if (!canContinue())
        {
            report(null);
            setResult(null);
            return Curator.CURATE_SKIP;
        }

        // The results that we'll return
        StringBuilder results = new StringBuilder();

        // Unless this is an item, we'll skip this item
        status = Curator.CURATE_SKIP;
        if (dso instanceof Item)
        {
            Item item = (Item) dso;

            // Get the URLs
            List<String> urls = getURLs(item);

            // Assume skip until we hit a URL to check
            status = Curator.CURATE_FAIL;
            results.append("Item: ")
                    .append(getItemHandle(item))
                    .append(" ")
                    .append(getFormattedEditItemURL(item))
                    .append(" has " + String.valueOf(urls.size())
                            + " urls to check...\n");

            if (0 < urls.size())
            {
                boolean all_ok = true;
                // Check the URLs

                long startTime;
                long endTime;

                for (String url : urls)
                {
                    startTime = System.nanoTime();
                    boolean ok = checkURL(url, results);
                    endTime = System.nanoTime();
                    log.info(String.format(
                            "URL [%s] checked in [%d] nanoseconds", url,
                            endTime - startTime));
                    if (!ok)
                    {
                        all_ok = false;
                    }
                }
                if (all_ok)
                {
                    status = Curator.CURATE_SUCCESS;
                }
                else
                {
                    errors++;
                }

            }
            else
            {
                status = Curator.CURATE_SKIP;
            }
        }

        report(results.toString());
        setResult(results.toString());

        return status;
    }

    /**
     * Get the URLs to check
     *
     * @param item
     *            The item to extract URLs from
     * @return An array of URL Strings
     */
    protected List<String> getURLs(Item item)
    {
        // Get URIs from anyschema.anyelement.uri.*
        Metadatum[] urls = item.getMetadata(Item.ANY, Item.ANY, "uri", Item.ANY);
        ArrayList<String> theURLs = new ArrayList<String>();
        for (Metadatum url : urls)
        {
            theURLs.add(url.value);
        }
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
            checked_results.put(url, responseStatus);
        }

        return processResult(url, responseStatus, results);
    }

    /**
     * Checks if given URL should be ignored
     *
     * @param url
     *            URL to be checked
     * @return True if url should be ignored
     */
    protected boolean isIgnoredURL(String url)
    {
        boolean res = false;

        if (null != ignore_urls)
        {
            for (String s : ignore_urls)
            {
                if (url.contains(s))
                {
                    res = true;
                    break;
                }
            }
        }

        return res;
    }

    /**
     * Analyses the result of checking URL
     *
     * @param url
     *            URL that was checked
     * @param responseStatus
     *            Result of checking URL
     * @return True is the URL is available, false otherwise
     */
    protected boolean processResult(String url, ResponseStatus responseStatus,
            StringBuilder results)
    {
        String level;
        boolean res;

        int httpStatus = responseStatus.isRedirection() ? responseStatus
                .getRedirectionCode() : responseStatus.getCode();

        if (((httpStatus >= 200) && (httpStatus < 300)) || httpStatus == 401)
        {
            level = "OK";
            res = true;
        }
        else if (httpStatus == -1)
        {
            level = "IGNORED";
            res = false;
        }
        else if (httpStatus == -2)
        {
            level = "TIMEOUT";
            res = false;
        }
        else if (httpStatus == -3)
        {
            level = "REDIRECTION LOOP";
            res = false;
        }
        else
        {
            level = "FAILED";
            res = false;
        }

        results.append(formatResult(level, url, responseStatus));
        return res;
    }

    /**
     * Formats results of checking URL
     *
     * @param level
     *            Error level
     * @param url
     *            URL that was checked
     * @param responseStatus
     *            Result of checking URL
     * @return
     */
    protected String formatResult(String level, String url,
            ResponseStatus responseStatus)
    {
        String res;
        if (responseStatus.isRedirection())
        {
            res = String.format(" - %s = %d redirected to %s = %d - %s\n", url,
                    responseStatus.getCode(),
                    responseStatus.getRedirectionURL(),
                    responseStatus.getRedirectionCode(), level);
        }
        else
        {
            res = String.format(" - %s = %d - %s\n", url,
                    responseStatus.getCode(), level);
        }
        return res;
    }

    /**
     * Get the response status for a URL with redirection following and special
     * handling of handle URLs. Possible response codes are: 0 = error, -2 =
     * timeout, -3 = redirection loop, HTTP code otherwise.
     *
     * @param url
     *            The url to open
     * @return Response status wrapping first response code, last redirection
     *         url, and last response code
     *
     */

    protected ResponseStatus getResponseStatus(String url)
    {
        Set<String> processedURLs = new HashSet<String>();
        boolean isHandle = isHandleURL(url);

        ResponseStatus responseStatus = getRealResponseStatus(url);
        processedURLs.add(url);
        ResponseStatus redirectionResponseStatus = new ResponseStatus(
                responseStatus);

        // loop over possible chain of redirections
        while (redirectionResponseStatus.isRedirection())
        {
            String redirected_to_url = redirectionResponseStatus.getRedirectionURL();
            try {
                final URI u = new URI(redirected_to_url);
                if ( !u.isAbsolute() ) {
                    redirected_to_url = new URL( new URL(url),
                        redirected_to_url ).toExternalForm();
                }
            } catch (URISyntaxException e) {
            } catch (MalformedURLException e) {
            }


            responseStatus.setRedirectionURL(redirected_to_url);

            if (processedURLs.contains(redirectionResponseStatus
                    .getRedirectionURL()))
            {
                responseStatus.setCode(-3);
                break;
            }

            redirectionResponseStatus = getRealResponseStatus(redirected_to_url);
            processedURLs.add(redirected_to_url);

            if (isHandle)
            {
                // for handles overwrite the response code with the last
                // code in redirection chain
                responseStatus.setCode(redirectionResponseStatus.getCode());
            }
            else
            {
                responseStatus.setRedirectionCode(redirectionResponseStatus
                        .getCode());
            }
        }

        return responseStatus;
    }

    /**
     * Get the response status for a URL. Redirection is not handled. Possible
     * response codes are: 0 = error, -2 = timeout, -3 = redirection loop, HTTP
     * code otherwise.
     *
     * @param url
     *            The url to open
     * @return Response status wrapping first response code, and redirection URL
     *         if the response code is 3xx
     *
     */

    protected ResponseStatus getRealResponseStatus(String url)
    {
        ResponseStatus responseStatus = new ResponseStatus(0);

        try
        {
            URL theURL = new URL(url);

            // prepare HTTP connection
            HttpURLConnection connection = (HttpURLConnection) theURL
                    .openConnection();
            connection
                    .setRequestProperty("User-Agent", getUserAgent());
            connection.setConnectTimeout(getConnectTimeout());
            connection.setReadTimeout(getReadTimeout());
            connection.setInstanceFollowRedirects(false);

            // handle HTTPS
            if (theURL.getProtocol().equals("https"))
            {
                HttpsURLConnection
                        .setDefaultHostnameVerifier(AllCertsTrustManager
                                .getHostNHostnameVerifier());
                ((HttpsURLConnection) connection)
                        .setSSLSocketFactory(AllCertsTrustManager
                                .getSocketFactory());
            }

            // get response code
            int code = connection.getResponseCode();

            responseStatus.setCode(code);
            if (responseStatus.isRedirection())
            {
                responseStatus.setRedirectionURL(connection
                        .getHeaderField("Location"));
            }

            connection.disconnect();

            return responseStatus;
        }
        catch (SocketTimeoutException ste)
        {
            // Must be a bad URL
            log.error(String.format("Timeout while checking link [%s]: [%s]",
                    url, ste.getMessage()));
            responseStatus.setCode(-2);
            return responseStatus;
        }
        catch (IOException ioe)
        {
            // Must be a bad URL
            log.error(String.format("Bad link [%s]: [%s]", url,
                    ioe.getMessage()));
            responseStatus.setCode(0);
            return responseStatus;
        }
    }

    /**
     * Internal utitity method to get a formatted URL of the item in local
     * repository
     *
     * @param item
     *            The item to get a URL of
     * @return The URL of the item
     */
    protected static String getFormattedEditItemURL(Item item)
    {
        return String.format(" [%s/admin/item?itemID=%d]",
                ConfigurationManager.getProperty("dspace.url"), item.getID());
    }

    /**
     * Internal utitity method to get a description of the handle
     *
     * @param item
     *            The item to get a description of
     * @return The handle, or in workflow
     */
    protected static String getItemHandle(Item item)
    {
        String handle = item.getHandle();
        return (handle != null) ? handle : " in workflow";
    }

    /**
     * Checks whether the given URL is Handle System URL
     *
     * @param url
     *            URL to check
     * @return True if the given URL is a Handle System URL
     */
    protected boolean isHandleURL(String url)
    {
        return url.startsWith("http://hdl.handle.net/");
    }

    /**
     * Checks whether we can continue performing tasks
     *
     * @return False if no more checking should be done
     */
    protected boolean canContinue()
    {
        return true;
    }

    protected String getUserAgent()
    {
        if(cachedUserAgent == null)
        {
            cachedUserAgent = ConfigurationManager.getProperty(LR_CONFIG_MODULE, LINK_CHECKER_USER_AGENT_KEY);
            if(cachedUserAgent == null)
            {
                cachedUserAgent = LINK_CHECKER_USER_AGENT;
            }

        }
        return cachedUserAgent;
    }

    protected int getConnectTimeout()
    {
        if(cachedReadTimeout == null)
        {
            cachedReadTimeout = ConfigurationManager.getIntProperty(LR_CONFIG_MODULE, LINK_CHECKER_CONNECT_TIMEOUT_KEY, LINK_CHECKER_CONNECT_TIMEOUT);
        }
        return cachedReadTimeout;
    }

    protected int getReadTimeout()
    {
        if(cachedReadTimeout == null)
        {
            cachedReadTimeout = ConfigurationManager.getIntProperty(LR_CONFIG_MODULE, LINK_CHECKER_CONNECT_TIMEOUT_KEY, LINK_CHECKER_CONNECT_TIMEOUT);
        }
        return cachedReadTimeout;
    }

}

