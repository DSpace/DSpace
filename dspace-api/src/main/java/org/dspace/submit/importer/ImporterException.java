package org.dspace.submit.importer;

public class ImporterException extends RuntimeException {
	
	private int total;
	
	private int limit;
	
		private String pluginName;
	
	public ImporterException(String cause, int tot, int limit, String pluginName) {
		super(cause);
		this.total = tot;
		this.limit = limit;
		this.pluginName = pluginName;
	}

   	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

}