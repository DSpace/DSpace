/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
     * @param step The current step of the workflow (-1 if no step).
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
        {
            return "current";
        }
        else
        {
            return null;
        }
    }
}
