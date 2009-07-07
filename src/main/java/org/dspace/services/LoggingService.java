package org.dspace.services;

import org.dspace.content.DSpaceObject;
import org.dspace.eperson.EPerson;

public interface LoggingService {

	public void fire(DSpaceObject dspaceObject, String ip, EPerson currentUser);

}
