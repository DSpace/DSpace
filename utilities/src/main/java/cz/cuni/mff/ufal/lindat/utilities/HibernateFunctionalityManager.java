package cz.cuni.mff.ufal.lindat.utilities;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import cz.cuni.mff.ufal.lindat.utilities.functionalities.ShibbolethScopeAffiliation;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.GenericEntity;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.HibernateUtil;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseFileDownloadStatistic;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceMapping;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.VerificationTokenEperson;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;
import cz.cuni.mff.ufal.lindat.utilities.units.Variables;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class HibernateFunctionalityManager implements IFunctionalities {

	private static Logger log = Logger.getLogger(HibernateFunctionalityManager.class);

	HibernateUtil hibernateUtil = new HibernateUtil();
	
	public HibernateFunctionalityManager() {
		init();
	}
	
	@Override
	public void setErrorMessage(String message) {
		Variables.setErrorMessage(message);
	}

	@Override
	public String getErrorMessage() {
		return Variables.getErrorMessage();
	}
	
	@Override
	public String get(String key) {
		String property = Variables.get(key);
		if (property != null)
			return property;
		return "";
	}

	public static void init() {
		Variables.init();
	}

	public  boolean isFunctionalityEnabled(String functionalityName) {
		// this logic needs inspection
		if(!Variables.isConfigurationTrue(functionalityName)){
			log.log(Level.FATAL,"Functionality " + functionalityName + " is not enabled!");
			return false;
		}
		return true;
	}


	@Override
	public List<LicenseDefinition> getLicensesToAgree(int userID, int resourceID) {

		Criterion criterion1 = Restrictions.eq("bitstreamId", resourceID);
		Criterion criterion2 = Restrictions.eq("active", true);
		
		List<LicenseDefinition> result = null;

		List<LicenseResourceMapping> mapping1 = (List<LicenseResourceMapping>) hibernateUtil
				.findByCriterie(LicenseResourceMapping.class, criterion1, criterion2);

		if (mapping1 != null && mapping1.size() != 0) {

			LicenseDefinition license = mapping1.get(0).getLicenseDefinition();
			result = new ArrayList<LicenseDefinition>();

			switch (license.getConfirmation()) {

			case 2:
				result.add(license);
				break;
			case 0:
				result = null;
				break;
			case 1:

				String query = "FROM LicenseResourceMapping mp"
						+ " LEFT JOIN mp.licenseResourceUserAllowances al"
						+ " WITH al.userRegistration.epersonId=:epersonId"
						+ " WHERE mp.bitstreamId=:bitstreamId AND al.transactionId is null AND mp.active=true";

				Hashtable<String, Object> params = new Hashtable<String, Object>();
				params.put("bitstreamId", resourceID);
				params.put("epersonId", userID);

				List<Object[]> tempRes = (List<Object[]>) hibernateUtil.findByQuery(query, params);
				for (Object[] res : tempRes) {
					if (res != null) {
						result.add(license);
						break;
					}
				}
				break;
			}

		}

		return result;

	}

	@Override
	public boolean isUserAllowedToAccessTheResource(int userID, int resourceID) {
		List<LicenseDefinition> result = getLicensesToAgree(userID, resourceID);
		return result == null || result.size() == 0;
	}

	@Override
	public boolean attachLicense(int licenseID, int resourceID) {
		LicenseDefinition license = new LicenseDefinition();
		license.setLicenseId(licenseID);
		LicenseResourceMapping mapping = new LicenseResourceMapping();
		mapping.setBitstreamId(resourceID);
		mapping.setLicenseDefinition(license);

		try {
			hibernateUtil.persist(LicenseResourceMapping.class, mapping);
		} catch (RuntimeException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	@Override
	public boolean detachLicenses(int resourceID) {
		try {
			/* hibernateUtil.deleteByCriteria(LicenseResourceMapping.class,
					Restrictions.eq("bitstreamId", resourceID));*/
			// only mark inactive
			
			List<LicenseResourceMapping> mappings = (List<LicenseResourceMapping>)hibernateUtil
					.findByCriterie(LicenseResourceMapping.class, 
							Restrictions.eq("bitstreamId", resourceID));
			if(mappings != null) {
				for(LicenseResourceMapping mapping : mappings) {
					mapping.setActive(false);
					hibernateUtil.update(LicenseResourceMapping.class, mapping);
				}
			}
			
		} catch (RuntimeException e) {
			log.error(e);
			return false;
		}

		return true;
	}

	@Override
	public List<LicenseDefinition> getAllLicenses() {
		List<LicenseDefinition> results = (List<LicenseDefinition>) hibernateUtil
				.findAll(LicenseDefinition.class);
		return results;
	}

	@Override
	public List<LicenseDefinition> getLicenses(int resourceID) {
		List<LicenseResourceMapping> mappings = (List<LicenseResourceMapping>) hibernateUtil
				.findByCriterie(LicenseResourceMapping.class,
						Restrictions.eq("bitstreamId", resourceID),
						Restrictions.eq("active", true));
		List<LicenseDefinition> results = new ArrayList<LicenseDefinition>();
		for (LicenseResourceMapping mapping : mappings) {
			results.add(mapping.getLicenseDefinition());
		}
		return results;
	}

	@Override
	public boolean agreeLicense(int licenseID, int resourceID, int userID) {
		Criterion license = Restrictions.eq("licenseDefinition.licenseId",licenseID);
		Criterion bitstream = Restrictions.eq("bitstreamId", resourceID);
		Criterion active = Restrictions.eq("active", true);
		List<LicenseResourceMapping> mappings = hibernateUtil.findByCriterie(
				LicenseResourceMapping.class,
				Restrictions.and(license, bitstream, active));

		UserRegistration userRegistration = new UserRegistration();
		userRegistration.setEpersonId(userID);

		for (LicenseResourceMapping mapping : mappings) {			
			LicenseResourceUserAllowance allowance = new LicenseResourceUserAllowance();
			allowance.setLicenseResourceMapping(mapping);
			allowance.setUserRegistration(userRegistration);
			allowance.setCreatedOn(new Date());
			try {
				hibernateUtil.saveOrUpdate(LicenseResourceUserAllowance.class,
						allowance);
			} catch (RuntimeException e) {
				log.error(e);
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean defineLicense(String name, int userID, String definition,
			int confirmation, String requiredInfo, int labelID) {
		LicenseDefinition license = new LicenseDefinition();
		license.setName(name);
		license.setDefinition(definition);
		license.setConfirmation(confirmation);
		license.setRequiredInfo(requiredInfo);

		UserRegistration user = new UserRegistration();
		user.setEpersonId(userID);

		LicenseLabel label = new LicenseLabel();
		label.setLabelId(labelID);

		license.setUserRegistration(user);
		license.setLicenseLabel(label);

		try {
			hibernateUtil.persist(LicenseDefinition.class, license);
		} catch (RuntimeException e) {
			Variables.setErrorMessage(e.getLocalizedMessage());
			log.error(e);
			return false;
		}

		return true;
	}

	@Override
	public boolean removeLicense(int licenseID) {
		try {
			hibernateUtil.deleteById(LicenseDefinition.class, licenseID);
		} catch (RuntimeException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	@Override
	public void updateFileDownloadStatistics(int userID, int resourceID) {
		UserRegistration userReg = new UserRegistration();
		userReg.setEpersonId(userID);
		LicenseFileDownloadStatistic licenseStat = new LicenseFileDownloadStatistic();
		licenseStat.setUserRegistration(userReg);
		licenseStat.setBitstreamId(resourceID);
		hibernateUtil.persist(LicenseFileDownloadStatistic.class, licenseStat);
	}

	@Override
	public LicenseDefinition getLicenseByName(String name) {
		List<LicenseDefinition> results = (List<LicenseDefinition>) hibernateUtil
				.findByCriterie(LicenseDefinition.class,
						Restrictions.eq("name", name));
		if (results != null && results.size() > 0)
			return results.get(0);
		else
			return null;
	}

	@Override
	public LicenseDefinition getLicenseByID(int id) {
		return (LicenseDefinition) hibernateUtil.findById(
				LicenseDefinition.class, id);
	}

	@Override
	public LicenseDefinition getLicenseByDefinition(String definition) {
		List<LicenseDefinition> results = (List<LicenseDefinition>) hibernateUtil
				.findByCriterie(LicenseDefinition.class,
						Restrictions.eq("definition", definition));
		if (results != null && results.size() > 0)
			return results.get(0);
		else
			return null;
	}

	@Override
	public GenericEntity findById(Class c, int id) {
		return hibernateUtil.findById(c, id);
	}	

	
	@Override
	public List getAll(Class c) {
		return hibernateUtil.findAll(c);
	}

	@Override
	public boolean persist(Class c, GenericEntity object) {
		try {
			hibernateUtil.persist(c, object);
		} catch (RuntimeException e) {
			Variables.setErrorMessage(e.getLocalizedMessage());
			log.error(e);
			return false;
		}
		return true;
	}

	@Override
	public boolean update(Class c, GenericEntity object) {
		try {
			hibernateUtil.update(c, object);
		} catch (RuntimeException e) {
			Variables.setErrorMessage(e.getLocalizedMessage());
			log.error(e);
			return false;
		}
		return true;
	}
	
	@Override
	public boolean saveOrUpdate(Class c, GenericEntity object) {
		try {
			hibernateUtil.saveOrUpdate(c, object);
		} catch (RuntimeException e) {
			Variables.setErrorMessage(e.getLocalizedMessage());
			log.error(e);
			return false;
		}
		return true;
	}


	@Override
	public boolean delete(Class c, GenericEntity object) {
		try {
			hibernateUtil.delete(c, object);
		} catch (RuntimeException e) {
			Variables.setErrorMessage(e.getLocalizedMessage());
			log.error(e);
			return false;
		}
		return true;
	}

	@Override
	public int getLicenseResources(int licenseID) {
		List<LicenseResourceMapping> mappings = (List<LicenseResourceMapping>) hibernateUtil
				.findByCriterie(LicenseResourceMapping.class,
						Restrictions.eq("licenseDefinition.licenseId", licenseID),
						Restrictions.eq("active", true));
		return mappings.size();
	}

	@Override
	public List<LicenseLabel> getAllLicenseLabels() {
		List<LicenseLabel> results = hibernateUtil.findByCriterie(
				LicenseLabel.class,
				Restrictions.eq("isExtended", Boolean.FALSE));
		return results;
	}

	@Override
	public List<LicenseLabel> getAllLicenseLabelsExtended() {
		List<LicenseLabel> results = (List<LicenseLabel>) hibernateUtil
				.findByCriterie(LicenseLabel.class,
						Restrictions.eq("isExtended", Boolean.TRUE));
		return results;
	}

	@Override
	public boolean defineLicenseLabel(String code, String title,
			boolean isExtended) {
		try {
			LicenseLabel label = new LicenseLabel();
			label.setLabel(code);
			label.setTitle(title);
			label.setIsExtended(isExtended);
			hibernateUtil.persist(LicenseLabel.class, label);
		} catch (RuntimeException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	// test connection
	public static void main(String args[]) {
		try {
			//expecting the user.dir is .../utilities
			String dspace_path = "file://" + System.getProperty("user.dir")
					+ "/../dspace/config/modules/lr.cfg";
			Variables.init(dspace_path);
			System.out
					.println(String
							.format("\nUsing dspace configuration from %s.\nTrying to connect to %s",
									dspace_path, Variables.databaseURL));
			SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
			Session session = sessionFactory.openSession();
			Query query = session.createSQLQuery("select current_date");
            List result = query.list();
            System.out.println(String.format("Current date selected from database is: %s", result.get(0)));
			session.close();
			sessionFactory.close();
		} catch (Exception e) {
			System.err.println("\nCould NOT connect to utilities database!\n");
			throw new RuntimeException("Could not connect to database - "
					+ e.toString());
		}

		System.out.println("\nConnection to utilities database successfull!\n");
	}

	@Override
	public List<LicenseResourceUserAllowance> getSignedLicensesByDate() {
		return (List<LicenseResourceUserAllowance>) hibernateUtil
				.findByCriterie(LicenseResourceUserAllowance.class,
						Order.desc("createdOn"));
	}

	@Override
	public List<LicenseResourceUserAllowance> getSignedLicensesByUser(
			int eperson_id) {
		return (List<LicenseResourceUserAllowance>) hibernateUtil
				.findByCriterie(LicenseResourceUserAllowance.class,
						Restrictions.eq("userRegistration.epersonId",
								eperson_id), Order.desc("createdOn"));
	}

	@Override
	public boolean addUserMetadata(int epersonId, String key, String value) {
		try {
			UserMetadata metadata = getUserMetadata(epersonId, key);
			if (metadata == null) {
				metadata = new UserMetadata();
				UserRegistration user = new UserRegistration();
				user.setEpersonId(epersonId);
				metadata.setUserRegistration(user);
				metadata.setMetadataKey(key);
				metadata.setMetadataValue(value);
				hibernateUtil.persist(UserMetadata.class, metadata);
			} else {
				metadata.setMetadataValue(value);
				hibernateUtil.saveOrUpdate(UserMetadata.class, metadata);
			}
			return true;
		} catch (Exception e) {
			log.error(e);
			return false;
		}
	}

	@Override
	public boolean addUserMetadata_licenseSigning(int epersonId, String key,
			String value, int transaction_id) {
		try {
			UserMetadata metadata = getUserMetadata_License(epersonId, key,
					transaction_id);
			if (metadata == null) {
				metadata = new UserMetadata();
				UserRegistration user = new UserRegistration();
				user.setEpersonId(epersonId);
				metadata.setUserRegistration(user);
				metadata.setMetadataKey(key);
				metadata.setMetadataValue(value);
				LicenseResourceUserAllowance allowance = new LicenseResourceUserAllowance();
				allowance.setTransactionId(transaction_id);
				metadata.setLicenseResourceUserAllowance(allowance);
				hibernateUtil.persist(UserMetadata.class, metadata);
			} else {
				metadata.setMetadataValue(value);
				hibernateUtil.saveOrUpdate(UserMetadata.class, metadata);
			}
			return true;
		} catch (Exception e) {
			log.error(e);
			return false;
		}
	}

	@Override
	public List<UserMetadata> getUserMetadata(int epersonId) {
		return (List<UserMetadata>) hibernateUtil.findByCriterie(
				UserMetadata.class, Restrictions.and(
						Restrictions.eq("userRegistration.epersonId", epersonId),
						Restrictions.isNull("licenseResourceUserAllowance")));
	}

	@Override
	public List<UserMetadata> getUserMetadata_License(int epersonId,
			int transaction_id) {
		return (List<UserMetadata>) hibernateUtil.findByCriterie(
				UserMetadata.class, Restrictions.and(
						Restrictions.eq("userRegistration.epersonId", epersonId),
						Restrictions.eq("licenseResourceUserAllowance.transactionId", transaction_id)));
	}

	@Override
	public UserMetadata getUserMetadata(int epersonId, String key) {
		List<UserMetadata> metadata = (List<UserMetadata>) hibernateUtil
				.findByCriterie(UserMetadata.class, Restrictions.and(
						Restrictions.eq("userRegistration.epersonId", epersonId),
						Restrictions.eq("metadataKey", key),
						Restrictions.isNull("licenseResourceUserAllowance")));
		if (metadata != null && metadata.size() > 0) {
			return metadata.get(0);
		}
		return null;
	}

	@Override
	public UserMetadata getUserMetadata_License(int epersonId, String key,
			int transaction_id) {
		List<UserMetadata> metadata = (List<UserMetadata>) hibernateUtil
				.findByCriterie(UserMetadata.class, Restrictions.and(
						Restrictions.eq("userRegistration.epersonId", epersonId),
						Restrictions.eq("metadataKey", key),
						Restrictions.eq("licenseResourceUserAllowance.transactionId", transaction_id)));
		if (metadata != null && metadata.size() > 0) {
			return metadata.get(0);
		}
		return null;
	}
	
	@Override
	public UserRegistration registerUser(int userID, String email, String organization, boolean confirmation) {
		UserRegistration userReg = (UserRegistration)hibernateUtil.findById(UserRegistration.class, userID);
		
		if(userReg==null) {
			userReg = new UserRegistration();
			userReg.setEpersonId(userID);
			userReg.setEmail(email);
			userReg.setOrganization(organization);
			userReg.setConfirmation(confirmation);
			try{
				hibernateUtil.persist(UserRegistration.class, userReg);
			} catch(RuntimeException e) {
				log.error(e);
				return null;
			}			
		}
				
		return userReg;
	}
	
	@Override
	public UserRegistration updateRegisteredUser(int userID, String email, String organization, boolean confirmation) {

		UserRegistration userReg = (UserRegistration)hibernateUtil.findById(UserRegistration.class, userID);
		
		try{
			userReg.setEmail(email);
			userReg.setOrganization(organization);
			userReg.setConfirmation(confirmation);
			hibernateUtil.saveOrUpdate(UserRegistration.class, userReg);
		} catch(RuntimeException e) {
			log.error(e);
			return null;
		}			
		return userReg;
	}

	
	@Override
	public UserRegistration getRegisteredUser(int userID) {
		UserRegistration userReg = (UserRegistration)hibernateUtil.findById(UserRegistration.class, userID);
		return userReg;
	}

	@Override
	public boolean verifyUser(int userID) {
		try{
			UserRegistration userReg = (UserRegistration)hibernateUtil.findById(UserRegistration.class, userID);
			userReg.setConfirmation(true);
			hibernateUtil.saveOrUpdate(UserRegistration.class, userReg);
		} catch(RuntimeException e) {
			log.error(e);
			return false;
		}
		return true;
	}

	@Override
	public String getEmailAcceptedOrNull(String email) {
		if (email == null || email.isEmpty() || email.matches(".*\\s+.*")){ //no whitespaces in mail
			return null;
		}
		return email;
	}

	@Override
	public String convert(String toConvert) {
		String inputEncoding = Variables.get("lr.shibboleth.name.conversion.inputEncoding");
		String outputEncoding = Variables.get("lr.shibboleth.name.conversion.outputEncoding");
		
		if(toConvert==null)
			toConvert = Variables.emptyName;
		
		String converted = toConvert;
		
		try {
			converted = new String(toConvert.getBytes(inputEncoding), outputEncoding);
		} catch (UnsupportedEncodingException e) {
			converted = toConvert;
		}
		
		return converted;
	}

	@Override
	public String scopeRecognition(String affiliation) {
		return ShibbolethScopeAffiliation.scopeRecognition(affiliation);
	}

	@Override
	public void openSession() {
		hibernateUtil.openSession();
	}

	@Override
	public void closeSession() {
		hibernateUtil.closeSession();		
	}

	@Override
	public List findByCriterie(Class c, Object... criterion) {
		return hibernateUtil.findByCriterie(c, criterion);
	}

	@Override
	public VerificationTokenEperson getVerificationToken(String token) {		
		List<VerificationTokenEperson> result = (List<VerificationTokenEperson>)hibernateUtil.findByCriterie(VerificationTokenEperson.class, Restrictions.eq("token", token));
		if(result!=null && result.size()>0) {
			return result.get(0);
		}
		return null;
	}

	@Override
	public VerificationTokenEperson getVerificationTokenByEmail(String email) {
		List<VerificationTokenEperson> result = (List<VerificationTokenEperson>)hibernateUtil.findByCriterie(VerificationTokenEperson.class, Restrictions.eq("email", email));
		if(result!=null && result.size()>0) {
			return result.get(0);
		}
		return null;

	}

	@Override
	public List<LicenseResourceMapping> getAllMappings(int bitstreamId) {
		return (List<LicenseResourceMapping>)hibernateUtil.findByCriterie(LicenseResourceMapping.class,
				Restrictions.eq("bitstreamId", bitstreamId),
				Restrictions.eq("active", true));
	}

	@Override
	public boolean verifyToken(int bitstreamId, String token) {

		String query = "FROM LicenseResourceUserAllowance al"
				+ " JOIN al.licenseResourceMapping mp"
				+ " WITH mp.bitstreamId=:bitstreamId AND mp.active=true"
				+ " WHERE al.token=:token AND al.createdOn>=:notGeneratedBefore";

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		//TODO read this from config fixed 30 days for now
		cal.add(Calendar.DAY_OF_MONTH, -30);
		
		Hashtable<String, Object> params = new Hashtable<String, Object>();
		params.put("bitstreamId", bitstreamId);
		params.put("notGeneratedBefore", cal.getTime());
		params.put("token", token);
		
		boolean tokenFound = false;

		List<Object[]> tempRes = (List<Object[]>) hibernateUtil.findByQuery(query, params);
		for (Object[] res : tempRes) {
			if (res != null) {
				tokenFound = true;
			}
		}
		
		return tokenFound;
	}
	
	@Override
	public String getToken(int epersonId, int bitstreamId) {
		
		String query = "FROM LicenseResourceUserAllowance al"
				+ " JOIN al.licenseResourceMapping mp"
				+ " WITH mp.bitstreamId=:bitstreamId AND mp.active=true"
				+ " JOIN al.userRegistration ur"
				+ " WITH ur.epersonId=:epersonId";

		Hashtable<String, Object> params = new Hashtable<String, Object>();
		params.put("bitstreamId", bitstreamId);
		params.put("epersonId", epersonId);
		
		String token = null;
		
		List<Object[]> tempRes = (List<Object[]>) hibernateUtil.findByQuery(query, params);
		
		if(tempRes!=null && !tempRes.isEmpty()) {
			Object[] tempZero = tempRes.get(0);
			LicenseResourceUserAllowance allowance = (LicenseResourceUserAllowance)tempZero[0];
			token = allowance.getToken();
		}
				
		return token;
	}
	
	/**
	 * Allow resource leaks detection
	 */
	public void close(){
		this.closeSession();
	}
}



