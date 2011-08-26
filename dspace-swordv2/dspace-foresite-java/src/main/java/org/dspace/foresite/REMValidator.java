/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.util.List;
import java.util.Date;

/**
 * @Author Richard Jones
 */
public class REMValidator
{
	public void prepForSerialisation(ResourceMap rem) 
			throws OREException
	{
		// if there is no creator of the REM defined, then set a default one
		List<Agent> agents = rem.getCreators();
		if (agents.size() == 0)
		{
			Agent agent = OREFactory.createAgent();
			agent.addName("Foresite ORE Library");
			rem.addCreator(agent);
		}

		// if there is no modified date, set it to the current timestamp
		Date modified = rem.getModified();
		if (modified == null)
		{
			Date now = new Date();
			rem.setModified(now);
		}

		// if there are no aggregated resource in the aggregation, then we throw
		// an error, because we can't serialise!
		// FIXME: impelement!!!!
	}
}
