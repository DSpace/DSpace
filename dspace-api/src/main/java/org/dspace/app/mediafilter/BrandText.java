/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
