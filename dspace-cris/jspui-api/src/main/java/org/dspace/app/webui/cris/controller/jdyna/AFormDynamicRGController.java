/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import it.cilea.osd.jdyna.model.ANestedObjectWithTypeSupport;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ANestedProperty;
import it.cilea.osd.jdyna.model.AnagraficaObject;
import it.cilea.osd.jdyna.model.Containable;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.model.Property;
import it.cilea.osd.jdyna.web.IPropertyHolder;
import it.cilea.osd.jdyna.web.Tab;
import it.cilea.osd.jdyna.web.controller.FormAnagraficaController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.Project;
import org.dspace.app.webui.cris.util.CrisAuthorizeManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Context;

/**
 * This is the base abstract SpringMVC controller for the RPs authority list
 * project. All the other form controller extends this. This abstract controller
 * is responsible to initialize the SpringMVC binder system with the
 * configurated property editors and add common reference data to all the view
 * for the hide/unhide of menus on a role basis.
 * 
 * @author cilea
 * 
 */
public abstract class AFormDynamicRGController<P extends Property<TP>, TP extends PropertiesDefinition, H extends IPropertyHolder<Containable>, T extends Tab<H>, AO extends AnagraficaObject<P, TP>, ANO extends ANestedObjectWithTypeSupport<AP, ATP, P, TP>, AP extends ANestedProperty<ATP>, ATP extends ANestedPropertiesDefinition> extends FormAnagraficaController<P, TP, H, T, AO, ANO, AP, ATP> {

    /**
     * The log4j category
     */
	protected final Log log = LogFactory.getLog(getClass());
	
	private String specificPartPath;

	public String getSpecificPartPath() {
		return specificPartPath;
	}

	public void setSpecificPartPath(String specificPartPath) {
		this.specificPartPath = specificPartPath;
	}

	/**
     * Add common reference data to all the view for the hide/unhide of menus on
     * a role basis.
     */
    protected Map referenceData(HttpServletRequest request) throws Exception
    {
        Map<String, Object> reference = new HashMap<String, Object>();
        
        String id_s = request.getParameter("id");
        Integer id = Integer.parseInt(id_s);
        Project grant = getApplicationService().get(
                    Project.class, id);
        Context context = UIUtil.obtainContext(request);        
        if (CrisAuthorizeManager.isAdmin(context, grant))
        {
            reference.put("isAdmin", new Boolean(true));
            reference.put("grant_page_menu", new Boolean(true));
            reference.put("project", grant);         
        }
        
        reference.put("specificPartPath", getSpecificPartPath());      
        return reference;
    }
	

}
