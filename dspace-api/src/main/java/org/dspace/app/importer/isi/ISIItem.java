package org.dspace.app.importer.isi;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.importer.ItemImport;

public class ISIItem implements ItemImport {
	private static String repeatableFields = "AU AF BA CA GP BE SP C1 RP EM";
	// Nome della classe istanziata
	private String source;
	// Valore del metadato source
	private String record;

	private Map<String, List<String>> fields = new HashMap<String, List<String>>();

	public Map<String, List<String>> getFields() {
		return fields;
	}

	public void setSource(String source) {
		this.source = source;
	}

	@Override
	public void setRecord(String record) {
		this.record = record;
	}

	@Override
	public String getRecord() {
		return record;
	}

	@Override
	public String getSource() {
		return this.getClass().getCanonicalName();
	}

	public String getISICode() {
		List<String> uniqueID = getFields().get("UT");
		if (uniqueID != null && uniqueID.size() > 0)
			return uniqueID.get(0);
		else
			return null;
	}

	protected String getType() {
		List<String> publicationType = getFields().get("PT");
		List<String> conferanceName = getFields().get("CT");
		if (conferanceName != null && conferanceName.size() > 0
				&& StringUtils.isNotBlank(conferanceName.get(0)))
			return "P";
		if (publicationType != null && publicationType.size() > 0)
			return publicationType.get(0);
		else
			return "J";
	}

	public void addField(String currField, String value) {
		if (StringUtils.isNotBlank(value)) {
			if (repeatableFields.indexOf(currField) != -1) {
				List<String> values = fields.get(currField);
				if (values == null) {
					values = new ArrayList<String>();
					fields.put(currField, values);
				}
				values.add(value.trim());
			} else {
				List<String> values = fields.get(currField);
				if (values == null) {
					values = new ArrayList<String>();
					fields.put(currField, values);
					values.add(value.trim());
				} else {
					values.set(0, values.get(0) + " " + value.trim());
				}
			}
		}
	}

}