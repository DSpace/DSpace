package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.List;

public class LookupProvidersCheck {
	private List<String> providersOk = new ArrayList<String>();
	private List<String> providersErr = new ArrayList<String>();
	public List<String> getProvidersOk() {
		return providersOk;
	}
	public void setProvidersOk(List<String> providersOk) {
		this.providersOk = providersOk;
	}
	public List<String> getProvidersErr() {
		return providersErr;
	}
	public void setProvidersErr(List<String> providersErr) {
		this.providersErr = providersErr;
	}
	
}
