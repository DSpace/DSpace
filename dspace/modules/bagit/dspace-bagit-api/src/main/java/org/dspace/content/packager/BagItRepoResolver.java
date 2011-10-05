package org.dspace.content.packager;

import org.dspace.content.packager.targets.RemoteRepoHandler;
import org.dspace.content.packager.targets.TreeBaseHandler;

public class BagItRepoResolver {
	public static final RemoteRepoHandler getRepo(String aRepoName)
			throws BagItDisseminatorException {
		if (aRepoName.equalsIgnoreCase("TREEBASE")) {
			return new TreeBaseHandler();
		}
		else {
			throw new BagItDisseminatorException(
					"Unsupported remote repository requested");
		}
		
		// TODO: make this configurable, not hard-coded
	}
}
