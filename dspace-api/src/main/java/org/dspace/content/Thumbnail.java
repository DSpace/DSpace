/*
 * Thumbnail.java
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
