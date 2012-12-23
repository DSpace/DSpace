/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.kernel.mixins;


/**
 * Permit the service or provider to be ordered against other
 * classes that implement the same interface (not this one).
 * Classes that do not implement this interface may be initialized in 
 * any order.
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface OrderedService {

    /**
     * Sets the order to load the bean which implements this method compared
     * to other beans of the same type.  Lower orders (numbers) will be
     * loaded first (i.e. order 1 will appear before order 3 in the 
     * list).  Orders do not have to be consecutive (there can be gaps).
     * Beans with the same order or beans with no order set will be
     * ordered randomly.
     *
     * @return an int which represents the loading order
     */
    public int getOrder();

}
