/*
 * BrowseOutput.java
 * 
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
	 * @param sql
	 * @throws BrowseException
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
