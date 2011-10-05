package org.dspace.submit.bean;

import java.io.Serializable;

public class DataBean implements Serializable
{
	private static final long serialVersionUID = 1L;
	private String message;

	public DataBean()
	{
	}

	public String getMessage()
    {
			if (message == null)
			{
				return "";
            }

            return message;
    }

	public void setMessage(String fn)
	{
		message = fn;
	}
}