/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
importClass(Packages.javax.mail.internet.AddressException);

importClass(Packages.org.dspace.app.xmlui.utils.FlowscriptUtils);

importClass(Packages.org.dspace.core.ConfigurationManager);
importClass(Packages.org.dspace.core.Context);
importClass(Packages.org.dspace.authorize.AuthorizeException);
importClass(Packages.org.dspace.eperson.factory.EPersonServiceFactory);
importClass(Packages.org.dspace.content.factory.ContentServiceFactory);
importClass(Packages.java.util.UUID)


importClass(Packages.org.dspace.app.xmlui.utils.AuthenticationUtil);
importClass(Packages.org.dspace.app.xmlui.utils.ContextUtil);

importClass(Packages.java.lang.String);

/**
 * This class defines the workflows for three flows within the EPerson aspect.
 * 
 * FIXME: add more documentation
 * 
 * @author Scott Phillips
 */
 
/** These functions should be common to all Manakin Flow scripts */
function getObjectModel() 
{
  return FlowscriptUtils.getObjectModel(cocoon);
}

function getDSContext()
{
	return ContextUtil.obtainContext(getObjectModel());
}

function getEPerson() 
{
    return getDSContext().getCurrentUser();
}

function getAccountService()
{
    return EPersonServiceFactory.getInstance().getAccountService();
}

function getEPersonService()
{
    return EPersonServiceFactory.getInstance().getEPersonService();
}

function getSubscribeService()
{
    return EPersonServiceFactory.getInstance().getSubscribeService();
}

function getCollectionService()
{
    return ContentServiceFactory.getInstance().getCollectionService();
}





/**
 * Perform a new user registration. 
 */
function doRegister() 
{ 
    // Make sure that user registration is enabled
    if (!(ConfigurationManager.getBooleanProperty("xmlui.user.registration", true)))
    {
        // We're configured to not allow user registration
        // User should only have gotten here by manually typing /register in URL, so we'll throw an error.
        throw new AuthorizeException("User registration is disabled");
    }
    
    var token = cocoon.request.get("token");
    
    if (token == null) 
    {
        // We have no token, this is the initial form. First ask the user for their email address
        // and then send them a token.
        var accountExists = false;
        var errors = new Array();
        do {
            var email = cocoon.request.getParameter("email");
        			
            cocoon.sendPageAndWait("register/start",{"email" : email, "errors" : errors.join(','), "accountExists" : accountExists});
            var errors = new Array();
            accountExists = false;
            
            var submit_forgot = cocoon.request.getParameter("submit_forgot");
            
            if (submit_forgot != null)
            {
                // The user attempted to register with an email address that already exists then they clicked
                // the "I forgot my password" button. In this case, we send them a forgot password token.
                getAccountService().sendForgotPasswordInfo(getDSContext(),email);

                cocoon.sendPage("forgot/verify", {"email":email});
                return;
            }
            
            email = cocoon.request.getParameter("email");
            email = email.toLowerCase(); // all emails should be lowercase
            var epersonFound = (getEPersonService().findByEmail(getDSContext(),email) != null);
            
            if (epersonFound) 
            {
                accountExists = true;
                continue;
            }
            
            var canRegister = AuthenticationUtil.canSelfRegister(getObjectModel(), email);
           
            if (canRegister) 
            {
                try 
                {
                    // May throw the AddressException or a variety of SMTP errors.
                    getAccountService().sendRegistrationInfo(getDSContext(),email);
                }
                catch (error) 
                {
                    // If any errors occurred while trying to send the email set the field in error.
                    errors = new Array("email");
                    continue;
                }
                
                cocoon.sendPage("register/verify", { "email":email, "forgot":"false" });
                return; 
            } 
            else 
            {
                cocoon.sendPage("register/cannot", { "email" : email});
                return;
            }
           
        } while (accountExists || errors.length > 0)
    } 
    else 
    {
        // We have a token. Find out who it's for
        var email = getAccountService().getEmail(getDSContext(), token);
        
        if (email == null) 
        {
            cocoon.sendPage("register/invalid-token");
            return;
        }
        
        var setPassword = AuthenticationUtil.allowSetPassword(getObjectModel(),email);
        
        var errors = new Array();
        do {
            cocoon.sendPageAndWait("register/profile",{"email" : email, "allowSetPassword":setPassword , "errors" : errors.join(',')});
            
            // If the user had to retry the form a user may already be created.
            var eperson = getEPersonService().findByEmail(getDSContext(),email);
            if (eperson == null)
            {
                eperson = AuthenticationUtil.createNewEperson(getObjectModel(),email);
            }
            
            // Log the user in so that they can update their own information.
            getDSContext().setCurrentUser(eperson);
            
            errors = updateInformation(eperson);
            
            if (setPassword) 
            {
                var passwordErrors = updatePassword(eperson);
                errors = errors.concat(passwordErrors);
            }
            
            // Log the user back out.
            getDSContext().setCurrentUser(null);
        } while (errors.length > 0) 
        
        // Log the newly created user in.
        AuthenticationUtil.logIn(getObjectModel(),eperson);
        getAccountService().deleteToken(getDSContext(), token);

        cocoon.sendPage("register/finished");
        return;
    }
}
  

/**
 * Perform a forgotten password processes.
 */
function doForgotPassword() 
{ 
    var token = cocoon.request.get("token");

    if (token == null) 
    {
        // We have no token, this is the initial form. First ask the user for their email address
        // and then send them a token.
        
        var email = cocoon.request.getParameter("email");
        
        var errors = new Array();
        do {
            cocoon.sendPageAndWait("forgot/start",{"email" : email, "errors" : errors.join(',')});
  
            email = cocoon.request.getParameter("email");
            errors = new Array();

            var epersonFound = (getEPersonService().findByEmail(getDSContext(),email) != null);

            if (!epersonFound)
            {
                // No eperson found for the given address, set the field in error and let 
                // the user try again.
                errors = new Array("email");
                continue;
            }

            // An Eperson was found for the given email, so use the forgot password 
            // mechanism. This may throw a AddressException if the email is ill-formed.
            getAccountService().sendForgotPasswordInfo(getDSContext(),email);
        } while (errors.length > 0)
        
        cocoon.sendPage("forgot/verify", {"email":email});
    } 
    else 
    {
        // We have a token. Find out who the it's for
        var email = getAccountService().getEmail(getDSContext(), token);

        if (email == null) 
        {
            cocoon.sendPage("forgot/invalid-token");
            return;
        }

        var epersonFound = (getAccountService().getEPerson(getDSContext(), token) != null);

        if (!epersonFound)
        {
            cocoon.sendPage("forgot/invalid-token");
            return;
        }

        var errors = new Array();

        do {
            cocoon.sendPageAndWait("forgot/reset", { "email" : email, "errors" : errors.join(',') });

            // Get the eperson associated with the password change
            var eperson = getAccountService().getEPerson(getDSContext(), token);

            // Temporarily log the user in so that they can update their password.
            getDSContext().setCurrentUser(eperson);

            errors = updatePassword(eperson);

            getDSContext().setCurrentUser(null);

        } while (errors.length > 0)

        // Log the user in and remove the token.
        AuthenticationUtil.logIn(getObjectModel(),eperson);
        getAccountService().deleteToken(getDSContext(), token);

        cocoon.sendPage("forgot/finished");
    }
}
  
/**
 * Flow function to update a user's profile. This flow will iterate 
 * over the profile/update form until the user has provided correct 
 * data (i.e. filled in the required fields and meet the minimum 
 * password requirements).
 */
function doUpdateProfile()
{
    var retry = false;
    
    // check that the user is logged in.
    if (getEPerson() == null)
    {
        var contextPath = cocoon.request.getContextPath();
        cocoon.redirectTo(contextPath + "/login",true);
        getDSContext().complete();
        cocoon.exit();
    }
    
    // Do we allow the user to change their password or does 
    // it not make sense for their authentication mechanism?
    var setPassword = AuthenticationUtil.allowSetPassword(getObjectModel(),getEPerson().getEmail());
    
    // List of errors encountered.
    var errors = new Array();
    do {
        cocoon.sendPageAndWait("profile/update", {"allowSetPassword" : setPassword, "errors" : errors.join(',') } );
        
        
        if (cocoon.request.get("submit"))
        {    
            // Update the user's info and password.
            errors = updateInformation(getEPerson());
            
            if (setPassword) 
            {
                // check if they entered a new password:
                var password = cocoon.request.getParameter("password");
                
                if (password != null && !password.equals(""))
                { 
                    var passwordErrors = updatePassword(getEPerson());
                    
                    errors = errors.concat(passwordErrors);
                } 
            }
        }
        else if (cocoon.request.get("submit_subscriptions_add"))
        {
            // Add the a new subscription
            var collection = getCollectionService().find(getDSContext(),UUID.fromString(cocoon.request.get("subscriptions")));
            if (collection != null)
            {
                getSubscribeService().subscribe(getDSContext(),getEPerson(),collection);
            }
        }
        else if (cocoon.request.get("submit_subscriptions_delete"))
        {
            // Remove any selected subscriptions
            var names = cocoon.request.getParameterValues("subscriptions_selected");
            if (names != null)
            {
	            for (var i = 0; i < names.length; i++)
	            {
	            	var collectionID = cocoon.request.get(names[i]);
	                var collection = getCollectionService().find(getDSContext(),UUID.fromString(collectionID));
	                if (collection != null)
                        getSubscribeService().unsubscribe(getDSContext(),getEPerson(),collection);
	            }
            }
        }
            
    } while (errors.length > 0 || !cocoon.request.get("submit")) 
  
    cocoon.sendPage("profile/updated");
}
  
  
/**
 * Update the eperson's profile information. Some fields, such as 
 * last_name & first_name are required.
 *
 * Missing or malformed field names will be returned in an array. 
 * If the user's profile information was updated successfully then 
 * an empty array will be returned. 
 */
function updateInformation(eperson) 
{
    if (!(ConfigurationManager.getBooleanProperty("xmlui.user.editmetadata", true)))
    {
        // We're configured to not allow the user to update their metadata so return with no errors.
        return new Array();
    }


	// Get the parameters from the form
	var lastName = cocoon.request.getParameter("last_name");
	var firstName = cocoon.request.getParameter("first_name");
	var phone = cocoon.request.getParameter("phone");
        var language = cocoon.request.getParameter("language");

    // first, check that each parameter is filled in before setting anything.
	var idx = 0;
	var errors = new Array();
	
	if (firstName == null || firstName.equals(""))
    {
        errors[idx++] = "first_name";
    }
    
    if (lastName == null || lastName.equals(""))
	{
	    errors[idx++] = "last_name";
	}
	
	if (idx > 0) 
	{
	    // There were errors
	    return errors;
	}
	
	eperson.setFirstName(getDSContext(), firstName);
	eperson.setLastName(getDSContext(), lastName);
	
    getEPersonService().setMetadata(getDSContext(), eperson, "phone", phone);
    eperson.setLanguage(getDSContext(), language);
    getEPersonService().update(getDSContext(), eperson);
	
    return new Array();
}
  
  
  
/**
 * Update the eperson's password if it meets the minimum password
 * requirements. 
 *
 * Any fields that are in error will be returned in an array. If
 * the user's password was updated successfully then an empty array
 * will be returned.
 */
function updatePassword(eperson) 
{
    var password = cocoon.request.getParameter("password");
    var passwordConfirm = cocoon.request.getParameter("password_confirm");
    
    // No empty passwords
    if (password == null)
    {
        return new Array("password");
    }
    
    // No short passwords
	if ( password.length() < 6) 
	{
		return new Array("password");
	}  
    
    // No unconfirmed passwords
	if (!password.equals(passwordConfirm)) 
	{
	    return new Array("password_confirm");
	} 
    
    getEPersonService().setPassword(eperson, password);
    getEPersonService().update(getDSContext(), eperson);
	
	return new Array();
}
