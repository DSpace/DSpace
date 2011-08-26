/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.util.Date;
import java.util.List;
import java.net.URI;

/**
 * @Author Richard Jones
 */
public interface ResourceMap extends OREResource
{
	// methods for initialising the resource map

	void initialise(URI uri) throws OREException;

	// methods for dealing with authoritative resource maps

	boolean isAuthoritative() throws OREException;

	void setAuthoritative(boolean authoritative) throws OREException;

	// methods for dealing with date created

	Date getCreated() throws OREException;

    void setCreated(Date created);

	// methods for dealing with date modified

	Date getModified() throws OREException;

    void setModified(Date modified);

	// methods for dealing with rights associated with Resource Maps

	// FIXME: one or many rights?
	String getRights();

    void setRights(String rights);

    void removeRights();

	// methods for dealing with aggregations

	// FIXME: we maybe want to take this out, and have the Aggregation generate the resource map
	// Implementations need to ensure that one and only one aggregation is created
	Aggregation createAggregation(URI uri) throws OREException;

    Aggregation getAggregation() throws OREException;

	// FIXME: likewise with the above, perhaps we don't set the aggregation, we get the rem from it instead
	void setAggregation(Aggregation aggregation) throws OREException;

	// FIXME: is this a viable thing to do in a Graph?
	void removeAggregation() throws OREException;

	// methods for accessing AggregatedResources

	List<AggregatedResource> getAggregatedResources() throws OREException;

	// methods for doing SPARQL queries on resource maps
	
	List<Triple> doSparql(String sparql);
}
