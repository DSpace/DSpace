package org.dspace.submit.step.domain;

import java.util.Date;
import java.util.EnumSet;

/**
 * Register embargo options
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public enum EmbargoOption {
	
	FREE(3, "Acesso Aberto"){

		@Override
		public Date getAssociatedDate() 
		{
			return new Date();
		}
	},
	EMBARGOED(2, "Acesso Embargado"),
	RESTRICTED(1, "Acesso Restrito") {

		@Override
		public Date getAssociatedDate() 
		{
			return Constants.RESTRICT_DATE;
		}
		
	};
	
	private Integer id;
	private String key;
	
	private EmbargoOption(Integer id, String key) {
		this.id = id;
		this.key = key;
	}
	
	/**
	 * Recovers an instance of {@link EmbargoOption} by its ID
	 * @param id Id to be used as filer
	 * @return Found enum instance (or null)
	 */
	public static EmbargoOption recoverById(Integer id)
	{
		for(EmbargoOption embargoOption : EnumSet.allOf(EmbargoOption.class))
		{
			if(embargoOption.getId().equals(id))
			{
				return embargoOption;
			}
		}
		return null;
	}

	public String getKey() {
		return key;
	}

	public Integer getId() {
		return id;
	}

	public Date getAssociatedDate() {
		return null;
	}
	
	/**
	 * Constants for this enum
	 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
	 *
	 */
	private static class Constants {
		private static final Date RESTRICT_DATE = new Date(32503687200000l);
	}
}