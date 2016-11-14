/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.client;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A stream that will write any output to the specified panel. 
 * 
 * @author Neil Taylor
 */
public class DebugOutputStream extends OutputStream
{
    /**
     * Panel that will display the messages. 
     */
    private MessageOutputPanel panel; 
    
    /**
     * Create a new instance and specify the panel that will receive the output. 
     * 
     * @param panel The panel. 
     */
    public DebugOutputStream(MessageOutputPanel panel)
    {
        this.panel = panel;    
    }
    
    /**
     * Override the write method from OutputStream. Capture the char and 
     * send it to the panel. 
     *  
     * @param arg0 The output character, expressed as an integer. 
     *  
     * @see java.io.OutputStream#write(int)
     */
    public void write(int arg0) throws IOException
    {
        panel.addCharacter(Character.valueOf((char)arg0));  
    }
 
}
