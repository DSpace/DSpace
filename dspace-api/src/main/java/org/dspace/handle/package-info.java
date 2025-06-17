/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Provides classes and methods to interface with the
 * <a href="http://www.handle.net" target=_new>CNRI Handle System</a>.
 * The {@link HandleServiceImpl} class acts as the main entry point.
 * <p>
 * The {@link HandlePlugin} class is intended to be loaded into the CNRI Handle
 * Server. It acts as an adapter, translating Handle Server API calls into
 * DSpace ones.
 *
 * <h2>Using the Handle API</h2>
 *
 * <p>
 *   An example use of the Handle API is shown below:
 * <pre>{@code
 *    Item item;
 *
 *    // Create or obtain a context object
 *    Context context;
 *
 *    // Create a Handle for an Item
 *    String handle = HandleManager.createHandle(context, item);
 *    // The canonical form, which can be used for citations
 *    String canonical = HandleManager.getCanonicalForm(handle);
 *    // A URL pointing to the Item
 *    String url = HandleManager.resolveToURL(context, handle);
 *
 *    // Resolve the handle back to an object
 *    Item resolvedItem = (Item) HandleManager.resolveToObject(context, handle);
 *    // From the object, find its handle
 *    String rhandle = HandleManager.findHandle(context, resolvedItem);
 * }</pre>
 * </p>
 *
 * <h2>Using the HandlePlugin with CNRI Handle Server</h2>
 *
 * In the CNRI Handle Server configuration file, set storage_type to
 * <em>CUSTOM</em> and storage_class to
 * <em>org.dspace.handle.HandlePlugin</em>.
 *
 * <p>FIXME: Can we get a sample configuration file?
 *
 * @author Peter Breton
 */
package org.dspace.handle;
