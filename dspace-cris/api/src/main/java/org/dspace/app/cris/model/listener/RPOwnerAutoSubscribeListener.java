/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.listener;

import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.cris.model.ResearcherPage;
import org.dspace.app.cris.service.CrisSubscribeService;
import org.dspace.app.cris.util.Researcher;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import it.cilea.osd.common.listener.NativePostUpdateEventListener;
import it.cilea.osd.common.model.Identifiable;

public class RPOwnerAutoSubscribeListener implements NativePostUpdateEventListener {

	@Transient
	private static Logger log = Logger.getLogger(RPOwnerAutoSubscribeListener.class);

	private CrisSubscribeService rpSubService;
	
	@Override
	public <T extends Identifiable> void onPostUpdate(T entity) {

		Object object = entity;
		if (!(object instanceof ResearcherPage)) {
			// nothing to do
			return;
		}

		log.debug("Call onPostUpdate " + RPOwnerAutoSubscribeListener.class);

		ResearcherPage crisObj = (ResearcherPage) object;

		try {			
			if (!StringUtils.isBlank(crisObj.getUuid())) {
	            Context context = null;
	            try {
	                context = new Context();
	                Integer ownerUserID = crisObj.getEpersonID();
	                if(ownerUserID!=null) {
	                	EPerson currentUser = EPerson.find(context, ownerUserID);
	                	if(currentUser!=null) {
	                		getRpSubService().subscribe(currentUser, crisObj.getUuid());
	                	}
	                }
	            } catch (Exception ex) {
	                log.error(ex.getMessage(), ex);
	            } finally {
	                if (context != null && context.isValid()) {
	                    context.abort();
	                }
	            }
			}
		} catch (Exception e) {
			log.error("Failed to build CRISID for entity " + crisObj.getTypeText() + "/" + crisObj.getCrisID());
		}

		log.debug("End onPostUpdate " + RPOwnerAutoSubscribeListener.class);
	}

	public CrisSubscribeService getRpSubService() {
		if(rpSubService==null) {
			rpSubService = new Researcher().getCrisSubscribeService();
		}
		return rpSubService;
	}

	public void setRpSubService(CrisSubscribeService rpSubService) {
		this.rpSubService = rpSubService;
	}
}
