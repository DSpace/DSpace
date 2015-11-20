/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.swordapp.server.*;

import java.io.IOException;
import java.util.Date;

public class CollectionDepositManagerDSpace extends DSpaceSwordAPI
        implements CollectionDepositManager
{
    /**
     * logger
     */
    private static Logger log = Logger
            .getLogger(CollectionDepositManagerDSpace.class);

    protected CollectionService collectionService = ContentServiceFactory
            .getInstance().getCollectionService();

    private VerboseDescription verboseDescription = new VerboseDescription();

    public DepositReceipt createNew(String collectionUri, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription.append("Initialising verbose deposit");

        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

        try
        {
            // first authenticate the request
            // note: this will build our various DSpace contexts for us
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(
                        LogManager.getHeader(context, "sword_create_new", ""));
            }

            // get the deposit target
            Collection collection = this
                    .getDepositTarget(context, collectionUri, config);
            if (collection == null)
            {
                throw new SwordError(404);
            }

            // Ensure that this method is allowed
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.createResource(context, collection);

            // find out if the supplied SWORDContext can submit to the given
            // dspace object
            SwordAuthenticator auth = new SwordAuthenticator();
            if (!auth.canSubmit(sc, collection, this.verboseDescription))
            {
                // throw an exception if the deposit can't be made
                String oboEmail = "none";
                if (sc.getOnBehalfOf() != null)
                {
                    oboEmail = sc.getOnBehalfOf().getEmail();
                }
                log.info(LogManager
                        .getHeader(context, "deposit_failed_authorisation",
                                "user=" +
                                        sc.getAuthenticated().getEmail() +
                                        ",on_behalf_of=" + oboEmail));
                throw new SwordAuthException(
                        "Cannot submit to the given collection with this context");
            }

            // make a note of the authentication in the verbose string
            this.verboseDescription.append("Authenticated user: " +
                    sc.getAuthenticated().getEmail());
            if (sc.getOnBehalfOf() != null)
            {
                this.verboseDescription.append("Depositing on behalf of: " +
                        sc.getOnBehalfOf().getEmail());
            }

            DepositResult result = null;
            try
            {
                if (deposit.isBinaryOnly())
                {
                    result = this.createNewFromBinary(sc, collection, deposit,
                            authCredentials, config);
                }
                else if (deposit.isEntryOnly())
                {
                    result = this.createNewFromEntry(sc, collection, deposit,
                            authCredentials, config);
                }
                else if (deposit.isMultipart())
                {
                    result = this
                            .createNewFromMultipart(sc, collection, deposit,
                                    authCredentials, config);
                }
            }
            catch (DSpaceSwordException | SwordError e)
            {
                if (config.isKeepPackageOnFailedIngest())
                {
                    try
                    {
                        if (deposit.isBinaryOnly())
                        {
                            this.storePackageAsFile(deposit, authCredentials,
                                    config);
                        }
                        else if (deposit.isEntryOnly())
                        {
                            this.storeEntryAsFile(deposit, authCredentials,
                                    config);
                        }
                        else if (deposit.isMultipart())
                        {
                            this.storePackageAsFile(deposit, authCredentials,
                                    config);
                            this.storeEntryAsFile(deposit, authCredentials,
                                    config);
                        }
                    }
                    catch (IOException e2)
                    {
                        log.warn("Unable to store SWORD package as file: " + e);
                    }
                }
                throw e;
            }

            // now we've produced a deposit, we need to decide on its workflow state
            wfm.resolveState(context, deposit, result, this.verboseDescription);

            ReceiptGenerator genny = new ReceiptGenerator();
            DepositReceipt receipt = genny
                    .createReceipt(context, result, config);

            Date finish = new Date();
            long delta = finish.getTime() - start.getTime();

            this.verboseDescription
                    .append("Total time for deposit processing: " + delta +
                            " ms");
            this.addVerboseDescription(receipt, this.verboseDescription);

            // if something hasn't killed it already (allowed), then complete the transaction
            sc.commit();

            return receipt;
        }
        catch (DSpaceSwordException e)
        {
            log.error("caught exception:", e);
            throw new SwordServerException(
                    "There was a problem depositing the item", e);
        }
        finally
        {
            // this is a read operation only, so there's never any need to commit the context
            if (sc != null)
            {
                sc.abort();
            }
        }
    }

    protected DepositResult createNewFromBinary(SwordContext swordContext,
            Collection collection, Deposit deposit,
            AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();
        // is the content acceptable?  If not, this will throw an error
        this.isAcceptable(swordConfig, context, deposit, collection);

        // Obtain the relevant ingester from the factory
        SwordContentIngester si = SwordIngesterFactory
                .getContentInstance(context, deposit, collection);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        DepositResult result = si
                .ingest(context, deposit, collection, this.verboseDescription);
        this.verboseDescription.append("Archive ingest completed successfully");

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    protected DepositResult createNewFromEntry(SwordContext swordContext,
            Collection collection, Deposit deposit,
            AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();
        // Obtain the relevant ingester from the factory
        SwordEntryIngester si = SwordIngesterFactory
                .getEntryInstance(context, deposit, collection);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        DepositResult result = si
                .ingest(context, deposit, collection, this.verboseDescription);
        this.verboseDescription.append("Archive ingest completed successfully");

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    protected DepositResult createNewFromMultipart(SwordContext swordContext,
            Collection collection, Deposit deposit,
            AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // is the content acceptable?  If not, this will throw an error
        this.isAcceptable(swordConfig, context, deposit, collection);

        // Obtain the relevant content ingester from the factory
        SwordContentIngester sci = SwordIngesterFactory
                .getContentInstance(context, deposit, collection);
        this.verboseDescription
                .append("Loaded content ingester: " + sci.getClass().getName());

        // obtain the relevant entry intester from the factory
        SwordEntryIngester sei = SwordIngesterFactory
                .getEntryInstance(context, deposit, collection);
        this.verboseDescription
                .append("Loaded entry ingester: " + sei.getClass().getName());

        DepositResult result;
        if (swordConfig.isEntryFirst())
        {
            // do the entry deposit
            result = sei.ingest(context, deposit, collection,
                    this.verboseDescription);

            // do the content deposit
            result = sci.ingest(context, deposit, collection,
                    this.verboseDescription, result);
            this.verboseDescription
                    .append("Archive ingest completed successfully");
        }
        else
        {
            // do the content deposit
            result = sci.ingest(context, deposit, collection,
                    this.verboseDescription);

            // do the entry deposit
            result = sei.ingest(context, deposit, collection,
                    this.verboseDescription, result, false);
            this.verboseDescription
                    .append("Archive ingest completed successfully");
        }

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    protected Collection getDepositTarget(Context context, String depositUrl,
            SwordConfigurationDSpace config)
            throws DSpaceSwordException, SwordError
    {
        SwordUrlManager urlManager = config.getUrlManager(context, config);

        // get the target collection
        Collection collection = urlManager.getCollection(context, depositUrl);
        if (collection == null)
        {
            throw new SwordError(404);
        }

        this.verboseDescription
                .append("Performing deposit using deposit URL: " + depositUrl);

        this.verboseDescription
                .append("Location resolves to collection with handle: " +
                        collection.getHandle() +
                        " and name: " + collectionService.getName(collection));

        return collection;
    }
}
