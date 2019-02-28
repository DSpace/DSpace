package org.dspace.app.webui.cris.web.tag;

import java.util.Locale;
import java.util.MissingResourceException;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.dspace.core.I18nUtil;

public final class WidgetCheckRadioI18NWrapper implements MethodInterceptor {
	private Locale locale = null;
	private String simpleName = null;
	private String shortName = null;

	public WidgetCheckRadioI18NWrapper(String simpleName, String shortName, Locale locale) {
		this.locale = locale;
		this.simpleName = simpleName;
		this.shortName = shortName;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		if (locale != null) {
			String name = invocation.getMethod().getName();
			if (name.equals("getStaticValues")) {
				return getStaticValues(invocation);
			} 			
		}
		return invocation.proceed();
	}

	private Object getStaticValues(MethodInvocation invocation) throws Throwable {
		String originalValues = (String) invocation.proceed();
		StringBuffer sb = new StringBuffer();
		for (String val : originalValues.split("\\|\\|\\|")) {
			if (sb.length() > 0) {
				sb.append("|||");
			}
			String[] k = val.split("###");
			sb.append(k[0]);
			String label = k.length > 1? k[1]:k[0];
			try {
				label = I18nUtil.getMessage(simpleName + "." + shortName + ".label."+k[0], locale, true);
			} catch (MissingResourceException mre) {
			}
			sb.append("###");
			sb.append(label);
		}
		return sb.toString();
	}
}
