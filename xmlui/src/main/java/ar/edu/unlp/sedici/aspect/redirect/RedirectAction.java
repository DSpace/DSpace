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

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.sitemap.PatternException;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Attempt to authenticate the user based upon their presented credentials. 
 * This action uses the http parameters of login_email, login_password, and 
 * login_realm as credentials.
 * 
 * If the authentication attempt is successfull then an HTTP redirect will be
 * sent to the browser redirecting them to their original location in the 
 * system before authenticated or if none is supplied back to the DSpace 
 * homepage. The action will also return true, thus contents of the action will
 * be excuted.
 * 
 * If the authentication attempt fails, the action returns false.
 * 
 * Example use:
 * 
 * <map:act name="Authenticate">
 *   <map:serialize type="xml"/>
 * </map:act>
 * <map:transform type="try-to-login-again-transformer">
 *
 * @author Scott Phillips
 */

public class RedirectAction extends AbstractAction
{

	private static Map<String, Map<String, Object>> properties=new HashMap<String, Map<String,Object>>();
	private static Map<String, String> urls=null;

    public static Map<String, Map<String, Object>> getProperties() {    	
		   return properties;
	}

	public static void setProperties(
			Map<String, Map<String, Object>> properties2) {
		properties = properties2;
	}
	
	private static void generarProperties(String prefijo) {
		// Genera el hashMap para las properties
		HashMap<String, Object> retorno=new HashMap<String, Object>();
		String clave;
		String valor;
		Boolean valorBooleano;
		//cargo la key de la nueva url y el valor
		clave=prefijo+".new_url";
		valor=ConfigurationManager.getProperty("sedici.redirect", clave);
		retorno.put(clave,  valor);
		//cargo los parametros
		int inicial=1;
		clave=prefijo+".params."+inicial+".old_name";
		valor=ConfigurationManager.getProperty("sedici.redirect", clave);
		while (valor!=null){
			//agrego el nombre del parametro viejo al map
			retorno.put(clave,  valor);
			clave=prefijo+".params."+inicial+".url_part";
			valorBooleano=ConfigurationManager.getBooleanProperty("sedici.redirect", clave);
			retorno.put(clave, valorBooleano);
			if (!valorBooleano){
				//si no es parte de la url el parametro, tengo que guardar el new name
				clave=prefijo+".params."+inicial+".new_name";
				valor=ConfigurationManager.getProperty("sedici.redirect", clave);
				retorno.put(clave,  valor);
			}
			//recupero el proximo parametro
			inicial+=1;
			clave=prefijo+".params."+inicial+".old_name";
			valor=ConfigurationManager.getProperty("sedici.redirect", clave);
		}
       properties.put(prefijo,  retorno);
		
	}

	public static Map<String, String> getUrls() {
    	if (urls==null){
    		return RedirectAction.generarUrls();
    	} else {
		   return urls;
    	}
	}
	
	public static void setUrls(Map<String, String> urls2) {
		urls = urls2;
	}

	private static Map<String, String> generarUrls() {
		int inicial=1;
		Map<String, String> retorno=new HashMap<String, String>();
		String valor=ConfigurationManager.getProperty("sedici.redirect", "map.url"+inicial);
		while (valor!=null){
			retorno.put(valor, "map.url"+inicial);
			inicial+=1;
			valor=ConfigurationManager.getProperty("sedici.redirect", "map.url"+inicial);
		}
		RedirectAction.setUrls(retorno);
		return retorno;
		
	}



	public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        String old_url = request.getParameter("old_url");
        
        //recupero la clave para la vieja url
        String prefijo=this.recuperarClave(old_url);
        
        Map<String, Object> propertiesPrefijo=this.recuperarProperties(prefijo);
        
        //recupero los valores
        String new_url=(String)propertiesPrefijo.get(prefijo+".new_url");
        
        //recupero la cantidad de parámetros que van a ser pasados al redirect   
    	Hashtable<String, String> new_url_params=new Hashtable<String, String>();
    	
        int inicial=1;
        String param_old_name="";
        String param_new_name="";
    	Boolean url_part=false;
    	String param_value="";
    	String prefijo_parametro=prefijo+".params."+inicial;

        while (propertiesPrefijo.containsKey(prefijo_parametro+".old_name")){
            param_old_name=(String)propertiesPrefijo.get(prefijo_parametro+".old_name");
        	
    		//debo verificar si el parametro es parte de la url o se pasa como parámetro.
    		//IMPORTANTE: aca podriamos extender el config en el caso de que varios parametros puedan formar parte de la url, para
    		//            agregarle el orden y escribir bien la URL nueva.
    		//            En este caso, solo UN parámetro es parte de la url, los demás se pasan como parámetros.
    		url_part=(Boolean)propertiesPrefijo.get(prefijo_parametro+".url_part");
    		
    		//recupero del request el valor del parametro con nomre old_param
    		param_value=request.getParameter(param_old_name);
    		
			if (!url_part){
				//si no es parte de la url se agrega como parametro a la url
        		param_new_name=(String)propertiesPrefijo.get(prefijo_parametro+".new_name");
        		new_url_params.put(param_new_name, param_value);
			}else{
				new_url=new_url+"/"+param_value;
			}
			inicial+=1;
	    	prefijo_parametro=prefijo+".params."+inicial;

		};
		//recorro el hashtable creado con los parametros y lo agrego a la url
		Enumeration<String> claves_params=new_url_params.keys();
		Boolean first_element=true;
		String clave_param;
		//agrego los parametros a la nueva url
		while (claves_params.hasMoreElements()) {
			if (first_element){
				new_url+="?";
				first_element=false;
			} else {
				new_url+="&";
			};
			clave_param=claves_params.nextElement();
			new_url+=clave_param+"="+new_url_params.get(clave_param);
		};

        
        String redirectURL = request.getContextPath()+ new_url;
        
        System.out.println(redirectURL);
        
        final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
        
        httpResponse.sendRedirect(redirectURL);
        
        return null;
    }
    
    private Map<String, Object> recuperarProperties(String prefijo) {
		if (!RedirectAction.getProperties().containsKey(prefijo)){
			RedirectAction.generarProperties(prefijo);
		};
		return RedirectAction.getProperties().get(prefijo);
	}

	/*
     * Recorro las properties en búsqueda de la clave que tiene como valor una url dada.
     * Retorno la key en caso de existir, y null en caso contrario
     */
    public String recuperarClave(String old_url){
    	return this.getUrls().get(old_url);

    	/*Hashtable<Object, Object> propiedades=ConfigurationManager.getProperties("sedici.redirect");
    	Enumeration<Object> claves=propiedades.keys();
    	boolean encontrado=false;
    	String valor=null;
    	String clave_retorno=null;
    	while (claves.hasMoreElements()){
    		clave_retorno=(String)claves.nextElement();
    		valor=(String)(propiedades.get(clave_retorno));
    		if (valor.equals(old_url)){    			
    			return clave_retorno;
    		}
    	};
    	return null;*/
    }
}
