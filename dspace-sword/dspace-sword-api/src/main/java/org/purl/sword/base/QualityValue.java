/**
 * Copyright (c) 2008-2009, Aberystwyth University
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
package org.purl.sword.base;


/**
 * A representation of a quality value.
 * 
 * The quality value must be between 0 and 1, with no more than three digits
 * after the decimal place.
 * 
 * @author Stuart Lewis
 */
public class QualityValue {
	
	/** The quality value. */
	private float quality;
	
	/**
	 * Create a quality value defaulting to 1
	 * 
	 * @throws NumberFormatException thrown if the quality value is invalid according to the SWORD specification
	 */
	public QualityValue() throws NumberFormatException
	{
		// As per the spec, default to value 1
		setQualityValue(1f);
	}
	
	/**
	 * Create a quality value
	 * 
	 * @param q The quality value
	 * @throws NumberFormatException thrown if the quality value is invalid according to the SWORD specification
	 */
	public QualityValue(float q) throws NumberFormatException
	{
		setQualityValue(q);
	}
	
	/**
	 * Set the quality value.
	 * 
	 * @param q The quality value
	 * @throws NumberFormatException thrown if the quality value is invalid according to the SWORD specification
	 */
	public void setQualityValue(float q) throws NumberFormatException
	{
		// Check the float is in range
		if ((q < 0) || (q > 1))
		{
			throw new NumberFormatException("Invalid value - must be between 0 and 1");
		}
		
		// Check there are no more than three digits after the decimal point
		String qStr = "" + q;
        int pos = qStr.indexOf(".");
        if (qStr.substring(pos + 1).length() > 3)
		{
			throw new NumberFormatException("Invalid value - no more than three digits after the decimal point: " + qStr);
		}
		quality = q;
	}
	
	/**
	 * Get the quality value
	 * 
	 * @return the quality value
	 */
	public float getQualityValue()
	{
		return quality;
	}
	
	/**
	 * Get a String representation of this quality value
	 * 
	 * @return The String representation of the quality value
	 */
	public String toString()
	{
		return Float.toString(quality);
	}
	
	/**
	 * A main method with rudimentary tests to check the class
	 */
	/*public static void main(String[] args)
	{
		// Test the class
		
		// Fail - under 0
		try
		{
			QualityValue qv1 = new QualityValue(-0.01f);
			System.out.println("1) Fail: -0.01 passed unexpectedly");
		}
		catch (NumberFormatException nfe)
		{
			System.out.print("1) Pass: -0.01 failed as expected ");
			System.out.println(nfe);
		}
		
		// Fail - over 1
		try
		{
			QualityValue qv2 = new QualityValue(1.01f);
			System.out.println("2) Fail: 1.01 passed unexpectedly");
		}
		catch (NumberFormatException nfe)
		{
			System.out.print("2) Pass: 1.01 failed as expected ");
			System.out.println(nfe);
		}
		
		// Fail - to many decimal points
		try
		{
			QualityValue qv3 = new QualityValue(0.1234f);
			System.out.println("3) Fail: 0.1234 passed unexpectedly");
		}
		catch (NumberFormatException nfe)
		{
			System.out.print("3) Pass: 0.1234 failed as expected ");
			System.out.println(nfe);
		}
		
		// Pass - no decimal places 0
		try
		{
			QualityValue qv4 = new QualityValue(0f);
			System.out.println("4) Pass: 0 passed as expected");
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("4) Fail: 0 failed unexpectedly");
		}
		
		// Pass - no decimal places 1
		try
		{
			QualityValue qv5 = new QualityValue(1f);
			System.out.println("5) Pass: 1 passed as expected");
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("5) Fail: 1 failed unexpectedly");
		}

		// Pass - 3 decimal places
		try
		{
			QualityValue qv6 = new QualityValue(0.123f);
			System.out.print("6) Pass: 0.123 passed as expected - ");
			System.out.println(qv6);
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("6) Fail: 0.123 failed unexpectedly");
		}
		
		// Pass - No value given
		try
		{
			QualityValue qv6 = new QualityValue();
			System.out.print("7) Pass: no value passed as expected - ");
			System.out.println(qv6);
		}
		catch (NumberFormatException nfe)
		{
			System.out.println("7) Fail: no value failed unexpectedly");
		}
	}
     */
}
