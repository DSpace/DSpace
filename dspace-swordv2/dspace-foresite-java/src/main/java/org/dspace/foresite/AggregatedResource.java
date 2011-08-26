/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.util.List;
import java.net.URI;

/**
 * @Author Richard Jones
 */
public interface AggregatedResource extends OREResource
{
	// methods to initialise the resource
	
	void initialise(URI uri) throws OREException;

	// methods to work with the Aggregations pertaining to this AggregatedResource
	
	List<URI> getAggregations() throws OREException;

    void setAggregations(List<URI> aggregations);

    void addAggregation(URI aggregation);

    void clearAggregations();

    Aggregation getAggregation() throws OREException;

	// methods to deal with AggregatedResource type information

	List<URI> getTypes() throws OREException;

    void setTypes(List<URI> types) throws OREException;

    void addType(URI type) throws OREException;

    void clearTypes() throws OREException;

	// methods to deal with AggregatedResources which are also Aggregations

	List<URI> getResourceMaps() throws OREException;

	void setResourceMaps(List<URI> rems) throws OREException;

	void addResourceMap(URI rem) throws OREException;

	void clearResourceMaps() throws OREException;

	// methods to deal with Proxies

	boolean hasProxy() throws OREException;

	Proxy getProxy() throws OREException;

	Proxy createProxy(URI proxyURI) throws OREException;
}
