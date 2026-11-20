/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * This class has been initially developed by Graham Triggs, we have moved to a
 * CILEA package to make more clear that it is not included in the org.dspace
 * sourcecode
 * 
 * This defines the interface that must be implemented by 'virtual field' processors.
 * 
 * IMPORTANT: Calls to getMetadata() are stateless. If your implementation retains state
 * between calls, bad things will happen! The fieldCache may be used to store data that can
 * be referenced later on, but be aware that you will be sharing it with any other implementation
 * so name your keys appropriately!
 * 
 * @author grahamt
 */
public interface VirtualFieldDisseminator {

    public String[] getMetadata(Context context, Item item, String fieldName);
}