package org.dspace.app.rest.submit.factory;

import org.dspace.app.rest.submit.PatchConfigurationService;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

public class PatchOperationFactory {
	
	private PatchConfigurationService patchConfigurationService;

	public PatchOperation instanceOf(String instance, String operation) {
		PatchOperation result = getPatchConfigurationService().getMap().get(operation).get(instance);
		if(result==null) {
			throw new RuntimeException("Operation "+operation+" not yet implemented for " + instance);
		}
		return result;
	}

	public PatchConfigurationService getPatchConfigurationService() {
		if(patchConfigurationService==null) {
			patchConfigurationService = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("patchConfigurationService", PatchConfigurationService.class);
		}
		return patchConfigurationService;
	}

	public void setPatchConfigurationService(PatchConfigurationService patchOperationService) {
		this.patchConfigurationService = patchOperationService;
	}
}
