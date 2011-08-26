/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.foresite;

import java.net.URI;

/**
 * @Author Richard Jones
 */
public class TripleSelector
{
    private URI subjectURI;

    private Predicate predicate;

    private URI objectURI;

    private Object literal;

	public TripleSelector() { }

	public TripleSelector(URI subjectURI, URI predicateURI, URI objectURI)
	{
		this.setSubjectURI(subjectURI);
		this.setObjectURI(objectURI);
		Predicate predicate = new Predicate();
		predicate.setURI(predicateURI);
		this.setPredicate(predicate);
	}

	public TripleSelector(URI subjectURI, URI predicateURI, Object object)
	{
		this.setSubjectURI(subjectURI);
		this.setLiteral(object);
		Predicate predicate = new Predicate();
		predicate.setURI(predicateURI);
		this.setPredicate(predicate);
	}

	public URI getSubjectURI()
    {
        return subjectURI;
    }

    public void setSubjectURI(URI subjectURI)
    {
        this.subjectURI = subjectURI;
    }

    public Predicate getPredicate()
    {
        return predicate;
    }

    public void setPredicate(Predicate predicate)
    {
        this.predicate = predicate;
    }

    public URI getObjectURI()
    {
        return objectURI;
    }

    public void setObjectURI(URI objectURI)
    {
        this.objectURI = objectURI;
    }

    public Object getLiteral()
    {
        return literal;
    }

    public void setLiteral(Object literal)
    {
        this.literal = literal;
    }
}
