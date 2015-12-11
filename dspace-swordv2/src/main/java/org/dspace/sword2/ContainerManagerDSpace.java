/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.swordapp.server.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ContainerManagerDSpace extends DSpaceSwordAPI
        implements ContainerManager
{
    private static Logger log = Logger.getLogger(ContainerManagerDSpace.class);

    protected AuthorizeService authorizeService = AuthorizeServiceFactory
            .getInstance().getAuthorizeService();

    protected WorkflowItemService workflowItemService = WorkflowServiceFactory
            .getInstance().getWorkflowItemService();

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
            .getInstance().getWorkspaceItemService();

    private VerboseDescription verboseDescription = new VerboseDescription();

    public boolean isStatementRequest(String editIRI,
            Map<String, String> accept, AuthCredentials authCredentials,
            SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        SwordContext sc = null;
        try
        {
            sc = this.noAuthContext();
            SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

            String acceptContentType = this.getHeader(accept, "Accept", null);
            TreeMap<Float, List<String>> analysed = this
                    .analyseAccept(acceptContentType);

            // a request is for a statement if the content negotiation asks for a format that the
            // Statement disseminator supports
            SwordStatementDisseminator disseminator = null;
            try
            {
                SwordDisseminatorFactory
                        .getStatementInstance(analysed);
            }
            catch (SwordError swordError)
            {
                // in this case, it means that no relevant disseminator could be found, which means
                // this is not a statement request
                return false;
            }
            return true;
        }
        catch (DSpaceSwordException e)
        {
            log.error("caught exception:", e);
            throw new SwordServerException(
                    "There was a problem determining the request type", e);
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

    public DepositReceipt getEntry(String editIRI, Map<String, String> accept,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordServerException, SwordError, SwordAuthException
    {
        SwordContext sc = null;
        try
        {
            SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();
            SwordUrlManager urlManager = config.getUrlManager(context, config);

            Item item = urlManager.getItem(context, editIRI);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // we can't give back an entry unless the user is authorised to retrieve it
            authorizeService.authorizeAction(context, item, Constants.READ);

            ReceiptGenerator genny = new ReceiptGenerator();
            DepositReceipt receipt = genny.createReceipt(context, item, config);
            sc.abort();
            return receipt;
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException();
        }
        catch (SQLException | DSpaceSwordException e)
        {
            throw new SwordServerException(e);
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

    public DepositReceipt replaceMetadata(String editIRI, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose replace of metadata");

        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

        try
        {
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "sword_replace", ""));
            }

            // get the deposit target
            Item item = this.getDSpaceTarget(context, editIRI, config);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // now we have the deposit target, we can determine whether this operation is allowed
            // at all
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.replaceMetadata(context, item);

            // find out if the supplied SWORDContext can submit to the given
            // dspace object
            SwordAuthenticator auth = new SwordAuthenticator();
            if (!auth.canSubmit(sc, item, this.verboseDescription))
            {
                // throw an exception if the deposit can't be made
                String oboEmail = "none";
                if (sc.getOnBehalfOf() != null)
                {
                    oboEmail = sc.getOnBehalfOf().getEmail();
                }
                log.info(LogManager
                        .getHeader(context, "replace_failed_authorisation",
                                "user=" +
                                        sc.getAuthenticated().getEmail() +
                                        ",on_behalf_of=" + oboEmail));
                throw new SwordAuthException(
                        "Cannot replace the given item with this context");
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
                result = this
                        .doReplaceMetadata(sc, item, deposit, authCredentials,
                                config);
            }
            catch (DSpaceSwordException | SwordError e)
            {
                if (config.isKeepPackageOnFailedIngest())
                {
                    try
                    {
                        this.storeEntryAsFile(deposit, authCredentials, config);
                    }
                    catch (IOException e2)
                    {
                        log.warn("Unable to store SWORD entry as file: " + e);
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

    public DepositReceipt replaceMetadataAndMediaResource(String editIRI,
            Deposit deposit, AuthCredentials authCredentials,
            SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose multipart replace");

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
            Item item = this.getDSpaceTarget(context, editIRI, config);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // Ensure that this method is allowed
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.replaceMetadataAndMediaResource(context, item);

            // find out if the supplied SWORDContext can submit to the given
            // dspace object
            SwordAuthenticator auth = new SwordAuthenticator();
            if (!auth.canSubmit(sc, item, this.verboseDescription))
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
                result = this.replaceFromMultipart(sc, item, deposit,
                        authCredentials, config);
            }
            catch (DSpaceSwordException | SwordError e)
            {
                if (config.isKeepPackageOnFailedIngest())
                {
                    try
                    {
                        this.storePackageAsFile(deposit, authCredentials,
                                config);
                        this.storeEntryAsFile(deposit, authCredentials, config);
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

    public DepositReceipt addMetadataAndResources(String s, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException
    {
        return null;
    }

    public DepositReceipt addMetadata(String editIRI, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose replace of metadata");

        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

        try
        {
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "sword_replace", ""));
            }

            // get the deposit target
            Item item = this.getDSpaceTarget(context, editIRI, config);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // now we have the deposit target, we can determine whether this operation is allowed
            // at all
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.addMetadata(context, item);

            // find out if the supplied SWORDContext can submit to the given
            // dspace object
            SwordAuthenticator auth = new SwordAuthenticator();
            if (!auth.canSubmit(sc, item, this.verboseDescription))
            {
                // throw an exception if the deposit can't be made
                String oboEmail = "none";
                if (sc.getOnBehalfOf() != null)
                {
                    oboEmail = sc.getOnBehalfOf().getEmail();
                }
                log.info(LogManager
                        .getHeader(context, "replace_failed_authorisation",
                                "user=" +
                                        sc.getAuthenticated().getEmail() +
                                        ",on_behalf_of=" + oboEmail));
                throw new SwordAuthException(
                        "Cannot replace the given item with this context");
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
                result = this.doAddMetadata(sc, item, deposit, authCredentials,
                        config);
            }
            catch (DSpaceSwordException | SwordError e)
            {
                if (config.isKeepPackageOnFailedIngest())
                {
                    try
                    {
                        this.storeEntryAsFile(deposit, authCredentials, config);
                    }
                    catch (IOException e2)
                    {
                        log.warn("Unable to store SWORD entry as file: " + e);
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

    public DepositReceipt addResources(String s, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration config)
            throws SwordError, SwordServerException
    {
        return null;
    }

    public void deleteContainer(String editIRI, AuthCredentials authCredentials,
            SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription.append("Initialising verbose container delete");

        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

        try
        {
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "sword_delete", ""));
            }

            // get the deposit target
            Item item = this.getDSpaceTarget(context, editIRI, config);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // now we have the deposit target, we can determine whether this operation is allowed
            // at all
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.deleteItem(context, item);

            // find out if the supplied SWORDContext can submit to the given
            // dspace object
            SwordAuthenticator auth = new SwordAuthenticator();
            if (!auth.canSubmit(sc, item, this.verboseDescription))
            {
                // throw an exception if the deposit can't be made
                String oboEmail = "none";
                if (sc.getOnBehalfOf() != null)
                {
                    oboEmail = sc.getOnBehalfOf().getEmail();
                }
                log.info(LogManager
                        .getHeader(context, "replace_failed_authorisation",
                                "user=" +
                                        sc.getAuthenticated().getEmail() +
                                        ",on_behalf_of=" + oboEmail));
                throw new SwordAuthException(
                        "Cannot delete the given item with this context");
            }

            // make a note of the authentication in the verbose string
            this.verboseDescription.append("Authenticated user: " +
                    sc.getAuthenticated().getEmail());
            if (sc.getOnBehalfOf() != null)
            {
                this.verboseDescription.append("Depositing on behalf of: " +
                        sc.getOnBehalfOf().getEmail());
            }

            this.doContainerDelete(sc, item, authCredentials, config);

            Date finish = new Date();
            long delta = finish.getTime() - start.getTime();

            this.verboseDescription
                    .append("Total time for deposit processing: " + delta +
                            " ms");

            // if something hasn't killed it already (allowed), then complete the transaction
            sc.commit();
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

    public DepositReceipt useHeaders(String editIRI, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose empty request (headers only)");

        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

        try
        {
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager
                        .getHeader(context, "sword_modify_by_headers", ""));
            }

            // get the deposit target
            Item item = this.getDSpaceTarget(context, editIRI, config);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // now we have the deposit target, we can determine whether this operation is allowed
            // at all
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.modifyState(context, item);

            // find out if the supplied SWORDContext can submit to the given
            // dspace object
            SwordAuthenticator auth = new SwordAuthenticator();
            if (!auth.canSubmit(sc, item, this.verboseDescription))
            {
                // throw an exception if the deposit can't be made
                String oboEmail = "none";
                if (sc.getOnBehalfOf() != null)
                {
                    oboEmail = sc.getOnBehalfOf().getEmail();
                }
                log.info(LogManager
                        .getHeader(context, "modify_failed_authorisation",
                                "user=" +
                                        sc.getAuthenticated().getEmail() +
                                        ",on_behalf_of=" + oboEmail));
                throw new SwordAuthException(
                        "Cannot modify the given item with this context");
            }

            // make a note of the authentication in the verbose string
            this.verboseDescription.append("Authenticated user: " +
                    sc.getAuthenticated().getEmail());
            if (sc.getOnBehalfOf() != null)
            {
                this.verboseDescription.append("Modifying on behalf of: " +
                        sc.getOnBehalfOf().getEmail());
            }

            DepositResult result = new DepositResult();
            result.setItem(item);

            // the main objective here is just to resolve the state
            wfm.resolveState(context, deposit, result, this.verboseDescription);

            // now return the usual deposit receipt
            ReceiptGenerator genny = new ReceiptGenerator();
            DepositReceipt receipt = genny.createReceipt(context, item, config);

            Date finish = new Date();
            long delta = finish.getTime() - start.getTime();

            this.verboseDescription
                    .append("Total time for modify processing: " + delta +
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

    private DepositResult replaceFromMultipart(SwordContext swordContext,
            Item item, Deposit deposit, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // is the content acceptable?  If not, this will throw an error
        this.isAcceptable(swordConfig, context, deposit, item);

        // Obtain the relevant content ingester from the factory
        SwordContentIngester sci = SwordIngesterFactory
                .getContentInstance(context, deposit, item);
        this.verboseDescription
                .append("Loaded content ingester: " + sci.getClass().getName());

        // obtain the relevant entry intester from the factory
        SwordEntryIngester sei = SwordIngesterFactory
                .getEntryInstance(context, deposit, item);
        this.verboseDescription
                .append("Loaded entry ingester: " + sei.getClass().getName());

        try
        {
            // delegate the to the version manager to get rid of any existing content and to version
            // if if necessary
            VersionManager vm = new VersionManager();
            vm.removeBundle(context, item, "ORIGINAL");
        }
        catch (SQLException | IOException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }

        DepositResult result;
        if (swordConfig.isEntryFirst())
        {
            // do the entry deposit
            result = sei.ingest(context, deposit, item, this.verboseDescription,
                    null, true);

            // do the content deposit
            result = sci.ingest(context, deposit, item, this.verboseDescription,
                    result);
            this.verboseDescription
                    .append("Archive ingest completed successfully");
        }
        else
        {
            // do the content deposit
            result = sci.ingest(context, deposit, item, this.verboseDescription,
                    null);

            // do the entry deposit
            result = sei.ingest(context, deposit, item, this.verboseDescription,
                    result, true);
            this.verboseDescription
                    .append("Archive ingest completed successfully");
        }

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    private DepositResult doReplaceMetadata(SwordContext swordContext,
            Item item, Deposit deposit, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // Obtain the relevant ingester from the factory
        SwordEntryIngester si = SwordIngesterFactory
                .getEntryInstance(context, deposit, null);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        DepositResult result = si
                .ingest(context, deposit, item, this.verboseDescription, null,
                        true);
        this.verboseDescription.append("Replace completed successfully");

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    protected DepositResult doAddMetadata(SwordContext swordContext, Item item,
            Deposit deposit, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        return this.doAddMetadata(swordContext, item, deposit, authCredentials,
                swordConfig, null);
    }

    protected DepositResult doAddMetadata(SwordContext swordContext, Item item,
            Deposit deposit, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig, DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        if (result == null)
        {
            result = new DepositResult();
        }

        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // Obtain the relevant ingester from the factory
        SwordEntryIngester si = SwordIngesterFactory
                .getEntryInstance(context, deposit, null);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        result = si
                .ingest(context, deposit, item, this.verboseDescription, result,
                        false);
        this.verboseDescription.append("Replace completed successfully");

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    protected void doContainerDelete(SwordContext swordContext, Item item,
            AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordAuthException
    {
        try
        {
            Context context = swordContext.getContext();

            // first figure out if there's anything we need to do about the workflow/workspace state
            WorkflowTools wft = new WorkflowTools();
            if (wft.isItemInWorkspace(swordContext.getContext(), item))
            {
                WorkspaceItem wsi = wft.getWorkspaceItem(context, item);
                workspaceItemService.deleteAll(context, wsi);
            }
            else if (wft.isItemInWorkflow(context, item))
            {
                WorkflowItem wfi = wft.getWorkflowItem(context, item);
                workflowItemService.deleteWrapper(context, wfi);
            }

            // then delete the item
            itemService.delete(context, item);
        }
        catch (SQLException | IOException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
    }

    private Item getDSpaceTarget(Context context, String editUrl,
            SwordConfigurationDSpace config)
            throws DSpaceSwordException, SwordError
    {
        SwordUrlManager urlManager = config.getUrlManager(context, config);

        // get the target collection
        Item item = urlManager.getItem(context, editUrl);
        if (item == null)
        {
            throw new SwordError(404);
        }

        this.verboseDescription
                .append("Performing replace using edit-media URL: " + editUrl);
        this.verboseDescription
                .append("Location resolves to item with handle: " +
                        item.getHandle());

        return item;
    }
}
