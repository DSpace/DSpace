/*
 * EPersonUtils.java
 *
 * Version: $Revision: 1.3 $
 *
 * Date: $Date: 2006/04/25 21:35:09 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

package org.dspace.app.xmlui.aspect.eperson;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;


/**
 * A set of static utilities to help with EPerson workflows.
 * 
 * @author Scott Phillips
 */

public class EPersonUtils
{

    /**
     * Create a progress list for the registration workflow.
     * 
     * @param form The division of the current workflow step.
     * @param step The current step of the workflow (-1 if no step)
     * 
     */
    public static void registrationProgressList(Division form, int step) throws WingException
    {
        List progress = form.addList("registration-progress",
                List.TYPE_PROGRESS);

        new Message("default","xmlui.EPerson.EPersonUtils.register_verify_email");
        
        progress.addItem("register-verify-email", render(step, 1)).addContent(
                new Message("default","xmlui.EPerson.EPersonUtils.register_verify_email"));

        progress.addItem("register-create-profile", render(step, 2)).addContent(
                new Message("default","xmlui.EPerson.EPersonUtils.register_create_profile"));

        progress.addItem("register-finished", render(step, 3)).addContent(
                new Message("default","xmlui.EPerson.EPersonUtils.register_finished"));
    }

    /**
     * Create a progress list for the forgot password workflow.
     * 
     * @param form The division of the current workflow step
     * @param step The current step of the workflow (-1 if no step)
     */
    public static void forgottProgressList(Division form, int step) throws WingException
    {

        List progress = form.addList("forgot-password-progress",
                List.TYPE_PROGRESS);

        progress.addItem("forgot-verify-email", render(step, 1)).addContent(
                new Message("default","xmlui.EPerson.EPersonUtils.forgot_verify_email"));

        progress.addItem("forgot-reset-passowrd", render(step, 2)).addContent(
                new Message("default","xmlui.EPerson.EPersonUtils.forgot_reset_password"));

        progress.addItem("forgot-finished", render(step, 3)).addContent(
                new Message("default","xmlui.EPerson.EPersonUtils.forgot_finished"));
    }

    private static String render(int givenStep, int step)
    {
        if (givenStep == step)
            return "current";
        else
            return null;
    }
}
