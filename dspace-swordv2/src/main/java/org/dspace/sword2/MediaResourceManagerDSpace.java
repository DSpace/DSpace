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
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.swordapp.server.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

public class MediaResourceManagerDSpace extends DSpaceSwordAPI
        implements MediaResourceManager
{
    private static Logger log = Logger
            .getLogger(MediaResourceManagerDSpace.class);

    protected AuthorizeService authorizeService = AuthorizeServiceFactory
            .getInstance().getAuthorizeService();

    private VerboseDescription verboseDescription = new VerboseDescription();

    private boolean isAccessible(Context context, Bitstream bitstream)
            throws DSpaceSwordException
    {
        try
        {
            return authorizeService
                    .authorizeActionBoolean(context, bitstream, Constants.READ);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private boolean isAccessible(Context context, Item item)
            throws DSpaceSwordException
    {
        try
        {
            return authorizeService
                    .authorizeActionBoolean(context, item, Constants.READ);
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
    }

    private MediaResource getBitstreamResource(Context context,
            Bitstream bitstream)
            throws SwordServerException, SwordAuthException
    {
        try
        {
            InputStream stream = bitstreamService.retrieve(context, bitstream);
            MediaResource mr = new MediaResource(stream,
                    bitstream.getFormat(context).getMIMEType(), null, true);
            mr.setContentMD5(bitstream.getChecksum());
            mr.setLastModified(this.getLastModified(context, bitstream));
            return mr;
        }
        catch (IOException | SQLException e)
        {
            throw new SwordServerException(e);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
    }

    private MediaResource getItemResource(Context context, Item item,
            SwordUrlManager urlManager, String uri, Map<String, String> accept)
            throws SwordError, DSpaceSwordException, SwordServerException
    {
        boolean feedRequest = urlManager.isFeedRequest(context, uri);
        SwordContentDisseminator disseminator = null;

        // first off, consider the accept headers.  The accept argument is a map
        // from accept header to value.
        // we only care about Accept and Accept-Packaging
        if (!feedRequest)
        {
            String acceptContentType = this.getHeader(accept, "Accept", null);
            String acceptPackaging = this.getHeader(accept, "Accept-Packaging",
                    UriRegistry.PACKAGE_SIMPLE_ZIP);

            // we know that only one Accept-Packaging value is allowed, so we don't need
            // to do any further work on it.

            // we extract from the Accept header the ordered list of content types
            TreeMap<Float, List<String>> analysed = this
                    .analyseAccept(acceptContentType);

            // the meat of this is done by the package disseminator
            disseminator = SwordDisseminatorFactory
                    .getContentInstance(analysed, acceptPackaging);
        }
        else
        {
            // we just want to ask for the atom version, so we bypass the main content
            // negotiation place
            Map<Float, List<String>> analysed = new HashMap<Float, List<String>>();
            List<String> list = new ArrayList<String>();
            list.add("application/atom+xml");
            analysed.put((float) 1.0, list);
            disseminator = SwordDisseminatorFactory
                    .getContentInstance(analysed, null);
        }

        // Note that at this stage, if we don't have a desiredContentType, it will
        // be null, and the disseminator is free to choose the format
        InputStream stream = disseminator.disseminate(context, item);
        return new MediaResource(stream, disseminator.getContentType(),
                disseminator.getPackaging());
    }

    public MediaResource getMediaResourceRepresentation(String uri,
            Map<String, String> accept, AuthCredentials authCredentials,
            SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // all the bits we need to make this method function
        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;
        Context ctx = null;

        try
        {
            // create an unauthenticated context for our initial explorations
            ctx = new Context();
            SwordUrlManager urlManager = config.getUrlManager(ctx, config);

            // is this a request for a bitstream or an item (which is the full media resource)?
            if (urlManager.isActionableBitstreamUrl(ctx, uri))
            {
                // request for a bitstream
                Bitstream bitstream = urlManager.getBitstream(ctx, uri);
                if (bitstream == null)
                {
                    // bitstream not found in the database, so 404 the client.
                    // Arguably, we should try to authenticate first, but it's not so important
                    throw new SwordError(404);
                }

                // find out, now we know what we're being asked for, whether this is allowed
                WorkflowManagerFactory.getInstance()
                        .retrieveBitstream(ctx, bitstream);

                // we can do this in principle, but now find out whether the bitstream is accessible without credentials
                boolean accessible = this.isAccessible(ctx, bitstream);

                if (!accessible)
                {
                    // try to authenticate, and if successful switch the contexts around
                    sc = this.doAuth(authCredentials);
                    ctx.abort();
                    ctx = sc.getContext();

                    // re-retrieve the bitstream using the new context
                    bitstream = bitstreamService.find(ctx, bitstream.getID());

                    // and re-verify its accessibility
                    accessible = this.isAccessible(ctx, bitstream);
                    if (!accessible)
                    {
                        throw new SwordAuthException();
                    }
                }

                // if we get to here we are either allowed to access the bitstream without credentials,
                // or we have been authenticated with acceptable credentials
                MediaResource mr = this.getBitstreamResource(ctx, bitstream);
                if (sc != null)
                {
                    sc.abort();
                }
                if (ctx.isValid())
                {
                    ctx.abort();
                }
                return mr;
            }
            else
            {
                // request for an item
                Item item = urlManager.getItem(ctx, uri);
                if (item == null)
                {
                    // item now found in the database, so 404 the client
                    // Arguably, we should try to authenticate first, but it's not so important
                    throw new SwordError(404);
                }

                // find out, now we know what we're being asked for, whether this is allowed
                WorkflowManagerFactory.getInstance().retrieveContent(ctx, item);

                // we can do this in principle but now find out whether the item is accessible without credentials
                boolean accessible = this.isAccessible(ctx, item);

                if (!accessible)
                {
                    // try to authenticate, and if successful switch the contexts around
                    sc = this.doAuth(authCredentials);
                    ctx.abort();
                    ctx = sc.getContext();
                }

                // if we get to here we are either allowed to access the bitstream without credentials,
                // or we have been authenticated
                MediaResource mr = this
                        .getItemResource(ctx, item, urlManager, uri, accept);
                // sc.abort();
                ctx.abort();
                return mr;
            }
        }
        catch (SQLException | DSpaceSwordException e)
        {
            throw new SwordServerException(e);
        }
        finally
        {
            // if there is a sword context, abort it (this will abort the inner dspace context as well)
            if (sc != null)
            {
                sc.abort();
            }
            if (ctx != null && ctx.isValid())
            {
                ctx.abort();
            }
        }
    }

    private Date getLastModified(Context context, Bitstream bitstream)
            throws SQLException
    {
        Date lm = null;
        List<Bundle> bundles = bitstream.getBundles();
        for (Bundle bundle : bundles)
        {
            List<Item> items = bundle.getItems();
            for (Item item : items)
            {
                Date possible = item.getLastModified();
                if (lm == null)
                {
                    lm = possible;
                }
                else if (possible.getTime() > lm.getTime())
                {
                    lm = possible;
                }
            }
        }
        if (lm == null)
        {
            return new Date();
        }
        return lm;
    }

    public DepositReceipt replaceMediaResource(String emUri, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose replace of media resource");

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

            DepositReceipt receipt;
            SwordUrlManager urlManager = config.getUrlManager(context, config);
            if (urlManager.isActionableBitstreamUrl(context, emUri))
            {
                Bitstream bitstream = urlManager.getBitstream(context, emUri);
                if (bitstream == null)
                {
                    throw new SwordError(404);
                }

                // now we have the deposit target, we can determine whether this operation is allowed
                // at all
                WorkflowManager wfm = WorkflowManagerFactory.getInstance();
                wfm.replaceBitstream(context, bitstream);

                // check that we can submit to ALL the items this bitstream is in
                List<Item> items = new ArrayList<>();
                List<Bundle> bundles = bitstream.getBundles();
                for (Bundle bundle : bundles)
                {
                    List<Item> bundleItems = bundle
                            .getItems();
                    for (Item item : bundleItems)
                    {
                        this.checkAuth(sc, item);
                        items.add(item);
                    }
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
                            .replaceBitstream(sc, items, bitstream, deposit,
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
                        }
                        catch (IOException e2)
                        {
                            log.warn("Unable to store SWORD package as file: " +
                                    e);
                        }
                    }
                    throw e;
                }

                // now we've produced a deposit, we need to decide on its workflow state
                wfm.resolveState(context, deposit, null,
                        this.verboseDescription, false);

                ReceiptGenerator genny = new ReceiptGenerator();
                receipt = genny.createFileReceipt(context, result, config);
            }
            else
            {
                // get the deposit target
                Item item = this.getDSpaceTarget(context, emUri, config);
                if (item == null)
                {
                    throw new SwordError(404);
                }

                // now we have the deposit target, we can determine whether this operation is allowed
                // at all
                WorkflowManager wfm = WorkflowManagerFactory.getInstance();
                wfm.replaceResourceContent(context, item);

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

                try
                {
                    this.replaceContent(sc, item, deposit, authCredentials,
                            config);
                }
                catch (DSpaceSwordException | SwordError e)
                {
                    if (config.isKeepPackageOnFailedIngest())
                    {
                        try
                        {
                            this.storePackageAsFile(deposit, authCredentials,
                                    config);
                        }
                        catch (IOException e2)
                        {
                            log.warn("Unable to store SWORD package as file: " +
                                    e);
                        }
                    }
                    throw e;
                }

                // now we've produced a deposit, we need to decide on its workflow state
                wfm.resolveState(context, deposit, null,
                        this.verboseDescription, false);

                ReceiptGenerator genny = new ReceiptGenerator();
                receipt = genny
                        .createMediaResourceReceipt(context, item, config);
            }

            Date finish = new Date();
            long delta = finish.getTime() - start.getTime();

            this.verboseDescription
                    .append("Total time for deposit processing: " + delta +
                            " ms");
            // receipt.setVerboseDescription(this.verboseDescription.toString());

            // if something hasn't killed it already (allowed), then complete the transaction
            sc.commit();

            // return the receipt for the purposes of the location
            return receipt;
        }
        catch (DSpaceSwordException e)
        {
            log.error("caught exception:", e);
            throw new SwordServerException(
                    "There was a problem depositing the item", e);
        }
        catch (SQLException e)
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

    public void deleteMediaResource(String emUri,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose delete of media resource");

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

            SwordUrlManager urlManager = config.getUrlManager(context, config);
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();

            // get the deposit target
            if (urlManager.isActionableBitstreamUrl(context, emUri))
            {
                Bitstream bitstream = urlManager.getBitstream(context, emUri);
                if (bitstream == null)
                {
                    throw new SwordError(404);
                }

                // now we have the deposit target, we can determine whether this operation is allowed
                // at all
                wfm.deleteBitstream(context, bitstream);

                // check that we can submit to ALL the items this bitstream is in
                List<Item> items = new ArrayList<>();
                for (Bundle bundle : bitstream.getBundles())
                {
                    List<Item> bundleItems = bundle
                            .getItems();
                    for (Item item : bundleItems)
                    {
                        this.checkAuth(sc, item);
                        items.add(item);
                    }
                }

                // make a note of the authentication in the verbose string
                this.verboseDescription.append("Authenticated user: " +
                        sc.getAuthenticated().getEmail());
                if (sc.getOnBehalfOf() != null)
                {
                    this.verboseDescription.append("Depositing on behalf of: " +
                            sc.getOnBehalfOf().getEmail());
                }

                this.removeBitstream(sc, bitstream, items, authCredentials,
                        config);
            }
            else
            {
                Item item = this.getDSpaceTarget(context, emUri, config);
                if (item == null)
                {
                    throw new SwordError(404);
                }

                // now we have the deposit target, we can determine whether this operation is allowed
                // at all
                wfm.deleteMediaResource(context, item);

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

                // do the business of removal
                this.removeContent(sc, item, authCredentials, config);
            }

            // now we've produced a deposit, we need to decide on its workflow state
            wfm.resolveState(context, null, null, this.verboseDescription,
                    false);

            //ReceiptGenerator genny = new ReceiptGenerator();
            //DepositReceipt receipt = genny.createReceipt(context, result, config);

            Date finish = new Date();
            long delta = finish.getTime() - start.getTime();

            this.verboseDescription
                    .append("Total time for deposit processing: " + delta +
                            " ms");
            // receipt.setVerboseDescription(this.verboseDescription.toString());

            // if something hasn't killed it already (allowed), then complete the transaction
            sc.commit();

            // So, we don't actually return a receipt, but it was useful constructing it.  Perhaps this will
            // change in the spec?
        }
        catch (DSpaceSwordException e)
        {
            log.error("caught exception:", e);
            throw new SwordServerException(
                    "There was a problem depositing the item", e);
        }
        catch (SQLException e)
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

    public DepositReceipt addResource(String emUri, Deposit deposit,
            AuthCredentials authCredentials, SwordConfiguration swordConfig)
            throws SwordError, SwordServerException, SwordAuthException
    {
        // start the timer
        Date start = new Date();

        // store up the verbose description, which we can then give back at the end if necessary
        this.verboseDescription
                .append("Initialising verbose add to media resource");

        SwordContext sc = null;
        SwordConfigurationDSpace config = (SwordConfigurationDSpace) swordConfig;

        try
        {
            sc = this.doAuth(authCredentials);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager.getHeader(context, "sword_add", ""));
            }

            // get the deposit target
            Item item = this.getDSpaceTarget(context, emUri, config);
            if (item == null)
            {
                throw new SwordError(404);
            }

            // now we have the deposit target, we can determine whether this operation is allowed
            // at all
            WorkflowManager wfm = WorkflowManagerFactory.getInstance();
            wfm.addResourceContent(context, item);

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

            DepositResult result;
            try
            {
                result = this
                        .addContent(sc, item, deposit, authCredentials, config);
                if (deposit.isMultipart())
                {
                    ContainerManagerDSpace cm = new ContainerManagerDSpace();
                    result = cm
                            .doAddMetadata(sc, item, deposit, authCredentials,
                                    config, result);
                }
            }
            catch (DSpaceSwordException | SwordError e)
            {
                if (config.isKeepPackageOnFailedIngest())
                {
                    try
                    {
                        this.storePackageAsFile(deposit, authCredentials,
                                config);
                        if (deposit.isMultipart())
                        {
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
            wfm.resolveState(context, deposit, null, this.verboseDescription,
                    false);

            ReceiptGenerator genny = new ReceiptGenerator();

            // Now, this bit is tricky:
            DepositReceipt receipt;
            // If this was a single file deposit, then we don't return a receipt, we just
            // want to specify the location header
            if (deposit.getPackaging().equals(UriRegistry.PACKAGE_BINARY))
            {
                receipt = genny.createFileReceipt(context, result, config);
            }
            // if, on the other-hand, this was a package, then we want to generate a
            // deposit receipt proper, but with the location being for the media resource
            else
            {
                receipt = genny.createReceipt(context, result, config, true);
            }

            Date finish = new Date();
            long delta = finish.getTime() - start.getTime();

            this.verboseDescription
                    .append("Total time for add processing: " + delta + " ms");
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

    private void removeContent(SwordContext swordContext, Item item,
            AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordAuthException
    {
        try
        {
            // remove content only really means everything from the ORIGINAL bundle
            VersionManager vm = new VersionManager();
            Iterator<Bundle> bundles = item.getBundles().iterator();
            while (bundles.hasNext())
            {
                Bundle bundle = bundles.next();
                if (Constants.CONTENT_BUNDLE_NAME.equals(bundle.getName()))
                {
                    bundles.remove();
                    vm.removeBundle(swordContext.getContext(), item, bundle);
                }
            }
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

    private void removeBitstream(SwordContext swordContext, Bitstream bitstream,
            List<Item> items, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordAuthException
    {
        try
        {
            // remove content only really means everything from the ORIGINAL bundle
            VersionManager vm = new VersionManager();
            for (Item item : items)
            {
                vm.removeBitstream(swordContext.getContext(), item, bitstream);
            }
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

    private void replaceContent(SwordContext swordContext, Item item,
            Deposit deposit, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // is the content acceptable?  If not, this will throw an error
        this.isAcceptable(swordConfig, context, deposit, item);

        // Obtain the relevant ingester from the factory
        SwordContentIngester si = SwordIngesterFactory
                .getContentInstance(context, deposit, null);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        try
        {
            // delegate the to the version manager to get rid of any existing content and to version
            // if if necessary
            VersionManager vm = new VersionManager();
            vm.removeBundle(swordContext.getContext(), item, "ORIGINAL");
        }
        catch (SQLException | IOException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }

        // do the deposit
        DepositResult result = si
                .ingest(context, deposit, item, this.verboseDescription);
        this.verboseDescription.append("Replace completed successfully");

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);
    }

    private DepositResult replaceBitstream(SwordContext swordContext,
            List<Item> items, Bitstream bitstream, Deposit deposit,
            AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // FIXME: this is basically not possible with the existing DSpace API.

        // We hack around it by deleting the old bitstream and
        // adding the new one and returning it,
        // but this isn't in line with the REST approach of SWORD, so the caller should really
        // 405 the client

        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // is the content acceptable to the items?  If not, this will throw an error
        for (Item item : items)
        {
            this.isAcceptable(swordConfig, context, deposit, item);
        }

        // Obtain the relevant ingester from the factory
        SwordContentIngester si = SwordIngesterFactory
                .getContentInstance(context, deposit, null);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        try
        {
            // first we delete the original bitstream
            this.removeBitstream(swordContext, bitstream, items,
                    authCredentials, swordConfig);

            DepositResult result = null;
            boolean first = true;
            for (Item item : items)
            {
                if (first)
                {
                    // just do this to the first item
                    result = this.addContent(swordContext, item, deposit,
                            authCredentials, swordConfig);
                }
                else
                {
                    // now duplicate the bitstream to all the others
                    List<Bundle> bundles = item.getBundles();
                    if (!bundles.isEmpty())
                    {
                        bundleService.addBitstream(context, bundles.get(0),
                                result.getOriginalDeposit());
                    }
                    else
                    {
                        Bundle bundle = bundleService.create(context, item,
                                Constants.CONTENT_BUNDLE_NAME);
                        bundleService.addBitstream(context, bundle,
                                result.getOriginalDeposit());
                    }
                }

            }

            // DepositResult result = si.ingest(context, deposit, items, this.verboseDescription);
            this.verboseDescription.append("Replace completed successfully");

            return result;
        }
        catch (SQLException e)
        {
            throw new DSpaceSwordException(e);
        }
        catch (AuthorizeException e)
        {
            throw new SwordAuthException(e);
        }
    }

    private DepositResult addContent(SwordContext swordContext, Item item,
            Deposit deposit, AuthCredentials authCredentials,
            SwordConfigurationDSpace swordConfig)
            throws DSpaceSwordException, SwordError, SwordAuthException,
            SwordServerException
    {
        // get the things out of the service that we need
        Context context = swordContext.getContext();

        // is the content acceptable?  If not, this will throw an error
        this.isAcceptable(swordConfig, context, deposit, item);

        // Obtain the relevant ingester from the factory
        SwordContentIngester si = SwordIngesterFactory
                .getContentInstance(context, deposit, null);
        this.verboseDescription
                .append("Loaded ingester: " + si.getClass().getName());

        // do the deposit
        DepositResult result = si
                .ingest(context, deposit, item, this.verboseDescription);
        this.verboseDescription.append("Add completed successfully");

        // store the originals (this code deals with the possibility that that's not required)
        this.storeOriginals(swordConfig, context, this.verboseDescription,
                deposit, result);

        return result;
    }

    private Item getDSpaceTarget(Context context, String editMediaUrl,
            SwordConfigurationDSpace config)
            throws DSpaceSwordException, SwordError
    {
        SwordUrlManager urlManager = config.getUrlManager(context, config);

        // get the target collection
        Item item = urlManager.getItem(context, editMediaUrl);

        this.verboseDescription
                .append("Performing replace using edit-media URL: " +
                        editMediaUrl);
        this.verboseDescription
                .append("Location resolves to item with handle: " +
                        item.getHandle());

        return item;
    }

    private void checkAuth(SwordContext sc, Item item)
            throws DSpaceSwordException, SwordError, SwordAuthException
    {
        Context context = sc.getContext();
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
    }
}
