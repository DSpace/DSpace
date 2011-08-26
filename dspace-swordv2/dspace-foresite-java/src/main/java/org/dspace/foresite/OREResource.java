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
