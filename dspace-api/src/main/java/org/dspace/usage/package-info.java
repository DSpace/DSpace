/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

/**
 * Capture of "usage events".  A {@link org.dspace.usage.UsageEvent} represents
 * something like a download or viewing of a Bitstream -- that is, the
 * <em>use</em> of content as opposed to its ingestion or alteration.  Usage
 * events are meant to be useful for statistical analysis of content usage.
 *
 * <p>
 * Multiple {@link org.dspace.usage.AbstractUsageEventListener} implementations
 * may be configured for processing these events.  When an event is "fired",
 * it is passed to each configured listener.  Several stock listeners are provided,
 * in this package and others, for doing common tasks.
 * </p>
 *
 * <p>
 * To add a usage event listener to the bus, configure it as a new {@code <bean>}
 * in a web application's {@code applicationContext.xml} and inject the
 * {@code EventService}, as with the stock listeners.
 * </p>
 *
 * @see org.dspace.statistics.ElasticSearchLoggerEventListener
 * @see org.dspace.google.GoogleRecorderEventListener
 * @see org.dspace.statistics.SolrLoggerUsageEventListener
 */

package org.dspace.usage;
