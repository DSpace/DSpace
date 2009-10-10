/**
 * $Id$
 * $URL$
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace Foundation License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */
package org.dspace.statistics.content.filter;

/**
 * 
 * @author kevinvandevelde at atmire.com
 * Date: 12-mrt-2009
 * Time: 10:36:03
 * 
 */
public interface StatisticsFilter {
    
    public String toQuery();
}
