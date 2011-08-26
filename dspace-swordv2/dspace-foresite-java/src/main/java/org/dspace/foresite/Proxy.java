/*
 * Proxy.java
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
