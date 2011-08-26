/*
 * Behaviour09.java
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
package org.dspace.foresite.test;

import org.junit.Test;

/**
 * @Author Richard Jones
 */
public class Behaviour09
{
	/**
	 * In this method we want to test whether the creation of Aggregation
	 * and ResourceMap object via eachother is behaving correctly with regard
	 * to the common/shared underlying model
	 */
	@Test
	public void testResourceMapCreation()
	{
		// Need to do something like this
		//
		// Aggregation aggregation = OREFactory.createAggregation(uri);
		// ResourceMap rem = aggregation.createResourceMap(uri2);
		//
		// then do some testing on that
	}

	/**
	 * Test to see if the REM API behaves as expected
	 */
	@Test
	public void testRemBehaviour()
	{

	}

	/**
	 * Test to see if the Aggregation API behaves as expected
	 */
	@Test
	public void testAggregationBehaviour()
	{

	}

	/**
	 * Test to see if the AggregatedResource API behaves as expected
	 */
	@Test
	public void testAggregatedResourceBehaviour()
	{

	}

	/**
	 * Test to see if the Proxy API behaves as expected
	 */
	@Test
	public void testProxyBehaviour()
	{
		
	}

	/**
	 * Test the behaviour of the underlying graph to make sure that
	 * all resource maps specify the ore:describes relationship with
	 * the aggregation, but that this is appropriately removed during
	 * serialisation and other API calls
	 */
	@Test
	public void testDescribes()
	{
		// all rems should have the ore:describes relation to the aggregation
		// all aggregations should have the ore:isDescribedBy relation to the rem
	}

	/**
	 * Test what happens when you remove Resource Map serialisations from
	 * Aggregations.  What gets orphaned?  What can we do about it?
	 */
	@Test
	public void testREMSerialisationRemoval()
	{

	}

	/**
	 * Test to make sure that creating new Proxies behaves as expected, and
	 * that it appropriately creates and allows us to retrieve aggregated
	 * resources (or links them when they exist)
	 */
	@Test
	public void testProxyAndARCreation()
	{

	}

	/**
	 * Make sure that there's no data loss, and everything behaves as expected
	 * when aggregated aggregations as aggregated resources (!)
	 */
	@Test
	public void testAggregationNesting()
	{

	}
}
