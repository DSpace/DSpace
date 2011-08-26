/*
 * ResourceMap.java
 *
 * Copyright (c) 2008, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
