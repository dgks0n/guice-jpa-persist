package org.apache.onami.persist.test;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import org.apache.onami.persist.PersistenceFilter;
import org.apache.onami.persist.PersistenceModule;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Test which ensures that the @{link PersistenceFilter} fulfills the requirements of a guice servlet filter.
 */
public class PersistenceServletFilterTest
{
    private Injector injector;

    @Before
    public void setUp()
    {
        final PersistenceModule pm = createPersistenceModuleForTest();
        injector = Guice.createInjector( pm );
    }

    private PersistenceModule createPersistenceModuleForTest()
    {
        return new PersistenceModule()
        {

            @Override
            protected void configurePersistence()
            {
                bindApplicationManagedPersistenceUnit( "firstUnit" );
            }
        };
    }

    @Test
    public void persistenceFilterShouldBeSingleton()
    {
        assertThat( isSingleton( PersistenceFilter.class ), is( true ) );
    }

    private boolean isSingleton( Class<?> type )
    {
        return Scopes.isSingleton( injector.getBinding( type ) );
    }
}
