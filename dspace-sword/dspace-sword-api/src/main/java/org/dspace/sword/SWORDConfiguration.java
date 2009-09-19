/* SWORDConfiguration.java
 *
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.dspace.sword;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.BitstreamFormat;
import org.purl.sword.base.SWORDErrorException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.sql.SQLException;

/**
 * @author Richard Jones
 *
 * Class to represent the principal configurations of the sword
 * service being offered.  Not all configuration is available through
 * this class, but the most useful common options, and those with
 * default values are available
 *
 * Note that changes to values via the api will not be persisted
 * between sword requests.
 *
 * For detailed descriptions of configuration values, see the sword
 * configuration documentation
 *
 */
public class SWORDConfiguration
{
	/** logger */
	private static Logger log = Logger.getLogger(SWORDConfiguration.class);

	/** whether we can support noOp */
	private boolean noOp = true;

	/** whether we can be verbose */
	private boolean verbose = true;

	/** what our default max upload size is */
	private int maxUploadSize = -1;

	/** do we support mediation */
	private boolean mediated = false;

	/** should we keep the original package */
	private boolean keepOriginal = false;

	/** item bundle in which sword deposits are stored */
	private String swordBundle = "SWORD";

    /** Accepted formats */
    private List<String> swordaccepts;

	/**
	 * Initialise the sword configuration.  It is at this stage that the
	 * object will interrogate the DSpace Configuration for details
	 */
	public SWORDConfiguration()
	{
		// set the max upload size
		int mus = ConfigurationManager.getIntProperty("sword.max-upload-size");
		if (mus > 0)
		{
			this.maxUploadSize = mus;
		}

		// set the mediation value
		this.mediated = ConfigurationManager.getBooleanProperty("sword.on-behalf-of.enable");

		// find out if we keep the original
		this.keepOriginal = ConfigurationManager.getBooleanProperty("sword.keep-original-package");

		// get the sword bundle
		String bundle = ConfigurationManager.getProperty("sword.bundle.name");
		if (bundle != null && "".equals(bundle))
		{
			this.swordBundle = bundle;
		}

        // Get the accepted formats
        String acceptsProperty = ConfigurationManager.getProperty("sword.accepts");
        swordaccepts = new ArrayList<String>();
        if (acceptsProperty == null)
        {
            acceptsProperty = "application/zip";
        }
        for (String element : acceptsProperty.split(","))
        {
            swordaccepts.add(element.trim());
        }
	}

	/**
	 * Get the bundle name that sword will store its original deposit packages in, when
	 * storing them inside an item
	 * @return
	 */
	public String getSwordBundle()
	{
		return swordBundle;
	}

	/**
	 * Set the bundle name that sword will store its original deposit packages in, when
	 * storing them inside an item
	 * @param swordBundle
	 */
	public void setSwordBundle(String swordBundle)
	{
		this.swordBundle = swordBundle;
	}

	/**
	 * is this a no-op deposit
	 * @return
	 */
	public boolean isNoOp()
	{
		return noOp;
	}

	/**
	 * set whether this is a no-op deposit
	 *
	 * @param noOp
	 */
	public void setNoOp(boolean noOp)
	{
		this.noOp = noOp;
	}

	/**
	 * is this a verbose deposit
	 * @return
	 */
	public boolean isVerbose()
	{
		return verbose;
	}

	/**
	 * set whether this is a verbose deposit
	 * @param verbose
	 */
	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}

	/**
	 * what is the max upload size (in bytes) for the sword interface
	 * @return
	 */
	public int getMaxUploadSize()
	{
		return maxUploadSize;
	}

	/**
	 * set the max uplaod size (in bytes) for the sword interface
	 * @param maxUploadSize
	 */
	public void setMaxUploadSize(int maxUploadSize)
	{
		this.maxUploadSize = maxUploadSize;
	}

	/**
	 * does the server support mediated deposit (aka on-behalf-of)
	 * @return
	 */
	public boolean isMediated()
	{
		return mediated;
	}

	/**
	 * set whether the server supports mediated deposit (aka on-behalf-of)
	 * @param mediated
	 */
	public void setMediated(boolean mediated)
	{
		this.mediated = mediated;
	}

	/**
	 * should the repository keep the original package
	 * @return
	 */
	public boolean isKeepOriginal()
	{
		return keepOriginal;
	}

	/**
	 * set whether the repository should keep copies of the original package
	 * @param keepOriginal
	 */
	public void setKeepOriginal(boolean keepOriginal)
	{
		this.keepOriginal = keepOriginal;
	}

	/**
	 * Get the list of mime types that the given dspace object will
	 * accept as packages
	 *
	 * @param context
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public List<String> getAccepts(Context context, DSpaceObject dso)
			throws DSpaceSWORDException
	{
		try
		{
			List<String> accepts = new ArrayList<String>();
			if (dso instanceof Collection)
			{
				for (String format : swordaccepts)
                {
                    accepts.add(format);
                }
			}
			else if (dso instanceof Item)
			{
				BitstreamFormat[] bfs = BitstreamFormat.findNonInternal(context);
				for (int i = 0; i < bfs.length; i++)
				{
					accepts.add(bfs[i].getMIMEType());
				}
			}

			return accepts;
		}
		catch (SQLException e)
		{
			throw new DSpaceSWORDException(e);
		}
	}

    /**
	 * Get the list of mime types that a Collection will accept as packages
	 *
	 * @return the list of mime types
	 * @throws DSpaceSWORDException
	 */
	public List<String> getCollectionAccepts() throws DSpaceSWORDException
	{
        List<String> accepts = new ArrayList<String>();
        for (String format : swordaccepts)
        {
            accepts.add(format);
        }
        return accepts;
	}

	/**
	 * Get a map of packaging URIs to Q values for the packaging types which
	 * the given collection will accept.
	 *
	 * The URI should be a unique identifier for the packaging type,
	 * such as:
	 *
	 * http://purl.org/net/sword-types/METSDSpaceSIP
	 *
	 * and the Q value is a floating point between 0 and 1 which defines
	 * how much  the server "likes" this packaging type
	 *
	 * @param col
	 * @return
	 */
	public Map<String, Float> getAcceptPackaging(Collection col)
    {
        Map<String, String> identifiers = new HashMap<String, String>();
        Map<String, String> qs = new HashMap<String, String>();
		String handle = col.getHandle();

		// build the holding maps of identifiers and q values
        Properties props = ConfigurationManager.getProperties();
        Set keyset = props.keySet();
        for (Object keyObj : keyset)
        {
			String sw = "sword.accept-packaging.";

            if (!(keyObj instanceof String))
            {
                continue;
            }
            String key = (String) keyObj;

            if (!key.startsWith(sw))
            {
                continue;
            }

			// extract the configuration into the holding Maps
            String suffix = key.substring(sw.length());

            String[] bits = suffix.split("\\.");
            if (bits.length == 2)
            {
                // global settings
                String value = props.getProperty(key);
                if (bits[1].equals("identifier"))
                {
                    identifiers.put(bits[0], value);
                }
                else if (bits[1].equals("q"))
                {
                    qs.put(bits[0], value);
                }
            }
            if (bits.length == 3)
            {
                // collection settings
				if (bits[0].equals(handle))
				{
					// this is configuration for our collection
					String value = props.getProperty(key);
					if (bits[2].equals("identifier"))
					{
						identifiers.put(bits[1], value);
					}
					else if (bits[2].equals("q"))
					{
						qs.put(bits[1], value);
					}
				}
            }
        }

		// merge the holding maps into the Accept Packaging settings
		Map<String, Float> ap = new HashMap<String, Float>();
		for (String ik : identifiers.keySet())
		{
			String id = identifiers.get(ik);
			String qv = qs.get(ik);
			Float qf = Float.parseFloat(qv);
			ap.put(id, qf);
		}

		return ap;
    }

	/**
	 * is the given packaging/media type supported by the given dspace object
	 *
	 * @param mediaType
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 * @throws SWORDErrorException
	 */
	public boolean isSupportedMediaType(String mediaType, DSpaceObject dso)
			throws DSpaceSWORDException, SWORDErrorException
	{
		if (mediaType == null || "".equals(mediaType))
		{
			return true;
		}

		if (dso instanceof Collection)
		{
			Map<String, Float> accepts = this.getAcceptPackaging((Collection) dso);
			for (String accept : accepts.keySet())
			{
				if (accept.equals(mediaType))
				{
					return true;
				}
			}
		}
		else if (dso instanceof Item)
		{
			// items don't unpackage, so they don't care what the media type is
			return true;
		}
		return false;
	}

	/**
	 * is the given content mimetype acceptable to the given dspace object
	 * @param context
	 * @param type
	 * @param dso
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public boolean isAcceptableContentType(Context context, String type, DSpaceObject dso)
			throws DSpaceSWORDException
	{
		List<String> accepts = this.getAccepts(context, dso);
		return accepts.contains(type);
	}

	/**
	 * Get the temp directory for storing files during deposit
	 * 
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public String getTempDir()
			throws DSpaceSWORDException
	{
		String tempDir = ConfigurationManager.getProperty("upload.temp.dir");
		if (tempDir == null || "".equals(tempDir))
		{
			throw new DSpaceSWORDException("There is no temporary upload directory specified in configuration: upload.temp.dir");
		}
		return tempDir;
	}
}
