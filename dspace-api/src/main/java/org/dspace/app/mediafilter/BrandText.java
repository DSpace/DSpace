/*
 * BrandText.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2006, Hewlett-Packard Company and Massachusetts
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
package org.dspace.app.mediafilter;

/**
 * Identifier class, holding a single item of text and its location
 * within a rectangular areas. Allowable locations are any of the four corners.
 * This is a copy of Picture Australia's PiObj class re-organised with methods.
 * Thanks to Ninh Nguyen at the National Library for providing the original source.
 */
class BrandText
{
	/** Bottom Left */
	public static final String BL = "bl";
	/** Bottom Right */
	public static final String BR = "br";
	/** Top Left */
	public static final String TL = "tl";
	/** Top Right */
	public static final String TR = "tr";

	private String location;
	private String text;

	/**
	 * Constructor for an Identifier object containing a text string and
	 * its location within a rectangular area.
	 *
	 * @param location one of the class location constants e.g. <code>Identifier.BL</code>
	 * @param the text associated with the location
	 */
	public BrandText(String location, String text)
	{
		this.location = location;
		this.text = text;
	}

	/**
	 * get the location the text of the Identifier object is associated with
	 *
	 * @return String one the class location constants e.g. <code>Identifier.BL</code>
	 */
	public String getLocation()
	{
		return location;
	}


	/**
	 * get the text associated with the Identifier object
	 *
	 * @return String the text associated with the Identifier object
	 */
	public String getText()
	{
		return text;
	}


	/**
	 * set the location associated with the Identifier object
	 *
	 * @param location one of the class location constants
	 */
	public void setLocation(String location)
	{
		this.location = location;
	}


	/**
	 * set the text associated with the Identifier object
	 *
	 * @param text any text string (typically a branding or identifier)
	 */
	public void setText(String text)
	{
		this.text = text;
	}
}
