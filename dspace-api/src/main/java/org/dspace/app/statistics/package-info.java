/**
 * Defines usage event instrumentation points and provides implementations for
 * testing.
 * <p>
 * This package makes usage instrumentation (for statistics, or whatever else
 * you may fancy) pluggable, while avoiding any unnecessary assumptions about how
 * usage events may be transmitted, persisted, or processed.
 *
 * <p>
 * At appropriate points in the processing of user actions, events may be
 * assembled and "fired".  What happens when an event is fired is configurable
 * via the PluginService.  One must configure a plugin for the AbstractUsageEvent
 * class, defined in this package, to select an event processing implementation.
 *
 * <p>
 * Several "stock" implementations are provided.
 * <dl>
 *   <dt>{@link org.dspace.usage.PassiveUsageEventListener PassiveUsageEventListener}</dt>
 *   <dd>absorbs events without taking action, resulting in behavior identical
 *		to that of DSpace before this package was added.  This is the default
 *		if no plugin is configured.</dd>
 *   <dt>{@link org.dspace.usage.TabFileUsageEventListener TabFileUsageEventListener}</dt>
 *   <dd>writes event records to a file in Tab Separated Values format.</dd>
 *   <dt>{@link org.dspace.usage.LoggerUsageEventListener LoggerUsageEventListener}</dt>
 *   <dd>writes event records to the Java logger.</dd>
 *   <dt>{@link org.dspace.statistics.SolrLoggerUsageEventListener SolrLoggerUsageEventListener}</dt>
 *   <dd>writes event records to Solr.</dd>
 *   <dt>{@link org.dspace.google.GoogleRecorderEventListener GoogleRecorderEventListener}<.dt>
 *   <dd>writes event records to Google Analytics.</dd>
 * </dl>
 */
package org.dspace.app.statistics;
