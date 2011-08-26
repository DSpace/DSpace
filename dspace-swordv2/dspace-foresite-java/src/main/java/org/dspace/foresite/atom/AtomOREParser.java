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
package org.dspace.foresite.atom;

import org.dspace.foresite.OREParser;
import org.dspace.foresite.ResourceMap;
import org.dspace.foresite.OREParserException;
import org.dspace.foresite.OREFactory;
import org.dspace.foresite.Aggregation;
import org.dspace.foresite.Agent;
import org.dspace.foresite.OREVocabulary;
import org.dspace.foresite.OREException;
import org.dspace.foresite.AggregatedResource;
import org.dspace.foresite.ReMSerialisation;
import org.jdom.Element;
import org.jdom.Namespace;


import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import com.sun.syndication.io.XmlReader;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.WireFeedInput;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Person;
import com.sun.syndication.feed.atom.Link;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Generator;
import com.sun.syndication.feed.atom.Entry;

/**
 * @Author: Richard Jones
 */
public class AtomOREParser implements OREParser
{
	public ResourceMap parse(InputStream is, URI uri) throws OREParserException
	{
		return null;
	}

	public ResourceMap parse(InputStream is)
            throws OREParserException
    {
        try
        {
            // read in the ATOM document
            XmlReader reader = new XmlReader(is);
            WireFeedInput input = new WireFeedInput();
            Feed atom = (Feed) input.build(reader);

            // mine the atom feed
            URI uri_a = new URI(atom.getId());
            String title = atom.getTitle();
            List<Person> authors = atom.getAuthors();
            List<Link> links = atom.getOtherLinks();
            List<Link> altLinks = atom.getAlternateLinks();
            links.addAll(altLinks); // add all the links together
            List<Category> categories = atom.getCategories();
            List<Element> rdf = (List<Element>) atom.getForeignMarkup();
			Date updated = atom.getUpdated();
            Generator generator = atom.getGenerator();
            String rights = atom.getRights();
            List<Entry> entries = atom.getEntries();

            this.validate(categories);
            URI uri_r = this.getURIR(links);

            // extract the goodies from the embedded RDF
            AggregationRDF ardf = new AggregationRDF();
            RemRDF rrdf = new RemRDF();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Namespace oreNs = Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
            Namespace rdfNs = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
            Namespace dcNs = Namespace.getNamespace("dc", "http://purl.org/dc/terms/");
            for (Element element : rdf)
            {
                String about = element.getAttributeValue("about");
                if (about.equals(uri_a.toString()))
                {
                    ardf.setAbout(about);

                    List<Element> children = element.getChildren("isAggregatedBy", oreNs);
                    List<String> aggregatedBy = new ArrayList<String>();
                    for (Element child : children)
                    {
                        String aggregator = child.getValue().trim();
                        aggregatedBy.add(aggregator);
                    }
                    ardf.setAggregatedBy(aggregatedBy);

                    List<Element> kids = element.getChildren("created", dcNs);
                    List<Date> created = new ArrayList<Date>();
                    for (Element kid : kids)
                    {
                        String create = kid.getValue().trim();
                        Date date = sdf.parse(create);
                        created.add(date);
                    }
                    ardf.setCreated(created);
                }
                else if (about.equals(uri_r.toString()))
                {
                    rrdf.setAbout(about);

                    List<Element> kids = element.getChildren("created", dcNs);
                    List<Date> created = new ArrayList<Date>();
                    for (Element kid : kids)
                    {
                        String create = kid.getValue().trim();
                        Date date = sdf.parse(create);
                        created.add(date);
                    }
                    rrdf.setCreated(created);
                }

				// FIXME: what if the about is about neither?????
			}

            // construct our resource map
            ResourceMap rem = OREFactory.createResourceMap(uri_r);

            if (updated != null)
            {
                rem.setModified(updated);
            }
            if (rights != null)
            {
                rem.setRights(rights);
            }
            rem.setCreated(rrdf.getCreated().get(0));

            if (generator != null)
            {
                Agent remCreator = OREFactory.createAgent();
                remCreator.addName(generator.getValue());
                String genURL = generator.getUrl();
                if (genURL != null)
                {
                    // remCreator.addSeeAlso(new URI(genURL));
                }
                rem.addCreator(remCreator);
            }

            // construct the aggregation
            Aggregation aggregation = OREFactory.createAggregation(uri_a);
            aggregation.addTitle(title);
            aggregation.setCreated(ardf.getCreated().get(0));

            List<ReMSerialisation> rems = new ArrayList<ReMSerialisation>();
            for (String aggregator : ardf.getAggregatedBy())
            {
				// FIXME: can we get the mimetype from anywhere?
				ReMSerialisation serial = new ReMSerialisation();
                serial.setURI(new URI(aggregator));
                rems.add(serial);
            }
            aggregation.setReMSerialisations(rems);

            List<String> types = new ArrayList<String>();
            for (Category category : categories)
            {
                String aggURI = OREVocabulary.aggregation.getUri().toString();
                if (!aggURI.equals(category.getTerm()))
                {
                    // these specify the aggregation type
                    String type = category.getTerm();
                    types.add(type);
                }
            }
            // aggregation.setTypes(types);

            List<Agent> creators = new ArrayList<Agent>();
            for (Person author : authors)
            {
                Agent creator = OREFactory.createAgent();
                creator.addName(author.getName());
                creators.add(creator);
            }
            aggregation.setCreators(creators);

            List<URI> similars = new ArrayList<URI>();
            for (Link link : links)
            {
                String rel = link.getRel();
                if ("related".equals(rel))
                {
                    similars.add(new URI(link.getHref()));
                }
            }
            aggregation.setSimilarTo(similars);

            // process the entries, each of which is an AggregatedResource
            List<AggregatedResource> ars = new ArrayList<AggregatedResource>();
            for (Entry entry : entries)
            {
                // mine the entry
                URI uri_ar = new URI(entry.getId());
                String arTitle = entry.getTitle();
                Date arUpdated = entry.getUpdated();
                List<Link> arAlternates = entry.getAlternateLinks();
                List<Category> arCategories = entry.getCategories();

                AggregatedResource ar = OREFactory.createAggregatedResource(uri_ar);
                ars.add(ar);
            }

            // construct the model
            aggregation.setAggregatedResources(ars);
            rem.setAggregation(aggregation);

            return rem;
        }
        catch (IOException e)
        {
            throw new OREParserException(e);
        }
        catch (FeedException e)
        {
            throw new OREParserException(e);
        }
        catch (URISyntaxException e)
        {
            throw new OREParserException(e);
        }
        catch (OREException e)
        {
            throw new OREParserException(e);
        }
        catch (ParseException e)
        {
            throw new OREParserException(e);
        }
    }

    /**
     * is this a valid resource map (i.e. is there a category saying so)?
     * 
     * @param categories
     * @throws OREParserException
     */
    private void validate(List<Category> categories)
            throws OREParserException
    {
        for (Category category : categories)
        {
            String aggURI = OREVocabulary.aggregation.getUri().toString();
            if (aggURI.equals(category.getTerm()))
            {
                return;
            }
        }

        throw new OREParserException("Passed ATOM document is not an ORE Resource Map; it is missing a valid atom:category statement");
    }

    private URI getURIR(List<Link> links)
            throws OREParserException
    {
        try
        {
            for (Link link : links)
            {
                String rel = link.getRel();
                if ("self".equals(rel))
                {
                    return new URI(link.getHref());
                }
            }
            throw new OREParserException("Passed ATOM document does not contain a URI for the Resource Map; atom:link[@rel='self']");
        }
        catch (URISyntaxException e)
        {
            throw new OREParserException("unable to parse link in atom:link[#rel='self']", e);
        }
    }

    public void configure(Properties properties)
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

	/*
	public ResourceMap parse(InputStream is)
				throws OREParserException
		{
			try
			{
				// read in the ATOM document
				XmlReader reader = new XmlReader(is);
				WireFeedInput input = new WireFeedInput();
				Feed atom = (Feed) input.build(reader);

				// mine the atom feed
				URI uri_a = new URI(atom.getId());
				String title = atom.getTitle();
				List<Person> authors = atom.getAuthors();
				List<Link> links = atom.getOtherLinks();
				List<Link> altLinks = atom.getAlternateLinks();
				links.addAll(altLinks); // add all the links together
				List<Category> categories = atom.getCategories();
				List<Element> rdf = (List<Element>) atom.getForeignMarkup();
				Date updated = atom.getUpdated();
				Generator generator = atom.getGenerator();
				String rights = atom.getRights();
				List<Entry> entries = atom.getEntries();

				this.validate(categories);
				URI uri_r = this.getURIR(links);

				// extract the goodies from the embedded RDF
				AggregationRDF ardf = new AggregationRDF();
				RemRDF rrdf = new RemRDF();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
				Namespace oreNs = Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
				Namespace rdfNs = Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
				Namespace dcNs = Namespace.getNamespace("dc", "http://purl.org/dc/terms/");
				for (Element element : rdf)
				{
					String about = element.getAttributeValue("about");
					if (about.equals(uri_a.toString()))
					{
						ardf.setAbout(about);

						List<Element> children = element.getChildren("isAggregatedBy", oreNs);
						List<String> aggregatedBy = new ArrayList<String>();
						for (Element child : children)
						{
							String aggregator = child.getValue().trim();
							aggregatedBy.add(aggregator);
						}
						ardf.setAggregatedBy(aggregatedBy);

						List<Element> kids = element.getChildren("created", dcNs);
						List<Date> created = new ArrayList<Date>();
						for (Element kid : kids)
						{
							String create = kid.getValue().trim();
							Date date = sdf.parse(create);
							created.add(date);
						}
						ardf.setCreated(created);
					}
					else if (about.equals(uri_r.toString()))
					{
						rrdf.setAbout(about);

						List<Element> kids = element.getChildren("created", dcNs);
						List<Date> created = new ArrayList<Date>();
						for (Element kid : kids)
						{
							String create = kid.getValue().trim();
							Date date = sdf.parse(create);
							created.add(date);
						}
						rrdf.setCreated(created);
					}
				}

				// construct our resource map
				ResourceMap rem = OREFactory.createResourceMap(uri_r);

				if (updated != null)
				{
					rem.setModified(updated);
				}
				if (rights != null)
				{
					rem.setRights(rights);
				}
				rem.setCreated(rrdf.getCreated().get(0));

				if (generator != null)
				{
					Agent remCreator = OREFactory.createAgent();
					remCreator.addName(generator.getValue());
					String genURL = generator.getUrl();
					if (genURL != null)
					{
						// remCreator.addSeeAlso(new URI(genURL));
					}
					rem.addCreator(remCreator);
				}

				// construct the aggregation
				Aggregation aggregation = OREFactory.createAggregation(uri_a);
				aggregation.addTitle(title);
				aggregation.setCreated(ardf.getCreated().get(0));

				List<ReMSerialisation> rems = new ArrayList<ReMSerialisation>();
				for (String aggregator : ardf.getAggregatedBy())
				{
					// FIXME: can we get the mimetype from anywhere?
					ReMSerialisation serial = new ReMSerialisation();
					serial.setURI(new URI(aggregator));
					rems.add(serial);
				}
				aggregation.setReMSerialisations(rems);

				List<String> types = new ArrayList<String>();
				for (Category category : categories)
				{
					String aggURI = OREVocabulary.aggregation.getUri().toString();
					if (!aggURI.equals(category.getTerm()))
					{
						// these specify the aggregation type
						String type = category.getTerm();
						types.add(type);
					}
				}
				// aggregation.setTypes(types);

				List<Agent> creators = new ArrayList<Agent>();
				for (Person author : authors)
				{
					Agent creator = OREFactory.createAgent();
					creator.addName(author.getName());
					creators.add(creator);
				}
				aggregation.setCreators(creators);

				List<URI> similars = new ArrayList<URI>();
				for (Link link : links)
				{
					String rel = link.getRel();
					if ("related".equals(rel))
					{
						similars.add(new URI(link.getHref()));
					}
				}
				aggregation.setSimilarTo(similars);

				// process the entries, each of which is an AggregatedResource
				List<AggregatedResource> ars = new ArrayList<AggregatedResource>();
				for (Entry entry : entries)
				{
					// mine the entry
					URI uri_ar = new URI(entry.getId());
					String arTitle = entry.getTitle();
					Date arUpdated = entry.getUpdated();
					List<Link> arAlternates = entry.getAlternateLinks();
					List<Category> arCategories = entry.getCategories();

					AggregatedResource ar = OREFactory.createAggregatedResource(uri_ar);
					ars.add(ar);
				}

				// construct the model
				aggregation.setAggregatedResources(ars);
				rem.setAggregation(aggregation);

				return rem;
			}
			catch (IOException e)
			{
				throw new OREParserException(e);
			}
			catch (FeedException e)
			{
				throw new OREParserException(e);
			}
			catch (URISyntaxException e)
			{
				throw new OREParserException(e);
			}
			catch (OREException e)
			{
				throw new OREParserException(e);
			}
			catch (ParseException e)
			{
				throw new OREParserException(e);
			}
		}*/

	///////////////////////////////////////////////////////////////////
	// Privately used classes
	///////////////////////////////////////////////////////////////////
	
	private class AggregationRDF
    {
        private String about;

        private List<String> isAggregatedBy;

        private List<Date> created;

        public String getAbout()
        {
            return about;
        }

        public void setAbout(String about)
        {
            this.about = about;
        }

        public List<String> getAggregatedBy()
        {
            return isAggregatedBy;
        }

        public void setAggregatedBy(List<String> aggregatedBy)
        {
            isAggregatedBy = aggregatedBy;
        }

        public List<Date> getCreated()
        {
            return created;
        }

        public void setCreated(List<Date> created)
        {
            this.created = created;
        }
    }

    private class RemRDF
    {
        private String about;

        private List<Date> created;

        public String getAbout()
        {
            return about;
        }

        public void setAbout(String about)
        {
            this.about = about;
        }

        public List<Date> getCreated()
        {
            return created;
        }

        public void setCreated(List<Date> created)
        {
            this.created = created;
        }
    }
}
