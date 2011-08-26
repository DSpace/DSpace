/*
 * AggregatedResource.java
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
