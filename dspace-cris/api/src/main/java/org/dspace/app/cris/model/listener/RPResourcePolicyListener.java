/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostLoadEvent;
import org.hibernate.event.spi.PostLoadEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;

public class RPResourcePolicyListener implements PostUpdateEventListener,
        PostInsertEventListener, PostDeleteEventListener, PostLoadEventListener
{

    private static Logger log = Logger
            .getLogger(RPResourcePolicyListener.class);

    private static final String QUERY_DELETE_WITH_EPERSON = "select * from resourcepolicy where eperson_id = ? resource_type_id = 9 and resource_id = ?";

    private static final String QUERY_DELETE_WITHOUT_EPERSON = "select * from resourcepolicy where resource_type_id = 9 and resource_id = ?";

    @Override
    public void onPostDelete(PostDeleteEvent event)
    {
        Object object = event.getEntity();
        if (!(object instanceof ResearcherPage))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostDelete " + RPResourcePolicyListener.class);

        ResearcherPage cris = (ResearcherPage) object;

        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            delete(null, cris.getId(), context);
            context.complete();
        }
        catch (Exception e)
        {
            log.error(
                    "Failed to delete resource policy attached to RP just deleted"
                            + cris.getPublicPath() + " uuid:" + cris.getUuid());
            emailException(e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        log.debug("End onPostDelete " + RPResourcePolicyListener.class);
    }

    private void delete(Integer epersonID, Integer rpID, Context context)
            throws SQLException
    {
        TableRow row = null;

        if (epersonID == null)
        {
            row = DatabaseManager.querySingleTable(context, "ResourcePolicy",
                    QUERY_DELETE_WITHOUT_EPERSON, rpID);
        }
        else
        {
            row = DatabaseManager.querySingleTable(context, "ResourcePolicy",
                    QUERY_DELETE_WITH_EPERSON, epersonID, rpID);
        }
        if (row != null)
        {
            DatabaseManager.delete(context, row);
        }
    }

    @Override
    public void onPostInsert(PostInsertEvent event)
    {
        Object object = event.getEntity();
        if (!(object instanceof ResearcherPage))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostInsert " + RPResourcePolicyListener.class);

        ResearcherPage cris = (ResearcherPage) object;

        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            if (cris.getEpersonID() != null)
            {
                ResourcePolicy resourcePolicy = ResourcePolicy.create(context);
                resourcePolicy.setAction(Constants.ADMIN);
                resourcePolicy.setEPerson(cris.getDspaceUser());
                resourcePolicy.setResource(cris);
                resourcePolicy.setResourceType(CrisConstants.RP_TYPE_ID);
                resourcePolicy.setResourceID(cris.getID());
                resourcePolicy.update();
            }
            context.complete();
            cris.setOldEpersonID(cris.getEpersonID());
        }
        catch (Exception e)
        {
            log.error(
                    "Failed to delete resource policy attached to RP just deleted"
                            + cris.getPublicPath() + " uuid:" + cris.getUuid());
            emailException(e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        log.debug("End onPostInsert " + RPResourcePolicyListener.class);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event)
    {

        Object object = event.getEntity();
        if (!(object instanceof ResearcherPage))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostUpdate " + RPResourcePolicyListener.class);

        ResearcherPage cris = (ResearcherPage) object;

        Context context = null;
        try
        {
            context = new Context();
            context.turnOffAuthorisationSystem();
            if ((cris.getOldEpersonID() != null
                    && !cris.getOldEpersonID().equals(cris.getEpersonID()))
                    || (cris.getOldEpersonID() == null
                            && cris.getEpersonID() != null))
            {
                if (cris.getEpersonID() != null)
                {
                    ResourcePolicy resourcePolicy = ResourcePolicy
                            .create(context);
                    resourcePolicy.setAction(Constants.ADMIN);
                    resourcePolicy.setEPerson(cris.getDspaceUser());
                    resourcePolicy.setResource(cris);
                    resourcePolicy.setResourceType(CrisConstants.RP_TYPE_ID);
                    resourcePolicy.setResourceID(cris.getID());
                    resourcePolicy.update();
                }
                if (cris.getOldEpersonID() != null)
                {
                    delete(cris.getOldEpersonID(), cris.getId(), context);
                }
                cris.setOldEpersonID(cris.getEpersonID());
            }
            context.complete();
        }
        catch (Exception e)
        {
            log.error(
                    "Failed to delete resource policy attached to RP just deleted"
                            + cris.getPublicPath() + " uuid:" + cris.getUuid());
            emailException(e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        log.debug("End onPostUpdate " + RPResourcePolicyListener.class);
    }

    @Override
    public void onPostLoad(PostLoadEvent event)
    {
        Object object = event.getEntity();
        if (object instanceof ResearcherPage)
        {
            log.debug("Call onPostLoad " + RPResourcePolicyListener.class);
            ResearcherPage rp = (ResearcherPage) object;
            rp.setOldEpersonID(rp.getEpersonID());
            log.debug("End onPostLoad " + RPResourcePolicyListener.class);
        }
    }

    private void emailException(Exception exception)
    {
        // Also email an alert, system admin may need to check for stale lock
        try
        {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (recipient != null)
            {
                Email email = Email.getEmail(I18nUtil.getEmailFilename(
                        Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(
                        ConfigurationManager.getProperty("dspace.url"));
                email.addArgument(new Date());

                String stackTrace;

                if (exception != null)
                {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                }
                else
                {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        }
        catch (Exception e)
        {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }
    }
}
