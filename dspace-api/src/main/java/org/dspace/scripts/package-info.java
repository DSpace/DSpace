/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
 /**
  * Support for tools that can be run either from the command line or from the
  * DSpace GUI.
  *
  * <p>A tool will consist of (at least) three classes:
  *
  * <ul>
  * <li>The main tool class:  a subclass of {@link DSpaceRunnable}.</li>
  * <li>A configuration class:  a subclass of
  *     {@link ScriptConfiguration}
  *     over the main class.</li>
  * <li>An "options" enumeration which names all of the tool's options.  It
  *     also has a {@code constructOptions} method which builds the
  *     {@link Options} object for a command parser.</li>
  * </ul>
  *
  * <p>There is no need to catch all exceptions in your tool code.  This
  * infrastructure will catch them and report them appropriately for the
  * environment in which the tool is running.  Of course, if you can add
  * information by stacking on your own Exception, that is good practice.  You
  * can also throw your own exceptions and let the infrastructure handle them.
  *
  * <p>DSpaceRunnable makes a {@link DSpaceRunnableHandler} available to the
  * subclass as the instance field {@code handler}.  The handler provides a
  * number of useful methods for reporting to the user:
  * {@link DSpaceRunnableHandler#logInfo} and the like.  These should be used
  * rather than e.g. {@code System.out.format} for communicating with the user.
  *
  * <p>Do not call {@code System.exit} or otherwise try to terminate execution.
  * A simple {@code return} is preferred.
  */
package org.dspace.scripts;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.handler.DSpaceRunnableHandler;