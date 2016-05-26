package org.dspace.content.generator;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.eperson.EPerson;

public class SubmitterValueGenerator implements TemplateValueGenerator {

	@Override
	public Metadatum[] generator(Item targetItem, Item templateItem, Metadatum metadatum, String extraParams) {
		Metadatum[] m = new Metadatum[1];
		m[0] = metadatum;
		try {
			EPerson eperson = targetItem.getSubmitter();
			if (StringUtils.equalsIgnoreCase(extraParams, "email")) {
				metadatum.value = eperson.getEmail();
			}
			else if (StringUtils.equalsIgnoreCase(extraParams, "phone")) {
				metadatum.value = eperson.getMetadata("phone");
			}
			else if (StringUtils.equalsIgnoreCase(extraParams, "fullname")) {
				metadatum.value = eperson.getFullName();
			}
			else {
				metadatum.value = eperson.getMetadata(extraParams);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (StringUtils.isNotBlank(m[0].value)){
			return m;
		}
		else {
			return new Metadatum[0];
		}
	}

}
