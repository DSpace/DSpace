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

import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;

import ar.edu.unlp.sedici.dspace.utils.MailReporter;

/**
 * Clase encargada de reportar las excepciones que se detectan durante la
 * ejecucion de un pipeline cocoon
 */
public class ExceptionAction extends AbstractAction {
	public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) {

		Context context;
		try {
			context = ContextUtil.obtainContext(objectModel);
		} catch (SQLException e) {
			this.getLogger().error("Se produjo un error al levantar el context desde ExceptionAction", e);
			return null;
		}

		Request request = ObjectModelHelper.getRequest(objectModel);
		String url = request.getRequestURI();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MapUtils.verbosePrint(new PrintStream(os), "Parameters:", request.getParameters());

		Throwable thr = ObjectModelHelper.getThrowable(objectModel);
		if (thr == null) {
			this.getLogger().error(
					"En teoria, se produjo una exception, pero no se puede recuperar la excepcion para imprimir su stack trace. Es raro");
		} else {
			MailReporter.reportUnknownException(context, "Error en xmlui", thr, url, os.toString());
		}
		return null;

	}

}
