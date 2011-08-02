/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.PluginManager;

/**
 * TaskResolver takes a logical name of a curation task and delivers a 
 * suitable implementation object. Supported implementation types include:
 * (1) Classpath-local Java classes configured and loaded via PluginManager.
 * (2) Local script-based tasks, viz. coded in any scripting language whose
 * runtimes are accessible via the JSR-223 scripting API. This really amounts
 * to the family of dynamic JVM languages: JRuby, Jython, Groovy, Javascript, etc
 * Note that the requisite jars and other resources for these languages must be
 * installed in the DSpace instance for them to be used here.
 * Further work may involve remote URL-loadable code, etc. 
 * 
 * Scripted tasks are configured in dspace/config/modules/curate.cfg with the 
 * property "script.tasks" with value syntax:
 * <task-desc> = taskName,
 * <task-desc> = taskName
 * where task-desc is a descriptor of the form:
 * <engine>:<relfilePath>:<implClassName>
 * An example property value:
 * 
 * ruby:rubytask.rb:LinkChecker = linkchecker
 * 
 * This descriptor means that the 'ruby' script engine will be created,
 * a script file named 'rubytask.rb' in the directory <taskbase>/ruby/rubtask.rb will be loaded
 * and the resolver will expect that a class 'LinkChecker' will be defined in that script file.
 * 
 * @author richardrodgers
 */

public class TaskResolver
{
	// logging service
	private static Logger log = Logger.getLogger(TaskResolver.class);
	
	// base directory of task scripts
	private static String scriptDir = ConfigurationManager.getProperty("curate", "script.dir");
	 
	// map of task script descriptions, keyed by logical task name
	private static Map<String, String> scriptMap = new HashMap<String, String>();
	
	static
	{
		// build map of task descriptors
		loadDescriptors();
	}
	
	private TaskResolver()
	{
	}
	
	/**
	 * Loads the map of script descriptors
	 */
	public static void loadDescriptors()
	{
		scriptMap.clear();
		String propVal = ConfigurationManager.getProperty("curate", "script.tasks");
		if (propVal != null)
		{
			for (String desc : propVal.split(","))
			{
				String[] parts = desc.split("=");
				scriptMap.put(parts[1].trim(), parts[0].trim());
			}
		}
	}
	
	/**
	 * Returns a task implementation for a given task name,
	 * or <code>null</code> if no implementation could be obtained.
	 */
	public static CurationTask resolveTask(String taskName)
	{
		CurationTask task = (CurationTask)PluginManager.getNamedPlugin("curate", CurationTask.class, taskName);
		if (task == null)
		{
			// maybe it is implemented by a script?
			String scriptDesc = scriptMap.get(taskName);
			if (scriptDesc != null)
			{
				String[] descParts = scriptDesc.split(":");
				// first descriptor token is name ('alias') of scripting engine,
				// which is also the subdirectory where script file kept
				ScriptEngineManager mgr = new ScriptEngineManager();
			    ScriptEngine engine = mgr.getEngineByName(descParts[0]);
			    if (engine != null)
			    {
			    	// see if we can locate the script file and load it
			    	// the second token is the relative path to the file
			    	File script = new File(scriptDir, descParts[1]);
			    	if (script.exists())
			    	{
			    		try
			    		{
			    			Reader reader = new FileReader(script);
			    			engine.eval(reader);
			    			reader.close();
			    			// third token is name of class implementing
			    			// CurationTask interface - add ".new" to ask for an instance
			    			String implInst = descParts[2] + ".new";
			    			task = (CurationTask)engine.eval(implInst);
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
			    	log.error("Script engine: '" + descParts[0] + "' is not installed");
			    } 	
			}
		}
		return task;
	}
}
