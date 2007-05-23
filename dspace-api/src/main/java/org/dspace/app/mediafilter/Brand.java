/*
 * Brand.java
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

import java.io.File;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.imageio.ImageIO;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * Class to attach a footer to an image using ImageMagick.
 * Thanks Ninh Nguyen from the National Library of Australia for providing the source code.
 * This version of the code is basically Ninh's but reorganised a little. Used with permission.
 */

public class Brand
{
	private int brandWidth;
	private int brandHeight;
	private Font font;
	private int xOffset;

	/**
	 * Constructor to set up footer image attributes.
	 *
	 * @param footerWidth length of the footer in pixels
	 * @param footerHeight height of the footer in pixels
	 * @param font font to use for text on the footer
	 * @param xOffset number of pixels text should be indented from left-hand side of footer
	 *
	 */
	public Brand(int brandWidth,
		      int brandHeight,
		      Font font,
		      int xOffset)
	{
		this.brandWidth = brandWidth;
		this.brandHeight = brandHeight;
		this.font = font;
		this.xOffset = xOffset;
	}

	/**
	 * Create the brand image
	 *
	 * @param brandLeftText text that should appear in the bottom left of the image
	 * @param shortLeftText abbreviated form of brandLeftText that will be substituted if
	 *	the image is resized such that brandLeftText will not fit. <code>null</code> if not
	 *	required
	 * @param brandRightText text that should appear in the bottom right of the image
	 *
	 * @return BufferedImage a BufferedImage object describing the brand image file
	 */
	public BufferedImage create(String brandLeftText,
			   String shortLeftText,
			   String brandRightText)
	{
		BrandText[] allBrandText = null;

		BufferedImage brandImage =
			new BufferedImage(brandWidth, brandHeight, BufferedImage.TYPE_INT_RGB);

		if (brandWidth >= 350)
		{
			allBrandText = new BrandText[]
			{
				new BrandText(BrandText.BL, brandLeftText),
				new BrandText(BrandText.BR, brandRightText)
			};
		} 
		else if (brandWidth >= 190)
		{
			allBrandText = new BrandText[]
			{
				new BrandText(BrandText.BL, shortLeftText),
				new BrandText(BrandText.BR, brandRightText)
			};
		}
		else
		{
			allBrandText = new BrandText[]
			{
				new BrandText(BrandText.BR, brandRightText)
			};
		}

		if (allBrandText != null && allBrandText.length > 0)
		{
      		for (int i = 0; i < allBrandText.length; ++i)
			{
        		drawImage(brandImage, allBrandText[i]);
      		}
    	}

		return brandImage;
	}


	/**
	 * do the text placements and preparatory work for the brand image generation
	 *
	 * @param brandImage a BufferedImage object where the image is created
	 * @param identifier and Identifier object describing what text is to be placed in what
	 *	position within the brand
	 */
	private void drawImage(BufferedImage brandImage,
			BrandText brandText)
	{
		int imgWidth = brandImage.getWidth();
		int imgHeight = brandImage.getHeight();

		int bx, by, tx, ty, bWidth, bHeight;

		Graphics2D g2 = brandImage.createGraphics();
		g2.setFont(font);
		FontMetrics fm = g2.getFontMetrics();


		bWidth = fm.stringWidth(brandText.getText()) + xOffset * 2 + 1;
		bHeight = fm.getHeight();

		bx = by = 0;

		if (brandText.getLocation().equals(BrandText.TL))
		{
			bx = 0;
			by = 0;
		}
		else if (brandText.getLocation().equals(BrandText.TR))
		{
			bx = imgWidth - bWidth;
			by = 0;
		}
		else if (brandText.getLocation().equals(BrandText.BL))
		{
			bx = 0;
			by = imgHeight - bHeight;
		}
		else if (brandText.getLocation().equals(BrandText.BR))
		{
			bx = imgWidth - bWidth;
			by = imgHeight - bHeight;
		}

		Rectangle box = new Rectangle(bx, by, bWidth, bHeight);
		tx = bx + xOffset;
		ty = by + fm.getAscent();

    	g2.setColor(Color.black);
		g2.fill(box);
		g2.setColor(Color.white);
		g2.drawString(brandText.getText(), tx, ty);
	}
}
