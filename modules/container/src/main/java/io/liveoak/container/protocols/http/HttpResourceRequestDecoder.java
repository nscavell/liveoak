/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.protocols.http;

import io.liveoak.container.DefaultResourceParams;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ReturnFieldsImpl;
import io.liveoak.container.codec.MediaTypeMatcher;
import io.liveoak.container.codec.ResourceCodecManager;
import io.liveoak.container.codec.UnsupportedMediaTypeException;
import io.liveoak.security.impl.AuthConstants;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.Pagination;
import io.liveoak.spi.RequestType;
import io.liveoak.spi.ResourceParams;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.ReturnFields;
import io.liveoak.spi.Sorting;
import io.liveoak.spi.state.ResourceState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;

/**
 * @author Bob McWhirter
 */
public class HttpResourceRequestDecoder extends MessageToMessageDecoder<FullHttpRequest> {

    public HttpResourceRequestDecoder( ResourceCodecManager codecManager ) {
        this.codecManager = codecManager;
    }

    @Override
    public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception {
        if ( cause instanceof DecoderException ) {
            Throwable rootCause = cause.getCause();
            if ( rootCause instanceof UnsupportedMediaTypeException ) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse( HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_ACCEPTABLE );
                response.headers().add( HttpHeaders.Names.CONTENT_LENGTH, 0 );
                ctx.writeAndFlush( response );
                return;
            }
        }
        super.exceptionCaught( ctx, cause );
    }

    @Override
    protected void decode( ChannelHandlerContext ctx, FullHttpRequest msg, List<Object> out ) throws Exception {

        QueryStringDecoder decoder = new QueryStringDecoder( msg.getUri() );

        String path = decoder.path();

        int lastDotLoc = path.lastIndexOf( '.' );

        String extension = null;

        if ( lastDotLoc > 0 ) {
            extension = path.substring( lastDotLoc + 1 );
        }

        String acceptHeader = msg.headers().get( HttpHeaders.Names.ACCEPT );
        if ( acceptHeader == null ) {
            acceptHeader = "application/json";
        }
        MediaTypeMatcher mediaTypeMatcher = new MediaTypeMatcher( acceptHeader, extension );

        String authToken = getAuthorizationToken( msg );

        ResourceParams params = DefaultResourceParams.instance( decoder.parameters() );

        if ( msg.getMethod().equals( HttpMethod.POST ) ) {
            String contentTypeHeader = msg.headers().get( HttpHeaders.Names.CONTENT_TYPE );
            MediaType contentType = new MediaType( contentTypeHeader );
            out.add( new ResourceRequest.Builder( RequestType.CREATE, new ResourcePath( decoder.path() ) )
                    .resourceParams( params )
                    .mediaTypeMatcher( mediaTypeMatcher )
                    .requestAttribute( AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken )
                    .resourceState( decodeState( contentType, msg.content() ) )
                    .build() );
        } else if ( msg.getMethod().equals( HttpMethod.GET ) ) {
            out.add( new ResourceRequest.Builder( RequestType.READ, new ResourcePath( decoder.path() ) )
                    .resourceParams( params )
                    .mediaTypeMatcher( mediaTypeMatcher )
                    .requestAttribute( AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken )
                    .pagination( decodePagination( params ) )
                    .returnFields( decodeReturnFields( params ) )
                    .sorting( decodeSorting( params ) )
                    .build() );
        } else if ( msg.getMethod().equals( HttpMethod.PUT ) ) {
            String contentTypeHeader = msg.headers().get( HttpHeaders.Names.CONTENT_TYPE );
            MediaType contentType = new MediaType( contentTypeHeader );
            out.add( new ResourceRequest.Builder( RequestType.UPDATE, new ResourcePath( decoder.path() ) )
                    .resourceParams( params )
                    .mediaTypeMatcher( mediaTypeMatcher )
                    .requestAttribute( AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken )
                    .resourceState( decodeState( contentType, msg.content() ) )
                    .build() );
        } else if ( msg.getMethod().equals( HttpMethod.DELETE ) ) {
            out.add( new ResourceRequest.Builder( RequestType.DELETE, new ResourcePath( decoder.path() ) )
                    .resourceParams( params )
                    .mediaTypeMatcher( mediaTypeMatcher )
                    .requestAttribute( AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken )
                    .build() );
        }

    }

    private ReturnFields decodeReturnFields( ResourceParams params ) {
        String fieldsValue = params.value( "fields" );
        ReturnFieldsImpl returnFields = null;
        if ( fieldsValue != null && !"".equals( fieldsValue ) ) {
            returnFields = new ReturnFieldsImpl( fieldsValue );
        } else {
            returnFields = new ReturnFieldsImpl( "*" );
        }

        String expandValue = params.value( "expand" );

        if ( expandValue != null && !"".equals( expandValue ) ) {
            returnFields = returnFields.withExpand( expandValue );
        }

        return returnFields;
    }

    protected ResourceState decodeState( MediaType mediaType, ByteBuf content ) throws Exception {
        return codecManager.decode( mediaType, content );
    }

    protected Pagination decodePagination( ResourceParams params ) {
        int offset = limit( params.intValue( "offset", 0 ), 0, Integer.MAX_VALUE );
        int limit = limit( params.intValue( "limit", Pagination.DEFAULT_LIMIT ), 0, Pagination.MAX_LIMIT );

        return new Pagination() {
            public int offset() {
                return offset;
            }

            public int limit() {
                return limit;
            }
        };
    }

    protected Sorting decodeSorting( ResourceParams params ) {
        String spec = params.value( "sort" );
        if ( spec != null ) {
            return new Sorting( spec );
        }
        return null;
    }

    protected String getAuthorizationToken( FullHttpRequest req ) {
        String[] authorization = req.headers().contains( HttpHeaders.Names.AUTHORIZATION ) ? req.headers().get( HttpHeaders.Names.AUTHORIZATION ).split( " " ) : null;
        if ( authorization == null ) {
            return null;
        } else if ( authorization.length != 2 || !authorization[0].equalsIgnoreCase( "Bearer" ) ) {
            System.err.println( "Authorization header is invalid or it's of different type than 'Bearer'. Ignoring" );
            return null;
        } else {
            return authorization[1];
        }
    }

    private static int limit( int value, int lower, int upper ) {
        if ( value < lower ) {
            return lower;
        } else if ( value > upper ) {
            return upper;
        }
        return value;
    }

    private ResourceCodecManager codecManager;
}
