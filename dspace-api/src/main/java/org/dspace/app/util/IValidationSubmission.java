package org.dspace.app.util;

import java.util.List;

import org.dspace.core.Context;

public interface IValidationSubmission
{

    public List<ValidationMessage> check(Context context, SubmissionInfo subinfo);
    
}
