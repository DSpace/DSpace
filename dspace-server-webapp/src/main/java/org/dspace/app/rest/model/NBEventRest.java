/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.RestResourceController;

@LinksRest(links = {
		@LinkRest(name = "topic", method = "findByTopic")
})
public class NBEventRest extends BaseObjectRest<String> {

	private static final long serialVersionUID = -5001130073350654793L;

	private static final String NAME_PLURAL = "nbevents";
	public static final String NAME = "nbevent";
	public static final String CATEGORY = RestAddressableModel.INTEGRATION;

	private String id;
	private String originalId;
	private String title;
	private String topic;
	private String trust;
	private NBEventMessage message;
	private String type = "openaireBrokerEvent";

	//per i progetti, vedere se trovato o meno
	// e qui mettere l' handle del progetto
	private String matchFoundHandle;
	private String matchFoundId;
	
	@Override
	public String getType() {
		return NAME;
	}

	@Override
	public String getCategory() {
		return CATEGORY;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOriginalId() {
		return originalId;
	}

	public void setOriginalId(String originalId) {
		this.originalId = originalId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getTrust() {
		return trust;
	}

	public void setTrust(String trust) {
		this.trust = trust;
	}

	public NBEventMessage getMessage() {
		return message;
	}

	public void setMessage(NBEventMessage message) {
		this.message = message;
	}



}
