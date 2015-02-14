/*
 * Copyright 2014 Andrew Gaul <andrew@gaul.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gaul.areweconsistentyet;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closer;
import com.google.inject.Module;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.domain.Location;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public final class AreWeConsistentYetTest {
    private static final int ITERATIONS = 1;
    private static final int OBJECT_SIZE = 1;
    // model eventual consistency with two stores that never reconcile
    private BlobStoreContext context;
    private BlobStoreContext contextRead;
    private AreWeConsistentYet awcyStrong;
    private AreWeConsistentYet awcyEventual;

    @Before
    public void setUp() throws Exception {
        ContextBuilder builder = ContextBuilder
                .newBuilder("transient")
                .credentials("identity", "credential")
                .modules(ImmutableList.<Module>of(new SLF4JLoggingModule()));
        context = builder.build(BlobStoreContext.class);
        contextRead = builder.build(BlobStoreContext.class);

        BlobStore blobStore = context.getBlobStore();
        BlobStore blobStoreRead = contextRead.getBlobStore();

        String containerName = "container-name";
        Location location = null;
        blobStore.createContainerInLocation(location, containerName);
        blobStoreRead.createContainerInLocation(location, containerName);

        awcyStrong = new AreWeConsistentYet(blobStore,
                blobStore, containerName, ITERATIONS, OBJECT_SIZE);
        awcyEventual = new AreWeConsistentYet(blobStore,
                blobStoreRead, containerName, ITERATIONS, OBJECT_SIZE);
    }

    @After
    public void tearDown() throws Exception {
        try (Closer closer = Closer.create()) {
            closer.register(context);
            closer.register(contextRead);
        }
    }

    @Test
    public void testReadAfterCreate() throws Exception {
        assertThat(awcyStrong.readAfterCreate()).isEqualTo(0);
        assertThat(awcyEventual.readAfterCreate()).isEqualTo(ITERATIONS);
    }

    // TODO: how to model this?
    @Ignore
    @Test
    public void testReadAfterDelete() throws Exception {
        assertThat(awcyStrong.readAfterDelete()).isEqualTo(0);
        assertThat(awcyEventual.readAfterDelete()).isEqualTo(ITERATIONS);
    }

    @Test
    public void testReadAfterOverwrite() throws Exception {
        assertThat(awcyStrong.readAfterOverwrite()).isEqualTo(0);
        assertThat(awcyEventual.readAfterOverwrite()).isEqualTo(ITERATIONS);
    }

    @Test
    public void testListAfterCreate() throws Exception {
        assertThat(awcyStrong.listAfterCreate()).isEqualTo(0);
        assertThat(awcyEventual.listAfterCreate()).isEqualTo(ITERATIONS);
    }

    // TODO: how to model this?
    @Ignore
    @Test
    public void testListAfterDelete() throws Exception {
        assertThat(awcyStrong.listAfterDelete()).isEqualTo(0);
        assertThat(awcyEventual.listAfterDelete()).isEqualTo(ITERATIONS);
    }
}
