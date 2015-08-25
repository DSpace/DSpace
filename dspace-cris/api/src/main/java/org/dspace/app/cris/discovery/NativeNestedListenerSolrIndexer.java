/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.discovery;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;
import org.dspace.app.cris.model.jdyna.ACrisNestedObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;

import it.cilea.osd.common.listener.NativePostDeleteEventListener;
import it.cilea.osd.common.listener.NativePostUpdateEventListener;
import it.cilea.osd.common.model.Identifiable;

/**
 * This listener is used to keep the Search Solr core up-to-date with change to CRIS-NESTED fields. 
 * All the Event that are not related to the Cris Nested Entity will be ignored. 
 * When a change happen the solr document related to the ACrisNestedObject will be updated.
 * 
 * 
 * @author pascarelli
 * 
 */
public class NativeNestedListenerSolrIndexer implements NativePostUpdateEventListener, 
    NativePostDeleteEventListener
{
    private static Logger log = Logger
            .getLogger(NativeNestedListenerSolrIndexer.class);
    
    private CrisSearchService crisSearchService;

    public CrisSearchService getCrisSearchService()
    {
        return crisSearchService;
    }
    
    public void setCrisSearchService(CrisSearchService crisSearchService)
    {
        this.crisSearchService = crisSearchService;
    }
    
    @Override
    public <T extends Identifiable> void onPostUpdate(T entity)
    {
    	
        Object object = entity;
        if (!(object instanceof ACrisNestedObject))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostUpdate " + NativeNestedListenerSolrIndexer.class);
        
        ACrisNestedObject nested = (ACrisNestedObject) object;

        try
        {
            crisSearchService.indexNestedObject(nested, false);    
        }
        catch (Exception e)
        {
            log.error("Failed to update CRIS-NESTED metadata in discovery index for cris-nested:"
                    + nested.getDisplayValue() + " uuid:"+nested.getUuid());
            emailException(e);
        }
    }
 
    
    @Override
    public <T> void onPostDelete(T entity)
    {
    	
        Object object = entity;
        if (!(object instanceof ACrisNestedObject))
        {
            // nothing to do
            return;
        }

        log.debug("Call onPostDelete " + NativeNestedListenerSolrIndexer.class);
        
        ACrisNestedObject nested = (ACrisNestedObject) object;

        try
        {
            crisSearchService.unIndexContent(null, nested, false);    
        }
        catch (Exception e)
        {
            log.error("Failed to remove CRIS-NESTED metadata in discovery index for cris-nested:"
                    + nested.getDisplayValue() + " uuid:"+nested.getUuid());
            emailException(e);
        }
   }
    
    private void emailException(Exception exception) {
        // Also email an alert, system admin may need to check for stale lock
        try {
            String recipient = ConfigurationManager
                    .getProperty("alert.recipient");

            if (recipient != null) {
                Email email = Email
                        .getEmail(I18nUtil.getEmailFilename(
                                Locale.getDefault(), "internal_error"));
                email.addRecipient(recipient);
                email.addArgument(ConfigurationManager
                        .getProperty("dspace.url"));
                email.addArgument(new Date());

                String stackTrace;

                if (exception != null) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    exception.printStackTrace(pw);
                    pw.flush();
                    stackTrace = sw.toString();
                } else {
                    stackTrace = "No exception";
                }

                email.addArgument(stackTrace);
                email.send();
            }
        } catch (Exception e) {
            // Not much we can do here!
            log.warn("Unable to send email alert", e);
        }
    }
}
