package org.projectodd.restafari.container.codec.json;

import io.netty.buffer.ByteBuf;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.projectodd.restafari.container.SimpleObjectResource;
import org.projectodd.restafari.spi.ObjectResource;

import java.nio.charset.Charset;

/**
 * @author Bob McWhirter
 */
public class JSONCodecTest {

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSomething() throws Exception {

        JSONCodec codec = new JSONCodec();

        ObjectResource resource = new SimpleObjectResource();

        resource.setStringProperty("name", "bob" );

        ByteBuf result = codec.encode(resource);

        String str = result.toString(Charset.forName("utf-8"));

        assertEquals( "{\"name\":\"bob\"}", str );


    }

}
