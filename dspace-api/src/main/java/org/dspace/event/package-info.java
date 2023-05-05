/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Actions which alter DSpace model objects can queue {@link Event}s, which
 * are presented to {@link Consumer}s by a {@link Dispatcher}.  A pool of
 * {@code Dispatcher}s is managed by an {@link service.EventService}, guided
 * by configuration properties {@code event.dispatcher.*}.
 *
 * <p>One must be careful not to commit the current DSpace {@code Context}
 * during event dispatch.  {@code commit()} triggers event dispatching, and
 * doing this during event dispatch can lead to infinite recursion and
 * memory exhaustion.
 */

package org.dspace.event;
