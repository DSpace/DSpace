/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import mockit.Mock;
import mockit.MockClass;
import org.dspace.core.Context;
import org.dspace.event.Event;

/**
 * Dummy Discovery IndexEventConsumer. It essentially does nothing,
 * as Discovery/Solr is not actively running during unit testing.
 *
 * @author tdonohue
 */
@MockClass(realClass=IndexEventConsumer.class)
public class MockIndexEventConsumer {
   
    //public void initialize() throws Exception {
        //do nothing
    //}
    
    @Mock
    public void consume(Context ctx, Event event) throws Exception {
        //do nothing - Solr is not running during unit testing, so we cannot index test content in Solr
    }
    
    @Mock
    public void end(Context ctx) throws Exception {
        //do nothing - Solr is not running during unit testing, so we cannot index test content in Solr
    }
    
    //public void finish(Context ctx) throws Exception {
        //do nothing
    //}
}
