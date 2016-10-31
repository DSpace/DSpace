/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import java.io.*;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDErrorException;

/**
 * This class is responsible for initiating the process of 
 * deposit of SWORD Deposit objects into the DSpace repository
 *
 * @author Richard Jones
 *
 */
public class DepositManager
{
    /** Log4j logger */
    public static final Logger log = Logger.getLogger(DepositManager.class);

    /** The SWORD service implementation */
    private SWORDService swordService;

    /**
     * Construct a new DepositManager using the given instantiation of
     * the SWORD service implementation
     *
     * @param service
     *     SWORD service
     */
    public DepositManager(SWORDService service)
    {
        this.swordService = service;
        log.debug("Created instance of DepositManager");
    }

    public DSpaceObject getDepositTarget(Deposit deposit)
            throws DSpaceSWORDException, SWORDErrorException
    {
        SWORDUrlManager urlManager = swordService.getUrlManager();
        Context context = swordService.getContext();

        // get the target collection
        String loc = deposit.getLocation();
        DSpaceObject dso = urlManager.getDSpaceObject(context, loc);

        swordService.message("Performing deposit using location: " + loc);

        if (dso instanceof Collection)
        {
            CollectionService collectionService = ContentServiceFactory
                .getInstance().getCollectionService();
            swordService.message(
                "Location resolves to collection with handle: " +
                dso.getHandle() +
                " and name: " +
                collectionService.getName((Collection) dso));
        }
        else if (dso instanceof Item)
        {
            swordService.message("Location resolves to item with handle: " +
                dso.getHandle());
        }

        return dso;
    }

    /**
     * Once this object is fully prepared, this method will execute
     * the deposit process.  The returned DepositRequest can be
     * used then to assemble the SWORD response.
     *
     * @param deposit
     *     deposit request
     * @return the response to the deposit request
     * @throws DSpaceSWORDException
     *     can be thrown by the internals of the DSpace SWORD implementation
     * @throws SWORDErrorException on generic SWORD exception
     * @throws SWORDAuthenticationException Thrown if the authentication fails
     */
    public DepositResponse deposit(Deposit deposit)
            throws DSpaceSWORDException, SWORDErrorException,
            SWORDAuthenticationException
    {
        // start the timer, and initialise the verboseness of the request
        Date start = new Date();
        swordService.message("Initialising verbose deposit");

        // get the things out of the service that we need
        SWORDContext swordContext = swordService.getSwordContext();
        Context context = swordService.getContext();

        // get the deposit target
        DSpaceObject dso = this.getDepositTarget(deposit);

        // find out if the supplied SWORDContext can submit to the given
        // dspace object
        SWORDAuthenticator auth = new SWORDAuthenticator();
        if (!auth.canSubmit(swordService, deposit, dso))
        {
            // throw an exception if the deposit can't be made
            String oboEmail = "none";
            if (swordContext.getOnBehalfOf() != null)
            {
                oboEmail = swordContext.getOnBehalfOf().getEmail();
            }
            log.info(LogManager.getHeader(context,
                "deposit_failed_authorisation",
                "user=" + swordContext.getAuthenticated().getEmail() +
                ",on_behalf_of=" + oboEmail));
            throw new SWORDAuthenticationException(
                "Cannot submit to the given collection with this context");
        }

        // make a note of the authentication in the verbose string
        swordService.message("Authenticated user: " +
            swordContext.getAuthenticated().getEmail());
        if (swordContext.getOnBehalfOf() != null)
        {
            swordService.message("Depositing on behalf of: " +
                swordContext.getOnBehalfOf().getEmail());
        }

        // determine which deposit engine we initialise
        Depositor dep = null;
        if (dso instanceof Collection)
        {
            swordService.message(
                "Initialising depositor for an Item in a Collection");
            dep = new CollectionDepositor(swordService, dso);
        }
        else if (dso instanceof Item)
        {
            swordService.message(
                "Initialising depositor for a Bitstream in an Item");
            dep = new ItemDepositor(swordService, dso);
        }

        if (dep == null)
        {
            log.error(
                "The specified deposit target does not exist, or is not a collection or an item");
            throw new DSpaceSWORDException(
                "Deposit target is not a collection or an item");
        }

        DepositResult result = null;

        try
        {
            result = dep.doDeposit(deposit);
        }
        catch (DSpaceSWORDException | SWORDErrorException | RuntimeException e)
        {
            if (swordService.getSwordConfig().isKeepPackageOnFailedIngest())
            {
                try
                {
                    storePackageAsFile(deposit);
                }
                catch (IOException e2)
                {
                    log.warn("Unable to store SWORD package as file: " + e);
                }
            }
            throw e;
        }

        // now construct the deposit response.  The response will be
        // CREATED if the deposit is in the archive, or ACCEPTED if
        // the deposit is in the workflow.  We use a separate record
        // for the handle because DSpace will not supply the Item with
        // a record of the handle straight away.
        String handle = result.getHandle();
        int state = Deposit.CREATED;
        if (StringUtils.isBlank(handle))
        {
            state = Deposit.ACCEPTED;
        }

        DepositResponse response = new DepositResponse(state);
        response.setLocation(result.getMediaLink());

        DSpaceATOMEntry dsatom = null;
        if (result.getItem() != null)
        {
            swordService.message("Initialising ATOM entry generator for an Item");
            dsatom = new ItemEntryGenerator(swordService);
        }
        else if (result.getBitstream() != null)
        {
            swordService.message(
                "Initialising ATOM entry generator for a Bitstream");
            dsatom = new BitstreamEntryGenerator(swordService);
        }
        if (dsatom == null)
        {
            log.error("The deposit failed, see exceptions for explanation");
            throw new DSpaceSWORDException(
                "Result of deposit did not yield an Item or a Bitstream");
        }
        SWORDEntry entry = dsatom.getSWORDEntry(result, deposit);

        // if this was a no-op, we need to remove the files we just
        // deposited, and abort the transaction
        if (deposit.isNoOp())
        {
            dep.undoDeposit(result);
            swordService.message(
                "NoOp Requested: Removed all traces of submission");
        }

        entry.setNoOp(deposit.isNoOp());

        Date finish = new Date();
        long delta = finish.getTime() - start.getTime();
        swordService.message(
            "Total time for deposit processing: " + delta + " ms");
        entry.setVerboseDescription(
            swordService.getVerboseDescription().toString());

        response.setEntry(entry);

        return response;
    }

    /**
     * Store original package on disk and companion file containing SWORD
     * headers as found in the deposit object
     * Also write companion file with header info from the deposit object.
     *
     * @param deposit        the original deposit request
     */
    private void storePackageAsFile(Deposit deposit) throws IOException
    {
        String path = swordService.getSwordConfig().getFailedPackageDir();

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory())
        {
            throw new IOException(
                "Directory does not exist for writing packages on ingest error.");
        }

        String filenameBase =
            "sword-" + deposit.getUsername() + "-" + (new Date()).getTime();

        File packageFile = new File(path, filenameBase);
        File headersFile = new File(path, filenameBase + "-headers");

        InputStream is = new BufferedInputStream(
            new FileInputStream(deposit.getFile()));
        OutputStream fos = new BufferedOutputStream(
            new FileOutputStream(packageFile));
        Utils.copy(is, fos);
        fos.close();
        is.close();

        //write companion file with headers
        PrintWriter pw = new PrintWriter(
            new BufferedWriter(new FileWriter(headersFile)));

        pw.println("Content-Disposition=" + deposit.getContentDisposition());
        pw.println("Content-Type=" + deposit.getContentType());
        pw.println("Packaging=" + deposit.getPackaging());
        pw.println("Location=" + deposit.getLocation());
        pw.println("On Behalf of=" + deposit.getOnBehalfOf());
        pw.println("Slug=" + deposit.getSlug());
        pw.println("User name=" + deposit.getUsername());
        pw.close();
    }
}
