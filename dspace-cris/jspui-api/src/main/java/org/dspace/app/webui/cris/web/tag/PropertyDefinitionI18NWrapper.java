/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.cris.web.tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.dspace.core.I18nUtil;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.IPropertiesDefinition;
import it.cilea.osd.jdyna.widget.WidgetCheckRadio;

public final class PropertyDefinitionI18NWrapper implements MethodInterceptor {
	private Locale locale = null;
	private String localeString = null;
	private String simpleName = null;
	private String shortName = null;
	private int priority = 0;

	public PropertyDefinitionI18NWrapper(String simpleName, String shortName, String localeString, int priority) {
		this.locale = Locale.forLanguageTag(localeString);
		this.localeString = localeString;
		this.simpleName = simpleName;
		this.shortName = shortName;
		this.priority = priority;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (locale != null) {
			String name = invocation.getMethod().getName();
			if ("getLabel".equals(name)) {
				return getLabel(invocation);
			} else if ("getReal".equals(name) || "getObject".equals(name)) {
				return getWrapper((IPropertiesDefinition) invocation.proceed(), localeString);
			} else if ("getMask".equals(name)) {
				return getMask(invocation);
			} else if ("getPriority".equals(name)) {
                return priority;
			} else if ("getRendering".equals(name)) {
				AWidget widget = (AWidget) invocation.proceed();
				if (widget instanceof WidgetCheckRadio) {
					WidgetCheckRadio wCheck = (WidgetCheckRadio) widget;
					return getWidgetCheckRadioWrapper(wCheck, simpleName, shortName, locale);
				}
				return widget;
			}
			
		}
		return invocation.proceed();
	}

	private Object getLabel(MethodInvocation invocation) throws Throwable {
		try {
			return I18nUtil.getMessage(simpleName + "." + shortName + ".label", locale, true);
		} catch (MissingResourceException mre) {
			return invocation.proceed();
		}
	}

	private Object getMask(MethodInvocation invocation) throws Throwable {
		List wrappedList = new ArrayList<>();
		List origList = (List) invocation.proceed();
		for (Object o : origList) {
			wrappedList.add(getWrapper((IPropertiesDefinition) o, localeString));
		}
		return wrappedList;
	}

    public static IPropertiesDefinition getWrapper(IPropertiesDefinition pd, String locale) {
        AspectJProxyFactory pf = new AspectJProxyFactory(pd);
        pf.setProxyTargetClass(true);
        pf.addAdvice(
                new PropertyDefinitionI18NWrapper(pd.getAnagraficaHolderClass().getSimpleName(), pd.getShortName(), locale, pd.getPriority()));
        return pf.getProxy();
    }
    
    public static WidgetCheckRadio getWidgetCheckRadioWrapper(WidgetCheckRadio widget, String simpleName, String shortName, Locale locale) {
        AspectJProxyFactory pf = new AspectJProxyFactory(widget);
        pf.setProxyTargetClass(true);
        pf.addAdvice(
                new WidgetCheckRadioI18NWrapper(simpleName, shortName, locale));
        return pf.getProxy();
    }	
	
}
