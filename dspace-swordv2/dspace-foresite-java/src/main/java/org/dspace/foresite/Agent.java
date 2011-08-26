/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.net.URI;
import java.util.List;

/**
 * @Author Richard Jones
 */
public interface Agent extends OREResource
{
	// methods for initialising the Agent

	void initialise();

	void initialise(URI uri);

	// Refactored out for 0.9...
	
	// List<URI> getSeeAlso() throws OREException;

    // void addSeeAlso(URI uri);

    // void setSeeAlso(List<URI> uris);

    // void clearSeeAlso();

    List<String> getNames() throws OREException;

    void setNames(List<String> names);

    void addName(String name);

    List<URI> getMboxes() throws OREException;

    void setMboxes(List<URI> mboxes);

    void addMbox(URI mbox);
}
