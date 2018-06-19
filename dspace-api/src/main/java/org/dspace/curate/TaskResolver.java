/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * TaskResolver takes a logical name of a curation task and attempts to deliver 
 * a suitable implementation object. Supported implementation types include:
 * (1) Classpath-local Java classes configured and loaded via PluginService.
 * (2) Local script-based tasks, viz. coded in any scripting language whose
 * runtimes are accessible via the JSR-223 scripting API. This really amounts
 * to the family of dynamic JVM languages: JRuby, Jython, Groovy, Javascript, etc
 * Note that the requisite jars and other resources for these languages must be
 * installed in the DSpace instance for them to be used here.
 * Further work may involve remote URL-loadable code, etc.
 *
 * Scripted tasks are managed in a directory configured with the
 * dspace/config/modules/curate.cfg property "script.dir". A catalog of
 * scripted tasks named 'task.catalog" is kept in this directory.
 * Each task has a 'descriptor' property with value syntax:
 * {@code <engine>|<relFilePath>|<implClassCtor>}
 * An example property:
 * 
 * {@code linkchecker = ruby|rubytask.rb|LinkChecker.new}
 * 
 * This descriptor means that a 'ruby' script engine will be created,
 * a script file named 'rubytask.rb' in the directory {@code <script.dir>} will be
 * loaded and the resolver will expect an evaluation of 'LinkChecker.new' will 
 * provide a correct implementation object.
 * 
 * Script files may embed their descriptors to facilitate deployment.
 * To accomplish this, a script must include the descriptor string with syntax:
 * {@code $td=<descriptor>} somewhere on a comment line. for example:
 * 
 * {@code My descriptor $td=ruby|rubytask.rb|LinkChecker.new}
 * 
 * For portability, the {@code <relFilePath>} component may be omitted in this context.
 * Thus, {@code $td=ruby||LinkChecker.new} will be expanded to a descriptor
 * with the name of the embedding file.
 * 
 * @author richardrodgers
 */

public class TaskResolver
{
	// logging service
	private static Logger log = Logger.getLogger(TaskResolver.class);
	
	// base directory of task scripts & catalog name
	protected static final String CATALOG = "task.catalog";
	protected final String scriptDir;
	
	// catalog of script tasks
	protected Properties catalog;
	
	public TaskResolver()
	{
            scriptDir = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("curate.script.dir");
	}
		
	/**
	 * Installs a task script. Succeeds only if script:
	 * (1) exists in the configured script directory and
	 * (2) contains a recognizable descriptor in a comment line.
	 * If script lacks a descriptor, it may still be installed
	 * by manually invoking <code>addDescriptor</code>.
	 * 
	 * @param taskName
	 * 		  logical name of task to associate with script
	 * @param fileName
	 * 		  name of file containing task script
	 * @return true if script installed, false if installation failed
	 */
	public boolean installScript(String taskName, String fileName)
	{
		// Can we locate the file in the script directory?
		File script = new File(scriptDir, fileName);
		if (script.exists())
		{
			BufferedReader reader = null;
			try
			{
				reader = new BufferedReader(new FileReader(script));
				String line = null;
				while((line = reader.readLine()) != null)
				{
					if (line.startsWith("#") && line.indexOf("$td=") > 0)
					{
						String desc = line.substring(line.indexOf("$td=") + 4);
						// insert relFilePath if missing
						String[] tokens = desc.split("\\|");
						if (tokens[1].length() == 0)
						{
							desc = tokens[0] + "|" + fileName + "|" + tokens[2];
						}
						addDescriptor(taskName, desc);
						return true;
					}
				}
			}
			catch(IOException ioE)
			{
				log.error("Error reading task script: " + fileName);
			}
			finally
			{
				if (reader != null)
				{
					try
					{
						reader.close();
					}
					catch(IOException ioE)
					{
						log.error("Error closing task script: " + fileName);
					}
				}
			}			
		}
		else
		{
			log.error("Task script: " + fileName + "not found in: " + scriptDir);
		}
		return false;
	}
	
	/**
	 * Adds a task descriptor property and flushes catalog to disk.
	 * 
	 * @param taskName
	 *        logical task name
	 * @param descriptor
	 *         descriptor for task
	 */
	public void addDescriptor(String taskName, String descriptor)
	{
		loadCatalog();
		catalog.put(taskName, descriptor);
		Writer writer = null;
		try
		{
			writer = new FileWriter(new File(scriptDir, CATALOG));
			catalog.store(writer, "do not edit");
		}
		catch(IOException ioE)
		{
			log.error("Error saving scripted task catalog: " + CATALOG);
		}
		finally
		{
			if (writer != null)
			{
				try
				{
					writer.close();
				}
				catch (IOException ioE)
				{
					log.error("Error closing scripted task catalog: " + CATALOG);
				}
			}
		}
	}
	
	/**
	 * Returns a task implementation for a given task name,
	 * or <code>null</code> if no implementation could be obtained.
	 * 
	 * @param taskName
	 *        logical task name
	 * @return task
	 *        an object that implements the CurationTask interface
	 */
	public ResolvedTask resolveTask(String taskName)
	{
		CurationTask ctask = (CurationTask)CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(CurationTask.class, taskName);
		if (ctask != null)
		{
			return new ResolvedTask(taskName, ctask);
		}
		// maybe it is implemented by a script?
		loadCatalog();
		String scriptDesc = catalog.getProperty(taskName);
		if (scriptDesc != null)
		{
			String[] tokens = scriptDesc.split("\\|");
			// first descriptor token is name ('alias') of scripting engine
			ScriptEngineManager mgr = new ScriptEngineManager();
			ScriptEngine engine = mgr.getEngineByName(tokens[0]);
			if (engine != null)
			{
			    // see if we can locate the script file and load it
			    // the second token is the relative path to the file
			    File script = new File(scriptDir, tokens[1]);
			    if (script.exists())
			    {
			    	try
			    	{
			    		Reader reader = new FileReader(script);
			    		engine.eval(reader);
			    		reader.close();
			    		// third token is the constructor expression for the class
			    		// implementing CurationTask interface
			    		ScriptedTask stask = (ScriptedTask)engine.eval(tokens[2]);
			    		return new ResolvedTask(taskName, stask);
			    	}
			    	catch (FileNotFoundException fnfE)
			    	{
			    		log.error("Script: '" + script.getName() + "' not found for task: " + taskName);
			    	}
			    	catch (IOException ioE)
			    	{
			    		log.error("Error loading script: '" + script.getName() + "'");
			    	}
			    	catch (ScriptException scE)
			    	{
			    		log.error("Error evaluating script: '" + script.getName() + "' msg: " + scE.getMessage());
			    	}
			    }
			    else
			    {
			    	log.error("No script: '" + script.getName() + "' found for task: " + taskName);
			    }
			} 
			else
			{
			    log.error("Script engine: '" + tokens[0] + "' is not installed");
			} 	
		}
		return null;
	}
	
	/**
	 * Loads catalog of descriptors for tasks if not already loaded
	 */
	protected void loadCatalog()
	{
		if (catalog == null)
		{
			catalog = new Properties();
			File catalogFile = new File(scriptDir, CATALOG);
			if (catalogFile.exists())
			{
				try
				{
					Reader reader = new FileReader(catalogFile);
					catalog.load(reader);
					reader.close();
				}
				catch(IOException ioE)
				{
					log.error("Error loading scripted task catalog: " + CATALOG);
				}
			}
		}
	}
}
