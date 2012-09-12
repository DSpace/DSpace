package org.dspace.app.importer.isi;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.importer.AConfigurableImporter;
import org.dspace.app.importer.SingleImportResultBean;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;

/**
 * This plugin cannot be used as singleton i.e reusable = false
 * as it keeps state information.
 *
 * @author bollini
 *
 */
public class ISIImporter  extends AConfigurableImporter<ISIItem>
{
	private static Logger log = Logger.getLogger(ISIImporter.class);
	
	private Context context = null;
	
	private List<ISIItem> items = null;
	
    protected final int getTotal(String data)
    {
    	try
        {
            List<ISIItem> entries = getISIEntries(data);
            return entries.size();
        }
        catch (Exception e1)
        {
            return -1;
        }
    }

    protected List<SingleImportResultBean> processData(String data, Community community, Collection collection,
            EPerson eperson)
    {
        List<SingleImportResultBean> results = new LinkedList<SingleImportResultBean>();
        Context context = null;

        List<ISIItem> entries = null;
        try
        {
            entries = getISIEntries(data);
        }
        catch (Exception e1)
        {
            SingleImportResultBean result = new SingleImportResultBean(
                    SingleImportResultBean.ERROR, -1, e1.getMessage(), "", data);
            results.add(result);
            return results;
        }

        for (ISIItem entry : entries)
        {
            try
            {
                context = getContext(eperson);

                Collection targetCollection = getCollection(context, community, collection, entry);
                WorkspaceItem witem = WorkspaceItem.create(context,
                        targetCollection, true);
                fitMetatadata(witem, entry);
                extractMetadata(context, witem, getTargetCollectionFormName(entry));
                removeInvalidMetadata(context, witem, getTargetCollectionFormName(entry));
                witem.update();
                context.commit();
                SingleImportResultBean result = new SingleImportResultBean(
                        SingleImportResultBean.SUCCESS, witem.getID(),
                        "Tipologia assegnata: " + targetCollection.getName(),
                        entry.getISICode(), entry.toString());
                results.add(result);

            }
            catch (Exception e)
            {
                SingleImportResultBean result = new SingleImportResultBean(
						SingleImportResultBean.ERROR, -1, e.getMessage(), entry.getISICode(), entry.toString());
                results.add(result);
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }
        }
        if (context != null && context.isValid())
        {
            context.abort();
        }
        return results;
    }

    protected String getType(ISIItem crossitem)
	{
		return crossitem.getType();
	}

    private void fitMetatadata(WorkspaceItem witem, ISIItem entry)
    {
        for (String field : entry.getFields().keySet())
        {
            List<String> values = entry.getFields().get(field);
            for (String v : values){
            	addMetadata(witem.getItem(), field,
                        getTargetCollectionFormName(entry), v);
            }
        }
    }

    private Context getContext(EPerson eperson) throws SQLException
    {
        if (context != null && context.isValid())
        {
            return context;
        }

        context = new Context();
        context.setCurrentUser(eperson);
        int[] specialGroups = AuthenticationManager.getSpecialGroups(context,
                null);
        for (int groupid : specialGroups)
        {
            context.setSpecialGroup(groupid);
        }
        return context;
    }

    private synchronized List<ISIItem> getISIEntries(String data) throws IOException
    {
        if (items != null)
        {
            return items;
        }
        items = new ArrayList<ISIItem>();
        StringReader stringReader = new StringReader(data);
		BufferedReader br = new BufferedReader(stringReader);
        String strLine;
        ISIItem currItem = new ISIItem();
        String lastField = null;
        StringBuffer source = new StringBuffer();
        //Read File Line By Line
        while ((strLine = br.readLine()) != null)   {
        	strLine.trim();
        	if (strLine.startsWith("UT"))
        	{
        		currItem.setRecord(strLine.substring(2).trim());
        		currItem.addField("UT", strLine.substring(2));
        		lastField = "UT";
        		source.append(strLine);
        	}
        	else if (strLine.startsWith("ER"))
        	{
        		lastField = "ER";
        		source.append(strLine);
//        		currItem.setSource(source.toString());
        		// solo se ha il wos id aggiungo la entry (potrebbe essere un file in formato errato)
        		if (StringUtils.isNotEmpty(currItem.getRecord()))
        			items.add(currItem);        		
        		
        		currItem = new ISIItem();
        		source = new StringBuffer();
        	}
        	else if (strLine.startsWith("EF"))
        	{
        		lastField = "EF";
        		currItem = null;
        	}
        	else if (strLine.length() >= 2)
        	{
        		source.append(strLine);
        		String currField = strLine.substring(0, 2);
        		if (StringUtils.isBlank(currField))
        		{
        			if (StringUtils.isNotBlank(lastField))
        			{
        				currItem.addField(lastField, strLine.substring(2));
        			}
        			else
        			{
        				if (StringUtils.isNotBlank(strLine.substring(2)))
						{
        					// log riga sconosciuta
							log.warn(LogManager.getHeader(context,
									"ISIImporter",
									"Unparserizable data: "
											+ source));
						}
        				else
        				{
        					// nothing, riga vuota da saltare
        				}
        			}
        		}
        		else
        		{
        			currItem.addField(currField,strLine.substring(2));
        			lastField = currField;
        		}
        	}
        }
        if (!("EF".equals(lastField) && currItem == null))
        {
        	// file con sintassi errata
        	if (!"ER".equals(lastField))
        	{
        		if (StringUtils.isNotEmpty(currItem.getRecord()))
        		{
	        		// aggiungo comunque l'ultimo item, probabilmente e' incompleto
//	        		if (StringUtils.isBlank(currItem.getSource()) && source != null)
//    				{
//	        			currItem.setSource(source.toString());
//    				}
        			items.add(currItem);
					log.warn(LogManager.getHeader(context, "ISIImporter",
							"record " + currItem.getRecord()
									+ " incomplete, ER not found"));
        		}
        		else
        		{
        			log.warn(LogManager.getHeader(context, "ISIImporter",
							"record discarded as incomplete, ER not found:\n"+source));
        		}
        	}
        	else
        	{
        		log.warn(LogManager.getHeader(context, "ISIImporter",
						"End of file not found (EF), is the export incomplete?"));
        	}
        }
        //Close the input stream
        stringReader.close();
        return items;
    }
}