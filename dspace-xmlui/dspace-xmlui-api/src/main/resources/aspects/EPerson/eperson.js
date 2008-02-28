/*
 * eperson.js
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/06/02 21:37:32 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
 
importClass(Packages.javax.mail.internet.AddressException);

importClass(Packages.org.apache.cocoon.components.CocoonComponentManager);

importClass(Packages.org.dspace.core.Context);
importClass(Packages.org.dspace.content.Collection);
importClass(Packages.org.dspace.eperson.EPerson);
importClass(Packages.org.dspace.eperson.AccountManager);
importClass(Packages.org.dspace.eperson.Subscribe);

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
  return CocoonComponentManager.getCurrentEnvironment().getObjectModel();
}

function getDSContext()
{
	return ContextUtil.obtainContext(getObjectModel());
}

function getEPerson() 
{
    return getDSContext().getCurrentUser();
}



/**
 * Preform a new user registration. 
 */
function doRegister() 
{ 
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
                // The user attempted to register with an email address that all ready exists then they clicked
                // the "I forgot my password" button. In this case, we send them a forgot password token.
                AccountManager.sendForgotPasswordInfo(getDSContext(),email);

                cocoon.sendPage("forgot/verify", {"email":email});
                return;
            }
            
            email = cocoon.request.getParameter("email");
            email = email.toLowerCase(); // all emails should be lowercase
            var epersonFound = (EPerson.findByEmail(getDSContext(),email) != null);
            
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
                    // May throw the AddressException or a varity of SMTP errors.
                    AccountManager.sendRegistrationInfo(getDSContext(),email);
                } 
                catch (error) 
                {
                    // If any errors occure while trying to send the email set the field in error.
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
        // We have a token. Find out who the it's for
        var email = AccountManager.getEmail(getDSContext(), token);
        
        if (email == null) 
        {
            cocoon.sendPage("register/invalid-token");
            return;
        }
        
        var setPassword = AuthenticationUtil.allowSetPassword(getObjectModel(),email);
        
        var errors = new Array();
        do {
            cocoon.sendPageAndWait("register/profile",{"email" : email, "allowSetPassword":setPassword , "errors" : errors.join(',')});
            
            // If the user had to retry the form a user may allready be created.
            var eperson = EPerson.findByEmail(getDSContext(),email);
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
        AuthenticationUtil.loggedIn(getObjectModel(),eperson);
        AccountManager.deleteToken(getDSContext(), token);
        
        cocoon.sendPage("register/finished");
        return;
    }
}
  

/**
 * Preform a forgot password processes.
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

            var epersonFound = (EPerson.findByEmail(getDSContext(),email) != null);

            if (!epersonFound)
            {
                // No eperson found for the given address, set the field in error and let 
                // the user try again.
                errors = new Array("email");
                continue;
            }

            // An Eperson was found for the given email, so use the forgot password 
            // mechanism. This may throw a AddressException if the email is ill-formed.
            AccountManager.sendForgotPasswordInfo(getDSContext(),email);
        } while (errors.length > 0)
        
        cocoon.sendPage("forgot/verify", {"email":email});
    } 
    else 
    {
        // We have a token. Find out who the it's for
        var email = AccountManager.getEmail(getDSContext(), token);

        if (email == null) 
        {
            cocoon.sendPage("forgot/invalid-token");
            return;
        }

        var epersonFound = (AccountManager.getEPerson(getDSContext(), token) != null);

        if (!epersonFound)
        {
            cocoon.sendPage("forgot/invalid-token");
            return;
        }

        var errors = new Array();

        do {
            cocoon.sendPageAndWait("forgot/reset", { "email" : email, "errors" : errors.join(',') });

            // Get the eperson associated with the password change
            var eperson = AccountManager.getEPerson(getDSContext(), token);

            // Temporaraly log the user in so that they can update their password.
            getDSContext().setCurrentUser(eperson);

            errors = updatePassword(eperson);

            getDSContext().setCurrentUser(null);

        } while (errors.length > 0)

        // Log the user in and remove the token.
        AuthenticationUtil.loggedIn(getObjectModel(),eperson);
        AccountManager.deleteToken(getDSContext(), token);

        cocoon.sendPage("forgot/finished");
    }
}
  
/**
 * Flow function to update a user's profile. This flow will iterate 
 * over the profile/update form untill the user has provided correct 
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
        else if (cocoon.request.get("subscriptions_add"))
        {
            // Add the a new subscription
            var collection = Collection.find(getDSContext(),cocoon.request.get("subscriptions"));
            if (collection != null)
            {
                Subscribe.subscribe(getDSContext(),getEPerson(),collection);
                getDSContext().commit();
            }
        }
        else if (cocoon.request.get("subscriptions_delete"))
        {
            // Remove any selected subscriptions
            var names = cocoon.request.getParameterValues("subscriptions_selected");
            if (names != null)
            {
	            for (var i = 0; i < names.length; i++)
	            {
	            	var collectionID = cocoon.request.get(names[i]);
	                var collection = Collection.find(getDSContext(),collectionID);
	                if (collection != null)
	                    Subscribe.unsubscribe(getDSContext(),getEPerson(),collection);
	            }
            }
            getDSContext().commit();
        }
            
    } while (errors.length > 0 || !cocoon.request.get("submit")) 
  
    cocoon.sendPage("profile/updated");
}
  
  
/**
 * Update the eperson's profile information. Some fields, such as 
 * last_name & first_name are required.
 *
 * Missing or mailformed field names will be returned in an array. 
 * If the user's profile information was updated successfully then 
 * an empty array will be returned. 
 */
function updateInformation(eperson) 
{
	// Get the parameters from the form
	var lastName = cocoon.request.getParameter("last_name");
	var firstName = cocoon.request.getParameter("first_name");
	var phone = cocoon.request.getParameter("phone");

    // first check that each parameter is filled in before seting anything.	
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
	
	eperson.setFirstName(firstName);
	eperson.setLastName(lastName);
	
	eperson.setMetadata("phone", phone);
	eperson.update();
	
    return new Array();
}
  
  
  
/**
 * Update the eperson's password if it meets the minimum password
 * requirements. 
 *
 * Any fields that are in error will be returned in an array. If
 * the user's password was updated successfull then an empty array
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
    
	eperson.setPassword(password);
	eperson.update();
	
	return new Array();
}




