package cz.cuni.mff.ufal.lindat.utilities.interfaces;

import java.util.Date;
import java.util.List;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.GenericEntity;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseDefinition;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseLabel;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceMapping;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;

public interface ILicenses {

	/**
	 * Function discovers whether the user is allowed to access the resource.
	 * 
	 * @param userID
	 *            is the User obtained from the underlying architecture
	 * @param resourceID
	 *            is the Resource obtained from the underlying architecture
	 * @return true if the resource is not protected by any License or if the
	 *         user already signed the required licenses
	 * 
	 */
	public boolean isUserAllowedToAccessTheResource(int userID, int resourceID);

	/**
	 * Function returns the Licenses that user required to agree to access the
	 * resource
	 * 
	 * @param userID
	 *            is the User obtained from the underlying architecture
	 * @param resourceID
	 *            is the Resource obtained from the underlying architecture
	 */

	public List<LicenseDefinition> getLicensesToAgree(int userID, int resourceID);

	/**
	 * Function attach a license to a resource.
	 * 
	 * @param licenseID
	 *            the license to attach
	 * @param resourceID
	 *            the resource to which the license is to be attached
	 * @return true if the operation succeeded
	 */
	public boolean attachLicense(int licenseID, int resourceID);
	
	
	/**
	 * Function detach all licenses from a resource.
	 * 
	 * @param resourceID
	 *            the resource from which the license is to be detached
     */
	public boolean detachLicenses(int resourceID);
	
	
	/**
	 * returns all licenses available in the system
     */
	public List<LicenseDefinition> getAllLicenses();
	
	/**
	 * returns all license labels
     */
	public List<LicenseLabel> getAllLicenseLabels();

	/**
	 * returns all extended license labels
     */
	public List<LicenseLabel> getAllLicenseLabelsExtended();

	
	/**
	 * returns license by given name
     */
	public LicenseDefinition getLicenseByName(String name);
	

	/**
	 * returns license by ID
     */
	public LicenseDefinition getLicenseByID(int id);


	/**
	 * returns license by definition (URI)
     */
	public LicenseDefinition getLicenseByDefinition(String definition);

	
	/**
	 * Return licenses associated with resource.
	 * 
	 * @param resourceID
	 *            is the Resource obtained from the underlying architecture
     */
	public List<LicenseDefinition> getLicenses(int resourceID);

	/**
	 * Function satisfy the need of the user to agree the license.
	 * 
	 * @param licenseID
	 *            is the license specification identifier
	 * @param resourceID
	 *            is the resource specification identifier
	 * @param userID
	 *            is the user specification identifier
	 * @return true if the process succeeds
	 */
	boolean agreeLicense(int licenseID, int resourceID, int userID);

	/**
	 * Function creates new license
	 *
	 * @param name
	 * 			the name of the license
	 * @param userID
	 * 			the underlying user creating the license
	 * @param definition
	 * @param labelID
	 * 			label associated with the license
	 * @return true if all went well.
	 */
	public boolean defineLicense(String name, int userID, String definition, int confirmation, String requiredInfo, int labelID);

	/**
	 * Function removes a license.
	 *
	 * @param licenseID
	 * @return true if all went well.
	 */
	public boolean removeLicense(int licenseID);

    
	/**
     * Function records the access of given resource
     * 
     * @param userID is an identifier of the user
     * @param resourceID is an identifier of the resource 
     */
	public void updateFileDownloadStatistics(int userID, int resourceID);


	/**
	 * Function gets the record of given type from the database by id
	 * @param c
	 * @return id
	 */
	public GenericEntity findById(Class c, int id);
	
	public List findByCriterie(Class c, Object ... criterion);

	
	/**
	 * Function gets all the records of given type from the database
	 * @param c
	 * @return List of objects of given type
	 */
	public List getAll(Class c);


	/**
	 * Function persist record of given type
	 * @param c
	 * @param object
	 */
	public boolean persist(Class c, GenericEntity object);

	/**
	 * Function update record of given type
	 * @param c
	 * @param object
	 */
	public boolean update(Class c, GenericEntity object);
	
	public boolean saveOrUpdate(Class c, GenericEntity object);
		
	/**
	 * Function delete record of given type
	 * @param c
	 */
	public boolean delete(Class c, GenericEntity object);
	

	/**
	 * Function number of resources a license is attached
	 * @param licenseID
	 */
	public int getLicenseResources(int licenseID);

	
	/**
	 * Function creates new license label
	 *
	 * @param code
	 * 			the short label of the license
	 * @param title
	 * 			the long license label description
	 * @param isExtended
	 * 			if the label is main label or extended extra label
	 * @return true if all went well.
	 */	
	public boolean defineLicenseLabel(String code, String title, boolean isExtended);
	
	public List<LicenseResourceUserAllowance> getSignedLicensesByDate();
	
	public List<LicenseResourceUserAllowance> getSignedLicensesByUser(int eperson_id);
	
	public boolean addUserMetadata(int epersonId, String key, String value);
	
	public boolean addUserMetadata_licenseSigning(int epersonId, String key, String value, int transaction_id);
	
	public List<UserMetadata> getUserMetadata(int epersonId);
	
	public UserMetadata getUserMetadata(int epersonId, String key);
	
	public List<UserMetadata> getUserMetadata_License(int epersonId, int transaction_id);
	
	public UserMetadata getUserMetadata_License(int epersonId, String key, int transaction_id);
			
	public List<LicenseResourceMapping> getAllMappings(int bitstreamId);
	
	public boolean verifyToken(int bitstreamId, String token);

	public String getToken(int epersonId, int bitstreamId);
}



