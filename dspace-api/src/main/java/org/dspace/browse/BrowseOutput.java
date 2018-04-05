/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility class to provide a wrapper for the various output possibilities from
 * the IndexBrowse class.  It can output to the screen and to file, and it can be
 * verbose or not verbose.
 * 
 * @author Richard Jones
 *
 */
public class BrowseOutput
{

	/** be verbose? */
	private boolean verbose = false;

	/** print to the screen? */
	private boolean print = false;

	/** write to file? */
	private boolean file = false;

	/** append to file, or overwrite? */
	private boolean append = true;
	
	/** name of file to write to */
	private String fileName;

	/**
	 * Constructor.
	 */
	public BrowseOutput()
	{
		
	}

	/**
	 * @return Returns the append.
	 */
	public boolean isAppend()
	{
		return append;
	}

	/**
	 * @param append
	 *            The append to set.
	 */
	public void setAppend(boolean append)
	{
		this.append = append;
	}

	/**
	 * @return Returns the fileName.
	 */
	public String getFileName()
	{
		return fileName;
	}

	/**
	 * @param fileName
	 *            The fileName to set.
	 */
	public void setFileName(String fileName)
	{
		this.fileName = fileName;
		setAppend(false);
	}

	/**
	 * @return Returns the file.
	 */
	public boolean isFile()
	{
		return file;
	}

	/**
	 * @param file
	 *            The file to set.
	 */
	public void setFile(boolean file)
	{
		this.file = file;
	}

	/**
	 * @return Returns the print.
	 */
	public boolean isPrint()
	{
		return print;
	}

	/**
	 * @param print
	 *            The print to set.
	 */
	public void setPrint(boolean print)
	{
		this.print = print;
	}

	/**
	 * @return Returns the verbose.
	 */
	public boolean isVerbose()
	{
		return verbose;
	}

	/**
	 * @param verbose
	 *            The verbose to set.
	 */
	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}

	/**
	 * Pass in a message to be processed.  If the setting is verbose
	 * then this will be output to System.out
	 * 
	 * @param message	the message to set
	 */
	public void message(String message)
	{
		if (isVerbose())
		{
			System.out.println(message);
		}
	}

	/**
	 * Pass in a message that must be displayed to the user, irrespective
	 * of the verbosity.  Will be displayed to System.out
	 * 
	 * @param message	the urgent message
	 */
	public void urgent(String message)
	{
		System.out.println(message);
	}

	/**
	 * Pass in some SQL.  If print is set to true this will output to the
	 * screen.  If file is set to true, this will write to the file specified.
	 * 
	 * @param sql SQL string
	 * @throws BrowseException if browse error
	 */
	public void sql(String sql) throws BrowseException
	{
		if (isPrint())
		{
			System.out.println(sql);
		}

		if (isFile())
		{
			try
			{
				BufferedWriter out = new BufferedWriter(new FileWriter(fileName, isAppend()));
				out.write(sql + "\n");
				out.close();
				setAppend(true);
			}
			catch (IOException e)
			{
				throw new BrowseException(e);
			}
		}
	}

}
