/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class AllCertsTrustManager implements X509TrustManager {

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType)
			throws CertificateException {		
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
	
	public static SSLSocketFactory getSocketFactory() {
		TrustManager[] trustAllCerts = new TrustManager[]{new AllCertsTrustManager()};
		SSLContext sc = null;
		try {
		sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch(GeneralSecurityException gse) {
			throw new IllegalStateException(gse.getMessage());
		}
		return sc.getSocketFactory();
	}
	
	public static HostnameVerifier getHostNHostnameVerifier() {
		return new HostnameVerifier()
			    {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
			    };
	}
	
}

