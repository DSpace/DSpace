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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.location.LocatableException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;


public class ExceptionAction extends AbstractAction
{

	private static final Logger log = Logger.getLogger(ExceptionAction.class);
	private static final String PROPERTY_MAIL_RECIPIENT = "exceptionAction.mailRecipient";


	public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {

		Throwable thr;
	    Context context= ContextUtil.obtainContext(objectModel);
	    
		thr = ObjectModelHelper.getThrowable(objectModel);
		Request request = ObjectModelHelper.getRequest(objectModel);
		//mando el mail al administrador del sitio
		this.sendMail(context, request, thr);

        return null;
    }
	
	/*
	 * Parameters: 
	 * {0} DSpace server URL
	 * {1} Date & time
	 * {2} Session ID + IP
	 * {3} URL + HTTP parameters, if any
	 * {4} Exception stack trace
	 * {5} User details
	 */
	private void sendMail(Context context, Request request, Throwable thr){
		 try {
			 
             String recipient = ConfigurationManager
                     .getProperty("sedici-dspace", PROPERTY_MAIL_RECIPIENT);
 
             if (recipient != null) {
                 Email email = ConfigurationManager
                         .getEmail(I18nUtil.getEmailFilename(
                                 context.getCurrentLocale(), "exception_error"));
                 email.addRecipient(recipient);
                 
                 email.addArgument(ConfigurationManager
                         .getProperty("dspace.url"));
                 email.addArgument(new Date());
                 email.addArgument(context.getExtraLogInfo());
                 
                 String url=request.getRequestURI();
                 if (request.getParameters().size()>0){
                	 url=url+"?";
                	 Enumeration parametros=request.getParameterNames();
                	 while (parametros.hasMoreElements()) {
						String param = (String) parametros.nextElement();
						url=url + param + "=" + request.getParameter(param);
						if (parametros.hasMoreElements()){
							url=url+"&";
						};
                	 }
                 };                 
                 email.addArgument(url);
 
                 String stackTrace;
 
                 if (thr != null) {
                     StringWriter sw = new StringWriter();
                     PrintWriter pw = new PrintWriter(sw);
                     thr.printStackTrace(pw);
                     pw.flush();
                     stackTrace = sw.toString();
                 } else {
                     stackTrace = "No exception";
                     System.out.println("-------No hay excepción---------");
                 }
 
                 email.addArgument(stackTrace);
                 if (context.getCurrentUser()!=null){
                	 email.addArgument(context.getCurrentUser().getFullName() + " - ID: " + context.getCurrentUser().getID() + " - email: "+ context.getCurrentUser().getEmail());
                 } else {
                	 email.addArgument("Anónimo");
                 }                 
                 email.send();
             }
         } catch (Exception e) {
             //capturamos la excepcion de que no pudo mandar el error        	 
             log.warn("Incapaz de mandar mail de excepcion", e);
         }
	}
   

}
