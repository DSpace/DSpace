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
public interface Proxy extends OREResource
{
	// methods for initialising proxies

	void initialise(URI uri);

	// methods for dealing with Aggregations and AggregatedResources we are proxying with

	AggregatedResource getProxyFor() throws OREException;

    void setProxyFor(AggregatedResource proxyFor) throws OREException;

    void setProxyForURI(URI uri) throws OREException;

    Aggregation getProxyIn() throws OREException;

    void setProxyIn(Aggregation proxyIn) throws OREException;

    void setProxyInURI(URI uri) throws OREException;

	// methods for dealing with relationships between Proxies

	// FIXME: do we need thise, given the existence of createTriple?
	
	void assertRelation(URI uri, Proxy proxy) throws OREException;

	List<Proxy> getRelated(URI uri) throws OREException;

	// methods for dealing with lineage

	void setLineage(URI externalProxy) throws OREException;

	URI getLineage() throws OREException;
}
