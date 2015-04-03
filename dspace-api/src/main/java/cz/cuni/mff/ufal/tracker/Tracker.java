/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.tracker;

import javax.servlet.http.HttpServletRequest;

public interface Tracker
{
    public void trackDownload(HttpServletRequest request);
    public void trackPage(HttpServletRequest request, String pageName);
}
