/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.Bitstream;

/**
 * Wrapper class for bitstreams with Thumbnails associated with them for
 * convenience in the browse system
 * 
 * @author Richard Jones
 *
 */
public class Thumbnail
{
	/** the bitstream that is actually the thumbnail */
	private Bitstream thumb;
	
	/** the original bitstream for which this is the thumbnail */
	private Bitstream original;
	
	/**
	 * Construct a new thumbnail using the two bitstreams
	 * 
	 * @param thumb		the thumbnail bitstream	
	 * @param original	the original bitstream
	 */
	public Thumbnail(Bitstream thumb, Bitstream original)
	{
		this.thumb = thumb;
		this.original = original;
	}

	/**
	 * @return Returns the original.
	 */
	public Bitstream getOriginal()
	{
		return original;
	}

	/**
	 * @param original The original to set.
	 */
	public void setOriginal(Bitstream original)
	{
		this.original = original;
	}

	/**
	 * @return Returns the thumb.
	 */
	public Bitstream getThumb()
	{
		return thumb;
	}

	/**
	 * @param thumb The thumb to set.
	 */
	public void setThumb(Bitstream thumb)
	{
		this.thumb = thumb;
	}


}
