/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;
import org.purl.sword.atom.Content;
import org.purl.sword.atom.ContentType;
import org.purl.sword.atom.InvalidMediaTypeException;
import org.purl.sword.atom.Link;
import org.purl.sword.atom.Rights;
import org.purl.sword.atom.Summary;
import org.purl.sword.atom.Title;
import org.apache.log4j.Logger;

import java.sql.SQLException;

/**
 * @author Richard Jones
 *
 * Class to generate an ATOM Entry document for a DSpace Item
 */
public class ItemEntryGenerator extends DSpaceATOMEntry
{
	/** logger */
	private static Logger log = Logger.getLogger(ItemEntryGenerator.class);

	protected ItemEntryGenerator(SWORDService service)
	{
		super(service);
	}

	/**
	 * Add all the subject classifications from the bibliographic
	 * metadata.
	 *
	 */
	protected void addCategories()
	{
		Metadatum[] dcv = item.getMetadataByMetadataString("dc.subject.*");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				entry.addCategory(dcv[i].value);
			}
		}
	}

		/**
	 * Set the content type that DSpace received.  This is just
	 * "application/zip" in this default implementation.
	 *
	 */
	protected void addContentElement()
			throws DSpaceSWORDException
	{
		// get the things we need out of the service
		SWORDUrlManager urlManager = swordService.getUrlManager();

		try
		{
			if (!this.deposit.isNoOp())
			{
				String handle = "";
				if (item.getHandle() != null)
				{
					handle = item.getHandle();
				}

				if (handle != null && !"".equals(handle))
				{
					boolean keepOriginal = ConfigurationManager.getBooleanProperty("sword-server", "keep-original-package");
					String swordBundle = ConfigurationManager.getProperty("sword-server", "bundle.name");
					if (swordBundle == null || "".equals(swordBundle))
					{
						swordBundle = "SWORD";
					}

					// if we keep the original, then expose this as the content element
					// otherwise, expose the unpacked version
					if (keepOriginal)
					{
						Content con = new Content();
						Bundle[] bundles = item.getBundles(swordBundle);
						if (bundles.length > 0)
						{
							Bitstream[] bss = bundles[0].getBitstreams();
							for (int i = 0; i < bss.length; i++)
							{
								BitstreamFormat bf = bss[i].getFormat();
								String format = "application/octet-stream";
								if (bf != null)
								{
									format = bf.getMIMEType();
								}
								con.setType(format);

								// calculate the bitstream link.
								String bsLink = urlManager.getBitstreamUrl(bss[i]);
								con.setSource(bsLink);

								entry.setContent(con);
							}
						}
					}
					else
					{
						// return a link to the DSpace entry page
						Content content = new Content();
						content.setType("text/html");
						content.setSource(HandleManager.getCanonicalForm(handle));
						entry.setContent(content);
					}
				}
			}
		}
		catch (InvalidMediaTypeException e)
		{
			// do nothing; we'll live without the content type declaration!
		}
		catch (SQLException e)
		{
			log.error("caught exception: ", e);
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Add the identifier for the item.  If the item object has
	 * a handle already assigned, this is used, otherwise, the
	 * passed handle is used.  It is set in the form that
	 * they can be used to access the resource over http (i.e.
	 * a real URL).
	 */
	protected void addIdentifier()
	{
		// it's possible that the item hasn't been assigned a handle yet
		if (!this.deposit.isNoOp())
		{
			String handle = "";
			if (item.getHandle() != null)
			{
				handle = item.getHandle();
			}

			if (handle != null && !"".equals(handle))
			{
				entry.setId(HandleManager.getCanonicalForm(handle));
				return;
			}
		}

		// if we get this far, then we just use the dspace url as the
		// property
		String cfg = ConfigurationManager.getProperty("dspace.url");
		entry.setId(cfg);

		// FIXME: later on we will maybe have a workflow page supplied
		// by the sword interface?
	}

	/**
	 * Add links associated with this item.
	 *
	 */
	protected void addLinks()
		throws DSpaceSWORDException
	{
		SWORDUrlManager urlManager = swordService.getUrlManager();

		try
		{
			// if there is no handle, we can't generate links
			String handle = "";
			if (item.getHandle() != null)
			{
				handle = item.getHandle();
			}
			else
			{
				return;
			}

			// link to all the files in the item
			Bundle[] bundles = item.getBundles("ORIGINAL");
			for (int i = 0; i < bundles.length ; i++)
			{
				Bitstream[] bss = bundles[i].getBitstreams();
				for (int j = 0; j < bss.length; j++)
				{
					Link link = new Link();
					String url = urlManager.getBitstreamUrl(bss[j]);
					link.setHref(url);
					link.setRel("part");

					BitstreamFormat bsf = bss[j].getFormat();
					if (bsf != null)
					{
						link.setType(bsf.getMIMEType());
					}

					entry.addLink(link);
				}
			}

			// link to the item splash page
			Link splash = new Link();
			splash.setHref(HandleManager.getCanonicalForm(handle));
			splash.setRel("alternate");
			splash.setType("text/html");
			entry.addLink(splash);
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Add the date of publication from the bibliographic metadata
	 *
	 */
	protected void addPublishDate()
	{
		Metadatum[] dcv = item.getMetadataByMetadataString("dc.date.issued");
		if (dcv != null && dcv.length == 1)
        {
            entry.setPublished(dcv[0].value);
        }
	}


		/**
	 * Add rights information.  This attaches an href to the URL
	 * of the item's licence file
	 *
	 */
	protected void addRights()
			throws DSpaceSWORDException
	{
		SWORDUrlManager urlManager = swordService.getUrlManager();
		
		try
		{
			String handle = this.item.getHandle();

			// if there's no handle, we can't give a link
			if (handle == null || "".equals(handle))
			{
				return;
			}

			String base = ConfigurationManager.getProperty("dspace.url");

			// if there's no base URL, we are stuck
			if (base == null)
			{
				return;
			}

			StringBuilder rightsString = new StringBuilder();
			Bundle[] bundles = item.getBundles("LICENSE");
			for (int i = 0; i < bundles.length; i++)
			{
				Bitstream[] bss = bundles[i].getBitstreams();
				for (int j = 0; j < bss.length; j++)
				{
					String url = urlManager.getBitstreamUrl(bss[j]);
					rightsString.append(url + " ");
				}
			}

			Rights rights = new Rights();
			rights.setContent(rightsString.toString());
			rights.setType(ContentType.TEXT);
			entry.setRights(rights);
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

	/**
	 * Add the summary/abstract from the bibliographic metadata
	 *
	 */
	protected void addSummary()
	{
		Metadatum[] dcv = item.getMetadataByMetadataString("dc.description.abstract");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				Summary summary = new Summary();
				summary.setContent(dcv[i].value);
				summary.setType(ContentType.TEXT);
				entry.setSummary(summary);
			}
		}
	}

	/**
	 * Add the title from the bibliographic metadata
	 *
	 */
	protected void addTitle()
	{
		Metadatum[] dcv = item.getMetadataByMetadataString("dc.title");
		if (dcv != null)
		{
			for (int i = 0; i < dcv.length; i++)
			{
				Title title = new Title();
				title.setContent(dcv[i].value);
				title.setType(ContentType.TEXT);
				entry.setTitle(title);
			}
		}
	}

	/**
	 * Add the date that this item was last updated
	 *
	 */
	protected void addLastUpdatedDate()
	{
		String config = ConfigurationManager.getProperty("sword-server", "updated.field");
		Metadatum[] dcv = item.getMetadataByMetadataString(config);
		if (dcv != null && dcv.length == 1)
        {
            DCDate dcd = new DCDate(dcv[0].value);
            entry.setUpdated(dcd.toString());
        }
	}
}
