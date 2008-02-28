/*
 * MetadataStyleSelection.java
 *
 * Version: $Revision: 1 $
 *
 * Date: $Date: 2007-10-25 09:00:00 +0100 (thu, 25 oct 2007) $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.webui.util;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 * Use the value of the metadata specified with the key <code>webui.display.metadata-style</code>
 * as name for the display style of the item. Style name is case insensitive.
 * 
 * @author Andrea Bollini
 * @version $Revision: 1 $
 * 
 */
public class MetadataStyleSelection extends AKeyBasedStyleSelection
{
    /** log4j logger */
    private static Logger log = Logger
            .getLogger(MetadataStyleSelection.class);
    
    /**
     * Get the style using an item metadata
     */
    public String getStyleForItem(Item item) throws SQLException
    {
        String metadata = ConfigurationManager.getProperty("webui.itemdisplay.metadata-style");
        DCValue[] value = item.getMetadata(metadata);
        String styleName = "default";
        if (value.length > 0)
        {
            if (value.length >= 1)
            {
                log
                .warn("more then one value for metadata '"
                        + metadata
                        + "'. Using the first one");
            }
            styleName = value[0].value.toLowerCase();            
        }
        
       
        // Specific style specified. Check style exists
        if (isConfigurationDefinedForStyle(styleName))
        {
            log.warn("metadata '" + metadata + "' specify undefined item display style '"
                    + styleName + "'.  Using default");
            return "default";
        }
        // Style specified & exists
        return styleName;
    }
}
