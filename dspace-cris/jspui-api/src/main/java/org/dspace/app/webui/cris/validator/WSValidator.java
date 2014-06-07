/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.dspace.app.cris.model.ws.User;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.AddressUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class WSValidator implements Validator
{

    private InetAddressValidator validator = InetAddressValidator.getInstance();

    private Class clazz;

    private ApplicationService applicationService;

    public boolean supports(Class arg0)
    {
        return clazz.isAssignableFrom(arg0);
    }

    public void validate(Object arg0, Errors arg1)
    {
        User ws = (User) arg0;

        if (ws.getTypeDef().equals(User.TYPENORMAL))
        {
            ValidationUtils.rejectIfEmptyOrWhitespace(arg1,
                    "normalAuth.username", "error.form.ws.username.mandatory",
                    "Username is mandatory");
            ValidationUtils.rejectIfEmptyOrWhitespace(arg1,
                    "normalAuth.password", "error.form.ws.password.mandatory",
                    "Password is mandatory");
        }
        else
        {
            ValidationUtils.rejectIfEmptyOrWhitespace(arg1,
                    "specialAuth.token", "error.form.ws.token.mandatory",
                    "Token is mandatory");
            ValidationUtils.rejectIfEmptyOrWhitespace(arg1,
                    "specialAuth.fromIP", "error.form.ws.fromip.mandatory",
                    "Single IP is mandatory");

            Long froms = null;
            Long tos = null;
            if (ws.getFromIP() != null && !ws.getFromIP().isEmpty())
            {
                if (!validator.isValidInet4Address(ws.getFromIP()))
                {
                    arg1.reject("specialAuth.fromIP", "from IP not well formed");
                }
                else
                {
                    try
                    {
                        froms = AddressUtils.ipToLong(InetAddress.getByName(ws.getFromIP()));
                    }
                    catch (UnknownHostException e)
                    {
                        arg1.reject("specialAuth.fromIP",
                                "Unknown host exception");
                    }
                }

                if (ws.getToIP() != null && !ws.getToIP().isEmpty())
                {
                    if (!validator.isValidInet4Address(ws.getToIP()))
                    {
                        arg1.reject("specialAuth.ToIP", "to IP not well formed");
                    }
                    else
                    {
                        try
                        {
                            tos = AddressUtils.ipToLong(InetAddress.getByName(ws.getToIP()));
                                    
                        }
                        catch (UnknownHostException e)
                        {
                            arg1.reject("specialAuth.toIP",
                                    "Unknown host exception");
                        }
                    }
                }

                if (froms != null && tos != null)
                {
                    
                    if (froms >= tos)
                    {
                        arg1.reject("specialAuth.toIP", "Range not well formed");    
                    }                   
                    
                    
                }
            }

        }

    }

     
    public void setClazz(Class clazz)
    {
        this.clazz = clazz;
    }

    public void setApplicationService(ApplicationService applicationService)
    {
        this.applicationService = applicationService;
    }
}
