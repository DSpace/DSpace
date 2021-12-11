package org.dspace.app.canvasdimension.service;

import org.dspace.content.Bitstream;

public interface IIIFApiQueryService {

    public int[] getImageDimensions(Bitstream bitstream);

}
