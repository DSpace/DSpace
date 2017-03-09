package org.dspace.app.webui.jasper;

import org.dspace.app.webui.jasper.ItemDTO;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

public class ItemsDataSource implements JRDataSource  {

	private Iterator<ItemDTO> iter;
	
	private Map<String,String> metadata;

	public ItemsDataSource(Collection<ItemDTO> c) {
		iter = c.iterator();
	}

	public Object getFieldValue(JRField field) throws JRException {		
		String fieldName = field.getName();
		//System.out.println("chiesto il metadato: "+fieldName);
		return metadata.get(fieldName);
	}

	public boolean next() throws JRException {
		//System.out.println("nuova riga report");
		if (!iter.hasNext())
			return false;		
		ItemDTO dto = iter.next();
		metadata = dto.getMetadata();
		return true;
	}

}
