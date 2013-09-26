package org.dspace.submit.util;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class SubmissionLookupDTO implements Serializable {
	private static final long serialVersionUID = 1;
	
	private String uuid;

	private List<ItemSubmissionLookupDTO> items;

	public SubmissionLookupDTO() {
		this.uuid = UUID.randomUUID().toString();
	}
	
	public void setItems(List<ItemSubmissionLookupDTO> items) {
		this.items = items;
	}

	public ItemSubmissionLookupDTO getLookupItem(String uuidLookup) {
		if (items != null)
		{
			for (ItemSubmissionLookupDTO item : items)
			{
				if (item.getUUID().equals(uuidLookup))
				{
					return item;
				}
			}
		}
		return null;		
	}
}
