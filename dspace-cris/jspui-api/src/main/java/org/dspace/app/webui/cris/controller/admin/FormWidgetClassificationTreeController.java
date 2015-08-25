/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.admin;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.dspace.app.cris.model.jdyna.DynamicObjectType;

import it.cilea.osd.jdyna.controller.FormDecoratorPropertiesDefinitionController;
import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.web.IPropertyHolder;
import it.cilea.osd.jdyna.web.Tab;

public class FormWidgetClassificationTreeController<W extends AWidget, TP extends PropertiesDefinition, DTP extends ADecoratorPropertiesDefinition<TP>, H extends IPropertyHolder<Containable>, T extends Tab<H>>
		extends FormDecoratorPropertiesDefinitionController<W, TP, DTP, H, T> {

	public FormWidgetClassificationTreeController(Class<TP> targetModel, Class<W> renderingModel, Class<H> boxModel) {
		super(targetModel, renderingModel, boxModel);
	}

	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map map = super.referenceData(request);
		List<DynamicObjectType> researchobjects = getApplicationService().getList(DynamicObjectType.class);
		map.put("researchobjects", researchobjects);
		return map;
	}
	
}
