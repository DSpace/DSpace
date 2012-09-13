package org.dspace.app.webui.search;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.core.Context;

public interface SearchRequestProcessor
{
    public void doSimpleSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException,
            IOException, ServletException;

    public void doAdvancedSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException,
            IOException, ServletException;

    public void doOpenSearch(Context context, HttpServletRequest request,
            HttpServletResponse response) throws SearchProcessorException,
            IOException, ServletException;

}
