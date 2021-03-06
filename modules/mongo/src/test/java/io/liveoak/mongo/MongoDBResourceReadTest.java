/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */

package io.liveoak.mongo;

import com.mongodb.BasicDBObject;
import io.liveoak.container.ReturnFieldsImpl;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.ResourceNotFoundException;
import io.liveoak.spi.state.ResourceState;
import org.bson.types.ObjectId;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:mwringe@redhat.com">Matt Wringe</a>
 */
public class MongoDBResourceReadTest extends NewBaseMongoDBTest {

    @Test
    public void testGetSimple() throws Exception {
        String methodName = "testSimpleGet";
        assertFalse( db.collectionExists( methodName ) );

        // create the object using the mongo driver directly
        BasicDBObject object = new BasicDBObject();
        object.append( "foo", "bar" );
        db.getCollection( methodName ).insert( object );
        assertEquals( 1, db.getCollection( methodName ).getCount() );
        String id = object.getObjectId( "_id" ).toString();

        ResourceState result = connector.read( new RequestContext.Builder().build(), BASEPATH + "/" + methodName + "/" + id );

        //verify response
        assertThat( result ).isNotNull();
        assertThat( result.id() ).isEqualTo( id );
        assertThat( result.getProperty( "foo" ) ).isEqualTo( "bar" );
    }

    @Test
    public void testGetChild() throws Exception {
        String methodName = "testComplexGet";
        assertFalse( db.collectionExists( methodName ) );

        // create the object using the mongo driver directly
        BasicDBObject object = new BasicDBObject();
        object.append( "foo", "bar" ).append( "abc", "123" ).append( "obj", new BasicDBObject( "foo2", "bar2" ) );
        object.append( "child", new BasicDBObject( "grandchild", new BasicDBObject( "foo3", "bar3" ) ) );
        db.getCollection( methodName ).insert( object );
        assertEquals( 1, db.getCollection( methodName ).getCount() );
        String id = object.getObjectId( "_id" ).toString();

        ResourceState result = connector.read( new RequestContext.Builder().returnFields( new ReturnFieldsImpl( "*,grandchild(*)" ) ).build(), BASEPATH + "/" + methodName + "/" + id + "/child" );

        System.err.println( "result :: " + result );

        //verify response
        assertThat( result ).isNotNull();
        assertThat( result.id() ).isEqualTo( "child" );
        ResourceState grandChild = ( ResourceState ) result.getProperty( "grandchild" );
        assertThat( grandChild ).isNotNull();
        assertThat( grandChild.id() ).isEqualTo( "grandchild" );
        assertThat( grandChild.getProperty( "foo3" ) ).isEqualTo( "bar3" );

        result = connector.read( new RequestContext.Builder().build(), BASEPATH + "/" + methodName + "/" + id + "/child/grandchild" );
        assertThat( result ).isNotNull();
        assertThat( result.id() ).isEqualTo( "grandchild" );
        assertThat( result.getProperty( "foo3" ) ).isEqualTo( "bar3" );
    }

    @Test
    public void testGetInvalidId() throws Exception {
        String methodName = "testGetInvalidId";
        assertFalse( db.collectionExists( methodName ) );

        try {
            ResourceState result = connector.read( new RequestContext.Builder().build(), BASEPATH + "/" + methodName + "/foobar123" );
            fail( "shouldn't get here" );
        } catch ( ResourceNotFoundException rnfe ) {
            // expected
        }
    }

    @Test
    public void testGetNonExistantId() throws Exception {
        String methodName = "testGetNonExistantId";
        assertFalse( db.collectionExists( methodName ) );

        ObjectId id = new ObjectId();

        try {
            ResourceState result = connector.read( new RequestContext.Builder().build(), BASEPATH + "/" + methodName + "/" + id );
            fail( "shouldn't get here" );
        } catch ( ResourceNotFoundException rnfe ) {
            // expected
        }
    }


}
