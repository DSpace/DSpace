/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.LinkedList;
import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;

import org.dspace.app.util.XMLUtils;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.w3c.dom.Element;

/**
 *
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 * @author Philipp Rumpf
 */
public class ISBNUtils
{
	public static Record convertISBNDomToRecord(Element dataRoot, String isbn)
	{
		MutableRecord record = new SubmissionLookupPublication("");
		if (isbn != null)
			record.addValue("isbn", new StringValue(isbn));
		List<Element> datafields = XMLUtils.getElementList(dataRoot, "datafield");

		String title = "";
		List<String> authors = new LinkedList<String>();

		for (Element datafield : datafields)
		{
			String tag = datafield.getAttribute("tag");
			List<Element> subfields = XMLUtils.getElementList(datafield, "subfield");
			for (Element subfield : subfields) {
				String code = subfield.getAttribute("code");
				String value = subfield.getTextContent();

				// LOC records contain separators; be careful not to remove the period after a middle initial, though.
				value = value.replaceAll("([a-z ])\\.$", "$1");
				value = value.replaceFirst(" ?[,;:/-]+$", "");

				if (tag.equals("245") && code.equals("a")) {
					if (!title.equals(""))
						title += " ";
					title += value;
				}

				if (tag.equals("100") && code.equals("a")) {
					authors.add(value);
				}
				if (tag.equals("700") && code.equals("a")) {
					authors.add(value);
				}
				if (tag.equals("020") && code.equals("a")) {
					value = value.replaceAll("^.*?([- 0-9]*).*?$", "$1");
					value = value.replaceAll("[- ]", "");
					if (!value.equals(isbn))
						record.addValue("isbn", new StringValue(value));
				}
			}
		}

		if (!title.equals(""))
			record.addValue("title", new StringValue(title));

		record.addValue("type", new StringValue("book"));

		if (authors.size() > 0)
		{
			List<Value> values = new LinkedList<Value>();
			for (String sArray : authors)
			{
				values.add(new StringValue(sArray));
			}
			record.addField("author", values);
		}

		if (authors.size() > 0)
		{
			List<Value> values = new LinkedList<Value>();
			for (String sArray : authors)
			{
				values.add(new StringValue(sArray));
			}
			record.addField("authorWithAffiliation", values);
		}

		return record;
	}
}
