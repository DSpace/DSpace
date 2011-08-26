/*
 * REMValidatorJena.java
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
package org.dspace.foresite.jena;

import org.dspace.foresite.REMValidator;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREException;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Selector;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/**
 * @Author Richard Jones
 */
public class REMValidatorJena extends REMValidator
{
	public void prepForSerialisation(ResourceMap rem)
			throws OREException
	{
		// first, let the default validator deal with it
		////////////////////////////////////////////////

		super.prepForSerialisation(rem);

		// Now we can do our own preparation on the model, getting rid
		// of the oddities of the graph that are necessary in the working
		// Jena model
		////////////////////////////////////////////////

		Model model = ((ResourceMapJena) rem).getModel();
		URI uri = rem.getURI();

		// set all the appropriate namespace configurations
		model.setNsPrefix(JenaOREConstants.oreNamespacePrefix, ORE.NS);
		model.setNsPrefix(JenaOREConstants.foafNamespacePrefix, FOAF.NS);
		model.setNsPrefix(JenaOREConstants.dcTermsNamespacePrefix, DCTerms.NS);
		model.setNsPrefix(JenaOREConstants.dcNamespacePrefix, DC.NS);
		model.setNsPrefix(JenaOREConstants.owlNamespacePrefix, OWL.NS);

		// prune out the undesired ore:describes declarations
		List<Statement> describes = new ArrayList<Statement>();
		Selector selector = new SimpleSelector(null, ORE.describes, (RDFNode) null);
		StmtIterator itr = model.listStatements(selector);
		while (itr.hasNext())
		{
			Statement statement = itr.nextStatement();
			if (!statement.getSubject().getURI().equals(uri.toString()))
			{
				describes.add(statement);
			}
		}
		model.remove(describes);

		// remove all the OREX terms from the graph
		Selector sel = new SimpleSelector(null, OREX.isAuthoritativeFor, (RDFNode) null);
		StmtIterator sitr = model.listStatements(sel);
		model.remove(sitr);

		// FIXME: it would be nice to allow users to specify "internal" triples which are automatically
		// stripped during this kind of serialisation
	}
}
