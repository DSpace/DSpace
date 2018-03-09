/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.event;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.ArrayUtils;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Demonstration and test consumer for the event system. This consumer only
 * makes an entry in the log, and on an output stream, for each event it
 * receives. It also logs when consume() and end() get called. It is intended
 * for testing, exploring, and debugging the event system.
 * 
 * @version $Revision$
 */
public class TestConsumer implements Consumer
{
    // Log4j logger
    private static Logger log = Logger.getLogger(TestConsumer.class);

    // Send diagnostic output here - set to null to turn it off.
    private static PrintStream out = ConfigurationManager
            .getBooleanProperty("testConsumer.verbose") ? System.out : null;

    @Override
    public void initialize() throws Exception
    {
        log.info("EVENT: called TestConsumer.initialize();");
        if (out != null)
        {
            out.println("TestConsumer.initialize();");
        }
    }

    /**
     * Consume a content event - display it in detail.
     * 
     * @param ctx
     *            DSpace context
     * @param event
     *            Content event
     */
    @Override
    public void consume(Context ctx, Event event) throws Exception
    {
        EPerson ep = ctx.getCurrentUser();
        String user = (ep == null) ? "(none)" : ep.getEmail();
        String detail = event.getDetail();

        String msg = "EVENT: called TestConsumer.consume(): EventType="
                + event.getEventTypeAsString()
                + ", SubjectType="
                + event.getSubjectTypeAsString()
                + ", SubjectID="
                + String.valueOf(event.getSubjectID())
                + ", ObjectType="
                + event.getObjectTypeAsString()
                + ", ObjectID="
                + String.valueOf(event.getObjectID())
                + ", Identifiers="
                + ArrayUtils.toString(event.getIdentifiers())
                + ", TimeStamp="
                + applyDateFormat(new Date(event.getTimeStamp()))
                + ", user=\""
                + user
                + "\""
                + ", extraLog=\""
                + ctx.getExtraLogInfo()
                + "\""
                + ", dispatcher="
                + String.valueOf(event.getDispatcher())
                + ", detail="
                + (detail == null ? "[null]" : "\"" + detail + "\"")
                + ", transactionID="
                + (event.getTransactionID() == null ? "[null]" : "\""
                        + event.getTransactionID() + "\"") + ", context="
                + ctx.toString();
        log.info(msg);
        if (out != null)
        {
            out.println("TestConsumer.consume(): " + msg);
        }
    }

    @Override
    public void end(Context ctx) throws Exception
    {
        log.info("EVENT: called TestConsumer.end();");
        if (out != null)
        {
            out.println("TestConsumer.end();");
        }

    }

    @Override
    public void finish(Context ctx) throws Exception
    {
        log.info("EVENT: called TestConsumer.finish();");
        if (out != null)
        {
            out.println("TestConsumer.finish();");
        }

    }

    private String applyDateFormat(Date thisDate)
    {
        return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS Z").format(thisDate);
    }


}
