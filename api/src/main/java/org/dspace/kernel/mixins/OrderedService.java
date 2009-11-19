/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.kernel.mixins;


/**
 * This service mixin will cause the service/provider/etc. to be ordered against other
 * classes that implement the same interface (not this one),
 * classes that do not implement this interface
 * 
 * @author Aaron Zeckoski (azeckoski @ gmail.com)
 */
public interface OrderedService {

    /**
     * Sets the order to load the bean which implements this method compared
     * to other beans of the same type, lower orders (numbers) will be loaded first
     * (i.e. order 1 will appear before order 3 in the list) and the 
     * orders do not have to be consecutive (there can be gaps), 
     * 2 beans with the same order or beans with no order set will be ordered randomly
     * @return an int which represents the loading order
     */
    public int getOrder();

}
