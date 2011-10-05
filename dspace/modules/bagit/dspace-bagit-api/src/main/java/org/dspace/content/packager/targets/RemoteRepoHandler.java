package org.dspace.content.packager.targets;

import java.io.File;

import org.dspace.content.packager.BagItDisseminatorException;
import org.dspace.eperson.EPerson;

public interface RemoteRepoHandler {
	public void send(File aBagItFile, EPerson aPerson)
			throws BagItDisseminatorException;
	
	public String getHandlerName();
}
