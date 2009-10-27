/*
 * TestConsumer.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.event;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    static final DateFormat df = new SimpleDateFormat(
            "dd-MMM-yyyy HH:mm:ss.SSS Z");

    public void initialize() throws Exception
    {
        log.info("EVENT: called TestConsumer.initialize();");
        if (out != null)
            out.println("TestConsumer.initialize();");
    }

    /**
     * Consume a content event - display it in detail.
     * 
     * @param ctx
     *            DSpace context
     * @param event
     *            Content event
     */
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
                + ", TimeStamp="
                + df.format(new Date(event.getTimeStamp()))
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
            out.println("TestConsumer.consume(): " + msg);
    }

    public void end(Context ctx) throws Exception
    {
        log.info("EVENT: called TestConsumer.end();");
        if (out != null)
            out.println("TestConsumer.end();");

    }

    public void finish(Context ctx) throws Exception
    {
        log.info("EVENT: called TestConsumer.finish();");
        if (out != null)
            out.println("TestConsumer.finish();");

    }

}
