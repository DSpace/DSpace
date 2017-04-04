package org.dspace.app.webui.cris.web.tag;

import java.util.Locale;
import java.util.MissingResourceException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.dspace.core.I18nUtil;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition;
import it.cilea.osd.jdyna.model.PropertiesDefinition;

public final class PropertyDefintionI18NWrapper implements MethodInterceptor {
	private Locale locale = null;
	private String localeString = null;
	private String simpleName = null;
	private String shortName = null;

	public PropertyDefintionI18NWrapper(String simpleName, String shortName, String localeString) {
		this.locale = Locale.forLanguageTag(localeString);
		this.localeString = localeString;
		this.simpleName = simpleName;
		this.shortName = shortName;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (locale != null) {
			if (invocation.getMethod().getName().equals("getLabel")) {
				return getLabel(invocation);
			} else if (invocation.getMethod().getName().equals("getReal")) {
				return getWrapper((PropertiesDefinition) invocation.proceed(), localeString);
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

	public static ADecoratorPropertiesDefinition getWrapper(ADecoratorPropertiesDefinition pd, String locale) {
		AspectJProxyFactory pf = new AspectJProxyFactory(pd);
		pf.setProxyTargetClass(true);
		pf.addAdvice(
				new PropertyDefintionI18NWrapper(pd.getAnagraficaHolderClass().getSimpleName(), pd.getShortName(), locale));
		return pf.getProxy();
	}

	public static PropertiesDefinition getWrapper(PropertiesDefinition pd, String locale) {
		AspectJProxyFactory pf = new AspectJProxyFactory(pd);
		pf.setProxyTargetClass(true);
		pf.addAdvice(
				new PropertyDefintionI18NWrapper(pd.getAnagraficaHolderClass().getSimpleName(), pd.getShortName(), locale));
		return pf.getProxy();
	}
}
