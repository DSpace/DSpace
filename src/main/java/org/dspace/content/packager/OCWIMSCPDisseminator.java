/*
 * OCWIMSCPDisseminator
 *
 * Version: $Revision$
 *
 * Date: $Date$
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

package org.dspace.content.packager;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageException;
import org.dspace.core.Context;

import org.jdom.Document;

/**
 * Disseminator for MIT OpenCourseware (OCW)/CWSpace profile of the IMS
 * Content Package spec, or IMSCP.
 * <p>
 * <p>
 * For more information about IMSCP, see IMS Global Learning Consortium,
 * http://www.imsglobal.org/content/packaging/
 *
 * @author Larry Stone
 * @version $Revision$
 * @see org.dspace.content.packager.AbstractIMSCPDisseminator
 */
public class OCWIMSCPDisseminator
    extends AbstractIMSCPDisseminator
{
    /** log4j category */
    private static Logger log = Logger.getLogger(org.dspace.content.packager.OCWIMSCPDisseminator.class);

    /**
     * Throw an error since we cannot construct a manifest for any
     * random Item.
     */
    public Document makeManifest(Context context, Item item, PackageParameters params)
        throws PackageException
    {
        throw new PackageException("No manifest found: The OCW IMSCP disseminator cannot create a package out of an Item that was not imported from an OCW IMSCP package.");
    }

    public Class getManifestClass()
    {
        return OCWIMSCPManifest.class;
    }

}
