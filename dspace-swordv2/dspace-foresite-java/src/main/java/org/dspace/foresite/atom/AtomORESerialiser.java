/*
 * AtomORESerialiser.java
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
package org.dspace.foresite.atom;

import org.dspace.foresite.ORESerialiser;
import org.dspace.foresite.ResourceMapDocument;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.ORESerialiserException;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.OREException;
import org.dspace.foresite.ReMSerialisation;
import org.dspace.foresite.Triple;
import org.dspace.foresite.Agent;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.Proxy;
import org.jdom.Element;
import org.jdom.Namespace;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.net.URI;
import java.io.StringWriter;
import java.io.IOException;

import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.atom.Generator;
import com.sun.syndication.feed.atom.Entry;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.io.WireFeedOutput;
import com.sun.syndication.io.FeedException;

/**
 * @Author Richard Jones
 */
public class AtomORESerialiser implements ORESerialiser
{
    public void configure(Properties properties)
    {
        
    }

	public ResourceMapDocument serialiseRaw(ResourceMap rem) throws ORESerialiserException
	{
		return null;
	}

	public ResourceMapDocument serialise(ResourceMap rem)
			throws ORESerialiserException
	{
		try
		{
			Namespace rdfNS = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");

			// get the relevant info from the REM
			URI uri_r = rem.getURI();
			Aggregation agg = rem.getAggregation();
			List<URI> similarTo = agg.getSimilarTo();
			List<URI> seeAlso = agg.getSeeAlso();
			List<ReMSerialisation> otherRems = agg.getReMSerialisations();
			List<AggregatedResource> ars = agg.getAggregatedResources();

			// build the ATOM objects
			Feed atom = new Feed("atom_1.0");
			List<Entry> entries = new ArrayList<Entry>();
			List<Link> relateds = new ArrayList<Link>();
			List<Link> alternates = new ArrayList<Link>();

			// Do the cross-walk
			////////////////////

			// atom:id :: Aggregation URI
			atom.setId(agg.getURI().toString());

			// atom:link@rel=related :: ore:similarTo (& rdfs:seeAlso)
			// FIXME: rdfs:seeAlso should go into the rdf section of the document
			for (URI similar : similarTo)
			{
				Link link = new Link();
				link.setRel("related");
				link.setHref(similar.toString());
				relateds.add(link);
			}
			for (URI also : seeAlso)
			{
				Link link = new Link();
				link.setRel("related");
				link.setHref(also.toString());
				relateds.add(link);
			}

			// atom:link@rel=alternate :: ore:isDescribedBy for other Resource Maps
			for (ReMSerialisation serial : otherRems)
			{
				if (!serial.getURI().equals(uri_r))
				{
					Link link = new Link();
					link.setRel("alternate");
					link.setHref(serial.getURI().toString());
					alternates.add(link);
				}
			}

			// rdf:Description about=Aggregation URI :: all remaining RDF from the Aggregation
			List<Triple> aggTriples = agg.listTriples();
			Element aggElement = new Element("Description", rdfNS);
			aggElement.setAttribute("about", agg.getURI().toString());
			for (Triple triple : aggTriples)
			{
				// atom:**** :: anything in the atom: namespace

				// all other things are straight RDF

				// FIXME: we need to filter this list of triples to not insert duplicate
				// information.  Not hard, just boring, do it later ;)
			}

			// FIXME: this actually takes an argument of List<Element>
			// atom.setForeignMarkup(aggTriples);

			// atom:link@rel=self :: REM URI
			Link self = new Link();
			self.setRel("self");
			self.setHref(uri_r.toString());
			relateds.add(self);

			// atom:generator :: REM creator
			// FIXME: how do we deal with multiple generators
			List<Agent> remCreators = rem.getCreators();
			for (Agent creator : remCreators)
			{
				Generator generator = new Generator();
				List<String> names = creator.getNames();

				// FIXME: what do we do about URLs?  There needs to be something in my API to deal
				// with blank or non blank nodes for creators.
				//
				// generator.setUrl();

				// FIXME: what do we do about multiple foaf:name entries
				for (String name : names)
				{
					// FIXME: this weill register only the LAST name entry
					generator.setValue(name);
				}

				// FIXME: this will register only the LAST generator
				atom.setGenerator(generator);
			}

			// atom:updated :: REM modified date
			Date modified = rem.getModified();
			atom.setUpdated(modified);

			// atom:rights :: REM Rights
			String rights = rem.getRights();
			atom.setRights(rights);

			// rdf:Description about=REM URI :: all remaining RDF from the REM
			List<Triple> remTriples = agg.listTriples();
			Element remElement = new Element("Description", rdfNS);
			remElement.setAttribute("about", agg.getURI().toString());
			for (Triple triple : remTriples)
			{
				// atom:**** :: anything in the atom: namespace

				// all other things are straight RDF

				// FIXME: we need to filter this list of triples to not insert duplicate
				// information.  Not hard, just boring, do it later ;)
			}

			for (AggregatedResource ar : ars)
			{
				Entry entry = new Entry();
				List<Link> entryAlt = new ArrayList<Link>();
				List<Link> entryOther = new ArrayList<Link>();
				List<Category> categories = new ArrayList<Category>();

				// atom:entry/atom:id :: proxy URI if exists
				// atom:entry/atom:link@rel="via" :: Proxy Lineage URI if exists
				Proxy proxy = ar.getProxy();
				if (proxy != null)
				{
					entry.setId(proxy.getURI().toString());

					URI lineage = proxy.getLineage();
					if (lineage != null)
					{
						Link link = new Link();
						link.setRel("via");
						link.setHref(lineage.toString());
						entryOther.add(link);
					}
				}

				// atom:entry/atom:link@rel="alternate" :: Aggregated Resource URI
				Link alt = new Link();
				alt.setRel("alternate");
				alt.setHref(ar.getURI().toString());
				entryAlt.add(alt);

				// rdf:Description about=REM URI :: all remaining RDF from the REM
				List<Triple> arTriples = ar.listTriples();
				Element arElement = new Element("Description", rdfNS);
				arElement.setAttribute("about", ar.getURI().toString());
				for (Triple triple : arTriples)
				{
					// atom:entry/atom:**** :: anything in the atom: namespace

					// all other things are straight RDF

					// FIXME: we need to filter this list of triples to not insert duplicate
					// information.  Not hard, just boring, do it later ;)
				}

				// atom:entry/atom:link@rel="related" :: Other Aggregation URIs for this Aggregated Resource
				List<URI> otherAggs = ar.getAggregations();
				for (URI other : otherAggs)
				{
					Link link = new Link();
					link.setRel("related");
					link.setHref(other.toString());
					entryOther.add(link);
				}

				// atom:entry/atom:category :: other ResourceMaps for this aggregated resource
				List<URI> entryOtherRems = ar.getResourceMaps();
				for (URI other : entryOtherRems)
				{
					Category category = new Category();
					category.setTerm("http://www.openarchives.org/ore/terms/Aggregation");

					// FIXME: where do we put the URI?

					categories.add(category);
				}

				// add the various lists to the entry
				entry.setAlternateLinks(entryAlt);
				entry.setOtherLinks(entryOther);
				entry.setCategories(categories);

				// add the entry to the list of entries
				entries.add(entry);
			}

			// assemble all the lists into the ATOM document
			atom.setOtherLinks(relateds);
			atom.setAlternateLinks(alternates);
			atom.setEntries(entries);

			// write the ATOM feed document to a string
			StringWriter writer = new StringWriter();
			WireFeedOutput output = new WireFeedOutput();
			output.output(atom, writer);

			// build and return the resource map document
			ResourceMapDocument rmd = new ResourceMapDocument();
			rmd.setSerialisation(writer.toString());
			rmd.setMimeType("application/atom+xml");
			rmd.setUri(uri_r);

			return rmd;
		}
		catch (OREException e)
		{
			throw new ORESerialiserException(e);
		}
		catch(IOException e)
		{
			throw new ORESerialiserException(e);
		}
		catch (FeedException e)
		{
			throw new ORESerialiserException(e);
		}
	}
}
