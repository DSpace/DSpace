/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.text.ParseException;
import java.util.regex.*;

import org.dspace.core.Constants;

/**
 *   Holds the elements of a line in the Contents Entry file
 * 
 *   Based on private methods in ItemImport
 *   
 *   Lacking a spec or full documentation for the file format, 
 *   it looks from the source code that the ordering or elements is not fixed
 * 
 *   e.g.:
 *      {@code
 *      48217870-MIT.pdf\tbundle: bundlename\tpermissions: -r 'MIT Users'\tdescription: Full printable version (MIT only)
 *      permissions: -[r|w] ['group name']
 *      description: <the description of the file>
 *      }
 * 
 *
 */
public class ContentsEntry 
{
	public static final String HDR_BUNDLE = "bundle:";
	public static final String HDR_PERMISSIONS = "permissions:";
	public static final String HDR_DESCRIPTION = "description:";
		
	public static final Pattern permissionsPattern = Pattern.compile("-([rw])\\s*'?([^']+)'?");
	
	final String filename;
	final String bundlename;
	final String permissionsGroupName;
	final int permissionsActionId;
	final String description;
	
	protected ContentsEntry(String filename,
			             String bundlename,
			             int permissionsActionId,
			             String permissionsGroupName,
			             String description)
	{
		this.filename = filename;
		this.bundlename = bundlename;
		this.permissionsActionId = permissionsActionId;
		this.permissionsGroupName = permissionsGroupName;
		this.description = description;
	}
	
	/**
	 *   Factory method parses a line from the Contents Entry file
	 *   
	 * @param line line as string
	 * @return the parsed ContentsEntry object 
	 * @throws ParseException if parse error
	 */
	public static ContentsEntry parse(String line)
	throws ParseException
	{
		String[] ar = line.split("\t");
		ItemUpdate.pr("ce line split: " + ar.length);
		
		String[] arp = new String[4];
		arp[0] = ar[0];   //bitstream name doesn't have header and is always first
		
		String groupName = null;
		int actionId = -1;

		if (ar.length > 1)
		{
			for (int i=1; i < ar.length; i++)
			{
				ItemUpdate.pr("ce " + i + " : " + ar[i]);
				if (ar[i].startsWith(HDR_BUNDLE))
				{
					arp[1] = ar[i].substring(HDR_BUNDLE.length()).trim();
					
				}
				else if (ar[i].startsWith(HDR_PERMISSIONS))
				{
					arp[2] = ar[i].substring(HDR_PERMISSIONS.length()).trim();
					
					// parse into actionId and group name
					
					Matcher m = permissionsPattern.matcher(arp[2]);
					if (m.matches())
					{
						String action = m.group(1); //
						if (action.equals("r"))
						{
							 actionId = Constants.READ;
						}
						else if (action.equals("w"))
						{
							 actionId = Constants.WRITE;
						}
						
						groupName = m.group(2).trim();
					}
					
				}
				else if (ar[i].startsWith(HDR_DESCRIPTION))
				{
					arp[3] = ar[i].substring(HDR_DESCRIPTION.length()).trim();
					
				}
				else
				{
					throw new ParseException("Unknown text in contents file: " + ar[i], 0);				
				}			
			}
		}
		return new ContentsEntry(arp[0], arp[1], actionId, groupName, arp[3]);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder(filename);
		if (bundlename != null)
		{
			sb.append(HDR_BUNDLE).append(" ").append(bundlename);
		}
		
		if (permissionsGroupName != null)
		{
			sb.append(HDR_PERMISSIONS);
			if (permissionsActionId == Constants.READ)
			{
				sb.append(" -r ");
			}
			else if (permissionsActionId == Constants.WRITE)
			{
				sb.append(" -w ");
			}
			sb.append(permissionsGroupName);
		}
		
		if (description != null)
		{
			sb.append(HDR_DESCRIPTION).append(" ").append(description);
		}		
		
		return sb.toString();
	}
	
}
