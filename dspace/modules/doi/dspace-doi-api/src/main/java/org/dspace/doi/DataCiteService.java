package org.dspace.doi;

import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.RemoteException;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;

/**
 * This class isn't used anymore; it was the first of our remote DOI
 * registration services, but we now use the CDLDataCiteService for registering
 * our DOIs. I've kept it around in the svn repository for informational
 * purposes, but it can be deleted if need be.
 * 
 * @author Kevin S. Clarke <a
 *         href="mailto:ksclarke@nescent.org">ksclarke@nescent.org</a>
 * 
 */
public class DataCiteService {

	private static final String ENDPOINT = "https://doi.tib.uni-hannover.de:8443/cocoon/regserv/datacite/webserviceinterface/";
	private static final QName ECHO = new QName("RegServ", "echo");
	private static final QName REG_DOI = new QName("RegServ", "registerDataDOI");
	private static final QName UPDATE_DOI = new QName("RegServ", "update");

	private String myUsername;
	private String myPassword;

	public DataCiteService(String aUsername, String aPassword) {
		myUsername = aUsername;
		myPassword = aPassword;
	}

	public String registerDOI(String aDOI, String aURL) throws RemoteException,
			ServiceException {
		Service service = new Service(); // not thread safe
		Call call = (Call) service.createCall();

		try {
			call.setTargetEndpointAddress(new URL(ENDPOINT));
		}
		catch (MalformedURLException details) {
			throw new RuntimeException(details);
		}

		call.setUsername(myUsername);
		call.setPassword(myPassword);

		call.setOperationName(REG_DOI);
		return (String) call.invoke(new Object[] { aDOI, aURL });
	}

	public String updateURL(String aDOI, String aURL) throws RemoteException,
			ServiceException {
		Service service = new Service(); // not thread safe
		Call call = (Call) service.createCall();

		try {
			call.setTargetEndpointAddress(new URL(ENDPOINT));
		}
		catch (MalformedURLException details) {
			throw new RuntimeException(details);
		}

		call.setUsername(myUsername);
		call.setPassword(myPassword);

		return (String) call.invoke(UPDATE_DOI, new Object[] { aDOI, aURL });
	}

	public String echo(String aMessage) throws RemoteException,
			ServiceException {
		Service service = new Service(); // not thread safe
		Call call = (Call) service.createCall();

		try {
			call.setTargetEndpointAddress(new URL(ENDPOINT));
		}
		catch (MalformedURLException details) {
			throw new RuntimeException(details);
		}

		call.setUsername(myUsername);
		call.setPassword(myPassword);

		return (String) call.invoke(ECHO, new Object[] { aMessage });
	}

}

/*
 * 
 * First, visit URL in browser, accept cert, save cert to file system; then (all
 * in one command):
 * 
 * sudo keytool -import -alias datacite -file TIB.cer -keystore
 * /usr/lib/jvm/default-java/jre/lib/security/cacerts
 * 
 * The default password for the cacert for the Sun JVM is 'changeit' (if you
 * haven't already)
 */