package org.projectodd.restafari.container.codec;

import org.projectodd.restafari.spi.MediaType;

import java.util.List;

/**
 * @author Bob McWhirter
 */
public class UnsupportedMediaTypeException extends Exception {

    public UnsupportedMediaTypeException(List<MediaType> mediaTypes) {
        super( "Unsupported media-types: " + mediaTypes );
        this.mediaTypes = mediaTypes;
    }

    public List<MediaType> mediaTypes() {
        return this.mediaTypes;
    }

    private List<MediaType> mediaTypes;
}
