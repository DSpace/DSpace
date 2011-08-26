/*
 * AtomOREParser.java
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
public interface OREResource
{
	/**
	 * Get the URI representing the resource
	 * 
	 * @return
	 * @throws OREException
	 */
	URI getURI() throws OREException;

	// methods to deal with arbitrary relationships associated with resources

	/**
	 * List all of the triples directly descended from the resource type.
	 * That is: all the triples whose Subject is the resource
	 *
	 * @return
	 * @throws OREException
	 */
	List<Triple> listTriples() throws OREException;

	/**
	 * List all the triples directly descended from the resource type which match
	 * the selection criteria.  This means that the Subject of the TripleSelector
	 * will be set to the ORE Resource, irrespective of what is specified there
	 *
	 * @param selector
	 * @return
	 * @throws OREException
	 */
	List<Triple> listTriples(TripleSelector selector) throws OREException;

	/**
	 * List all triples associated with the whole ORE graph as known about by
	 * the resource in its current environment.
	 *
	 * @return
	 * @throws OREException
	 */
	List<Triple> listAllTriples() throws OREException;

	/**
	 * List all triples associated with the whole ORE graph as known about by
	 * the resource in its current environment, which match the selection
	 * criteria
	 * 
	 * @param selector
	 * @return
	 * @throws OREException
	 */
	List<Triple> listAllTriples(TripleSelector selector) throws OREException;

	void addTriples(List<Triple> relationships) throws OREException;

    void addTriple(Triple relationship) throws OREException;

    void removeTriple(Triple triple) throws OREException;

	// methods to deal with arbitrary relationships associated with /this/ resource

	Triple createTriple(Predicate pred, OREResource resource) throws OREException;

    Triple createTriple(Predicate pred, URI uri) throws OREException;

    Triple createTriple(Predicate pred, Object literal) throws OREException;

	// method to deal with deleting all content

	void empty() throws OREException;  // STOP THINKING IN HIERARCHIES

	// methods for dealing with creators of resource maps

	List<Agent> getCreators();

    void setCreators(List<Agent> creators);

    void addCreator(Agent creator);

    void clearCreators();

	// methods for dealing with other Person Constructs

	// FIXME: IMPLEMENT
	//List<Agent> getAgent(URI relationship) throws OREException;

	//void setAgents(URI relationship) throws OREException;

	//void addAgent(URI relationship) throws OREException;

	//void clearAgents(URI relationship) throws OREException;
}
