/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.apache.log4j.Logger;

import org.dspace.core.Context;
import org.dspace.core.LogManager;

import org.purl.sword.server.SWORDServer;
import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;

/**
 * An implementation of the SWORDServer interface to allow SWORD deposit
 * operations on DSpace.  See:
 *
 * http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_0.5
 *
 * @author Richard Jones
 */
public class DSpaceSWORDServer implements SWORDServer
{
    /** Log4j logger */
    public static final Logger log = Logger.getLogger(DSpaceSWORDServer.class);

    // methods required by SWORDServer interface
    ////////////////////////////////////////////

    /* (non-Javadoc)
     * @see org.purl.sword.SWORDServer#doServiceDocument(org.purl.sword.base.ServiceDocumentRequest)
     */
    public ServiceDocument doServiceDocument(ServiceDocumentRequest request)
            throws SWORDAuthenticationException, SWORDException,
            SWORDErrorException
    {
        // gah.  bloody variable scoping.
        // set up a dummy sword context for the "finally" block
        SWORDContext sc = null;

        try
        {
            // first authenticate the request
            // note: this will build our various DSpace contexts for us
            SWORDAuthenticator auth = new SWORDAuthenticator();
            sc = auth.authenticate(request);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager
                        .getHeader(context, "sword_do_service_document", ""));
            }

            // log the request
            log.info(LogManager
                    .getHeader(context, "sword_service_document_request",
                            "username=" + request.getUsername() +
                                    ",on_behalf_of=" +
                                    request.getOnBehalfOf()));

            // prep the service request, then get the service document out of it
            SWORDService service = new SWORDService(sc);
            ServiceDocumentManager manager = new ServiceDocumentManager(
                    service);
            ServiceDocument doc = manager
                    .getServiceDocument(request.getLocation());

            return doc;
        }
        catch (DSpaceSWORDException e)
        {
            log.error("caught exception: ", e);
            throw new SWORDException(
                    "The DSpace SWORD interface experienced an error", e);
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

    /* (non-Javadoc)
     * @see org.purl.sword.SWORDServer#doSWORDDeposit(org.purl.sword.server.Deposit)
     */
    public DepositResponse doDeposit(Deposit deposit)
            throws SWORDAuthenticationException, SWORDException,
            SWORDErrorException
    {
        // gah.  bloody variable scoping.
        // set up a dummy sword context for the "finally" block
        SWORDContext sc = null;

        try
        {
            // first authenticate the request
            // note: this will build our various DSpace contexts for us
            SWORDAuthenticator auth = new SWORDAuthenticator();
            sc = auth.authenticate(deposit);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(
                        LogManager.getHeader(context, "sword_do_deposit", ""));
            }

            // log the request
            log.info(LogManager.getHeader(context, "sword_deposit_request",
                    "username=" + deposit.getUsername() + ",on_behalf_of=" +
                            deposit.getOnBehalfOf()));

            // prep and execute the deposit
            SWORDService service = new SWORDService(sc);
            service.setVerbose(deposit.isVerbose());
            DepositManager dm = new DepositManager(service);
            DepositResponse response = dm.deposit(deposit);

            // if something hasn't killed it already (allowed), then complete the transaction
            sc.commit();

            return response;
        }
        catch (DSpaceSWORDException e)
        {
            log.error("caught exception:", e);
            throw new SWORDException("There was a problem depositing the item",
                    e);
        }
        finally
        {
            // if, for some reason, we wind up here with a not null context
            // then abort it (the above should commit it if everything works fine)
            if (sc != null)
            {
                sc.abort();
            }
        }
    }

    /* (non-Javadoc)
     * @see org.purl.sword.SWORDServer#doSWORDDeposit(org.purl.sword.server.Deposit)
     */
    public AtomDocumentResponse doAtomDocument(AtomDocumentRequest adr)
            throws SWORDAuthenticationException, SWORDException,
            SWORDErrorException
    {
        // gah.  bloody variable scoping.
        // set up a dummy sword context for the "finally" block
        SWORDContext sc = null;

        try
        {
            // first authenticate the request
            // note: this will build our various DSpace contexts for us
            SWORDAuthenticator auth = new SWORDAuthenticator();
            sc = auth.authenticate(adr);
            Context context = sc.getContext();

            if (log.isDebugEnabled())
            {
                log.debug(LogManager
                        .getHeader(context, "sword_do_atom_document", ""));
            }

            // log the request
            log.info(LogManager
                    .getHeader(context, "sword_atom_document_request",
                            "username=" + adr.getUsername()));

            // prep the service request, then get the service document out of it
            SWORDService service = new SWORDService(sc);
            MediaEntryManager manager = new MediaEntryManager(service);

            return manager.getMediaEntry(adr.getLocation());
        }
        catch (DSpaceSWORDException e)
        {
            log.error("caught exception: ", e);
            throw new SWORDException(
                    "The DSpace SWORD interface experienced an error", e);
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
}
