/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.dspace.services.model.Event;

/**
 * A null implementation of AbstractUsageEvent to absorb events harmlessly and
 * cheaply.
 * 
 * @author Mark H. Wood
 * @author Mark Diggory (mdiggory at atmire.com)
 * @version $Revision: 3734 $
 */
public class PassiveUsageEventListener extends AbstractUsageEventListener
{
	/**
     * Do nothing and return. Effectively, the event is discarded.
     */
	@Override
	public void receiveEvent(Event event) {
		return;
	}

}
