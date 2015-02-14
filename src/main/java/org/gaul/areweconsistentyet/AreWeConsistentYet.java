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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteSource;
import com.google.common.io.ByteStreams;
import com.google.inject.Module;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.options.ListContainerOptions;
import org.jclouds.domain.Location;
import org.jclouds.io.Payload;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public final class AreWeConsistentYet {
    private final ByteSource payload1;
    private final ByteSource payload2;
    private final BlobStore blobStore;
    private final BlobStore blobStoreRead;
    private final String containerName;
    private final int iterations;
    private final Random random = new Random();

    public AreWeConsistentYet(BlobStore blobStore,
            BlobStore blobStoreRead, String containerName, int iterations,
            long objectSize) {
        this.blobStore = Preconditions.checkNotNull(blobStore);
        this.blobStoreRead = Preconditions.checkNotNull(blobStoreRead);
        this.containerName = Preconditions.checkNotNull(containerName);
        Preconditions.checkArgument(iterations > 0,
                "iterations must be greater than zero, was: " + iterations);
        this.iterations = iterations;
        Preconditions.checkArgument(objectSize >= 0,
                "object size must be at least zero, was: " + objectSize);
        payload1 = Utils.infiniteByteSource((byte) 1).slice(0, objectSize);
        payload2 = Utils.infiniteByteSource((byte) 2).slice(0, objectSize);
    }

    public int readAfterCreate() throws IOException {
        int count = 0;
        for (int i = 0; i < iterations; ++i) {
            String blobName = makeBlobName();
            blobStore.putBlob(containerName, makeBlob(blobName, payload1));
            Blob getBlob = blobStoreRead.getBlob(containerName, blobName);
            if (getBlob == null) {
                ++count;
            } else {
                try (Payload payload = getBlob.getPayload();
                     InputStream is = payload.openStream()) {
                    ByteStreams.copy(is, ByteStreams.nullOutputStream());
                }
            }
            blobStore.removeBlob(containerName, blobName);
        }
        return count;
    }

    public int readAfterDelete() throws IOException {
        int count = 0;
        for (int i = 0; i < iterations; ++i) {
            String blobName = makeBlobName();
            blobStore.putBlob(containerName, makeBlob(blobName, payload1));
            blobStore.removeBlob(containerName, blobName);
            Blob getBlob = blobStoreRead.getBlob(containerName, blobName);
            if (getBlob != null) {
                ++count;
                try (Payload payload = getBlob.getPayload();
                     InputStream is = payload.openStream()) {
                    ByteStreams.copy(is, ByteStreams.nullOutputStream());
                }
            }
        }
        return count;
    }

    public int readAfterOverwrite() throws IOException {
        int count = 0;
        for (int i = 0; i < iterations; ++i) {
            String blobName = makeBlobName();
            blobStore.putBlob(containerName, makeBlob(blobName, payload1));
            blobStore.putBlob(containerName, makeBlob(blobName, payload2));
            Blob getBlob = blobStoreRead.getBlob(containerName, blobName);
            try (Payload payload = getBlob.getPayload();
                 InputStream is = payload.openStream()) {
                if (Arrays.equals(payload1.read(), ByteStreams.toByteArray(
                        is))) {
                    ++count;
                }
            }
            blobStore.removeBlob(containerName, blobName);
        }
        return count;
    }

    public int listAfterCreate() throws IOException {
        int count = 0;
        for (int i = 0; i < iterations; ++i) {
            String blobName = makeBlobName();
            blobStore.putBlob(containerName, makeBlob(blobName, payload1));
            if (!listAllBlobs().contains(blobName)) {
                ++count;
            }
            blobStore.removeBlob(containerName, blobName);
        }
        return count;
    }

    public int listAfterDelete() throws IOException {
        int count = 0;
        for (int i = 0; i < iterations; ++i) {
            String blobName = makeBlobName();
            blobStore.putBlob(containerName, makeBlob(blobName, payload1));
            blobStore.removeBlob(containerName, blobName);
            if (listAllBlobs().contains(blobName)) {
                ++count;
            }
        }
        return count;
    }

    private String makeBlobName() {
        return "blob-name-" + random.nextInt();
    }

    private Blob makeBlob(String blobName, ByteSource payload)
            throws IOException {
        return blobStore.blobBuilder(blobName)
                .payload(payload)
                .contentLength(payload.size())
                .build();
    }

    private Set<String> listAllBlobs() {
        Set<String> blobNames = new HashSet<String>();
        ListContainerOptions options = new ListContainerOptions();
        while (true) {
            PageSet<? extends StorageMetadata> set = blobStoreRead.list(
                    containerName, options);
            for (StorageMetadata sm : set) {
                blobNames.add(sm.getName());
            }
            String marker = set.getNextMarker();
            if (marker == null) {
                break;
            }
            options = options.afterMarker(marker);
        }
        return blobNames;
    }

    static final class AreWeConsistentYetOptions {
        @Option(name = "--container-name",
                usage = "container name for tests, will be created and removed",
                required = true)
        private String containerName = "container-name";

        @Option(name = "--iterations", usage = "number of iterations")
        private int iterations = 1;

        @Option(name = "--location", usage = "container location")
        private String location;

        @Option(name = "--size", usage = "object size in bytes (default: 1)")
        private long objectSize = 1;

        @Option(name = "--properties", usage = "configuration file",
                required = true)
        private File propertiesFile;

        @Option(name = "--reader-endpoint",
                usage = "separate endpoint to read from")
        private String readerEndpoint;
    }

    public static void main(String[] args) throws Exception {
        AreWeConsistentYetOptions options = new AreWeConsistentYetOptions();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException cle) {
            PrintStream err = System.err;
            err.println("are-we-consistent-yet version " +
                    AreWeConsistentYet.class.getPackage()
                            .getImplementationVersion());
            err.println("Usage: are-we-consistent-yet" +
                    " --container-name NAME --properties FILE [options...]");
            parser.printUsage(err);
            System.exit(1);
        }

        Properties properties = new Properties();
        try (InputStream is = new FileInputStream(options.propertiesFile)) {
            properties.load(is);
        }
        Properties propertiesRead = (Properties) properties.clone();
        if (options.readerEndpoint != null) {
            propertiesRead.setProperty(Constants.PROPERTY_ENDPOINT,
                    options.readerEndpoint);
        }

        try (BlobStoreContext context = blobStoreContextFromProperties(
                     properties);
             BlobStoreContext contextRead = blobStoreContextFromProperties(
                     propertiesRead)) {
            BlobStore blobStore = context.getBlobStore();
            BlobStore blobStoreRead = contextRead.getBlobStore();

            Location location = null;
            if (options.location != null) {
                for (Location loc : blobStore.listAssignableLocations()) {
                    if (loc.getId().equalsIgnoreCase(options.location)) {
                        location = loc;
                        break;
                    }
                }
                if (location == null) {
                    throw new Exception("Could not find location: " +
                            options.location);
                }
            }
            blobStore.createContainerInLocation(location,
                    options.containerName);
            AreWeConsistentYet test = new AreWeConsistentYet(
                    blobStore, blobStoreRead, options.containerName,
                    options.iterations, options.objectSize);
            PrintStream out = System.out;
            out.println("eventual consistency count with " +
                    options.iterations + " iterations: ");
            out.println("read after create: " + test.readAfterCreate());
            out.println("read after delete: " + test.readAfterDelete());
            out.println("read after overwrite: " + test.readAfterOverwrite());
            out.println("list after create: " + test.listAfterCreate());
            out.println("list after delete: " + test.listAfterDelete());
            blobStore.deleteContainer(options.containerName);
        }
    }

    private static BlobStoreContext blobStoreContextFromProperties(
            Properties properties) {
        String provider = properties.getProperty(Constants.PROPERTY_PROVIDER);
        String identity = properties.getProperty(Constants.PROPERTY_IDENTITY);
        String credential = properties.getProperty(
                Constants.PROPERTY_CREDENTIAL);
        String endpoint = properties.getProperty(Constants.PROPERTY_ENDPOINT);
        if (provider == null || identity == null || credential == null) {
            System.err.println("Properties file must contain:\n" +
                    Constants.PROPERTY_PROVIDER + "\n" +
                    Constants.PROPERTY_IDENTITY + "\n" +
                    Constants.PROPERTY_CREDENTIAL);
            System.exit(1);
        }

        ContextBuilder builder = ContextBuilder
                .newBuilder(provider)
                .credentials(identity, credential)
                .modules(ImmutableList.<Module>of(new SLF4JLoggingModule()))
                .overrides(properties);
        if (endpoint != null) {
            builder = builder.endpoint(endpoint);
        }
        return builder.build(BlobStoreContext.class);
    }
}
