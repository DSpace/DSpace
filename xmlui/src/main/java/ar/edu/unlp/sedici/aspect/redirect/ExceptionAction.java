/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.aspect.redirect;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;

public class ExceptionAction extends AbstractAction {

	private static final String SEDICI_MODULE = "sedici-dspace";

	private static final String PROPERTY_MAIL_RECIPIENT = "exceptionAction.mailRecipient";
	private static final String PROPERTY_DEBUG_MODE = "xmlui.debug";

	public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) {
		boolean debug;
		try {
			debug = ConfigurationManager.getBooleanProperty(SEDICI_MODULE, PROPERTY_DEBUG_MODE);
		} catch (Exception e) {
			this.getLogger().error(
					"Fallo la recuperación de la property " + PROPERTY_DEBUG_MODE + "del modulo " + SEDICI_MODULE
							+ " en el action 'ExceptionAction'", e);
			debug = false;
		}

		if (!debug) {

			String recipient = ConfigurationManager.getProperty(SEDICI_MODULE, PROPERTY_MAIL_RECIPIENT);

			if (recipient != null) {
				// mando el mail al administrador del sitio
				try {
					this.sendMail(objectModel, recipient);
				} catch (Exception e) {
					// capturamos la excepcion de que no pudo mandar el error
					this.getLogger().error("Se produjo una exception al tratar de mandar el mail de excepcion a "+recipient, e);
				}
			}else {
				this.getLogger().info("No mando el mail por que exceptionAction.mailRecipient esta vacio");
			}
		} else {
			this.getLogger().info("No mando el mail por la exception porque xmlui.debug es true");
		}
		return null;
	}

	/*
	 * Parameters: {0} DSpace server URL {1} Date & time {2} Session ID + IP {3}
	 * URL + HTTP parameters, if any {4} Exception stack trace {5} User details
	 */
	private void sendMail(Map objectModel, String recipient) throws SQLException, MessagingException, IOException {
		Context context = ContextUtil.obtainContext(objectModel);
		Throwable thr = ObjectModelHelper.getThrowable(objectModel);
		Request request = ObjectModelHelper.getRequest(objectModel);

		Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "exception_error"));
		email.addRecipient(recipient);

		email.addArgument(ConfigurationManager.getProperty("dspace.url"));
		email.addArgument(new Date());
		email.addArgument(context.getExtraLogInfo());

		String url = request.getRequestURI();
		if (request.getParameters().size() > 0) {
			url = url + "?";
			Enumeration parametros = request.getParameterNames();
			while (parametros.hasMoreElements()) {
				String param = (String) parametros.nextElement();
				url = url + param + "=" + request.getParameter(param);
				if (parametros.hasMoreElements()) {
					url = url + "&";
				}
				;
			}
		}
		;
		email.addArgument(url);

		String stackTrace;

		if (thr != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			thr.printStackTrace(pw);
			pw.flush();
			stackTrace = sw.toString();
		} else {
			stackTrace = "No exception (mmmm )";
			this.getLogger().error("En teoria, se produjo una exception, pero no se puede recuperar la excepcion para imprimir su stack trace. Es raro");
		}
		email.addArgument(stackTrace);

		if (context.getCurrentUser() != null) {
			email.addArgument(context.getCurrentUser().getFullName() + " - ID: " + context.getCurrentUser().getID() + " - email: "
					+ context.getCurrentUser().getEmail());
		} else {
			email.addArgument("Anónimo");
		}

		email.send();

	}

}
