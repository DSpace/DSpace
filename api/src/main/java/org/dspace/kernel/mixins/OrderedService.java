/**
 * $Id: OrderedService.java 3236 2008-10-24 16:46:39Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/api/src/main/java/org/dspace/kernel/mixins/OrderedService.java $
 * OrderedService.java - DSpace2 - Oct 24, 2008 4:54:26 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2008 Aaron Zeckoski
 * Licensed under the Apache License, Version 2.0
 * 
 * A copy of the Apache License has been included in this 
 * distribution and is available at: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Aaron Zeckoski (azeckoski @ gmail.com) (aaronz @ vt.edu) (aaron @ caret.cam.ac.uk)
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
