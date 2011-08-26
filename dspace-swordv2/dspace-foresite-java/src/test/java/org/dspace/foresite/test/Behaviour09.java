/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
