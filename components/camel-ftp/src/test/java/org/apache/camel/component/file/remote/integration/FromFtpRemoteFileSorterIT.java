/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.file.remote.integration;

import java.util.Comparator;

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.remote.RemoteFile;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test to verify remotefile sorter option.
 */
public class FromFtpRemoteFileSorterIT extends FtpServerTestSupport {

    @BindToRegistry("mySorter")
    private MyRemoteFileSorter sorter = new MyRemoteFileSorter();

    private String getFtpUrl() {
        return "ftp://admin@localhost:{{ftp.server.port}}/sorter?password=admin&sorter=#mySorter";
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        prepareFtpServer();
    }

    @Test
    public void testFtpSorter() throws Exception {
        MockEndpoint mock = getMockEndpoint("mock:result");
        mock.expectedMessageCount(3);
        mock.expectedBodiesReceived("Hello Copenhagen", "Hello London", "Hello Paris");
        mock.assertIsSatisfied();
    }

    private void prepareFtpServer() {
        // prepares the FTP Server by creating files on the server that we want
        // to unit
        // test that we can pool
        sendFile(getFtpUrl(), "Hello Paris", "paris.txt");
        sendFile(getFtpUrl(), "Hello London", "london.txt");
        sendFile(getFtpUrl(), "Hello Copenhagen", "copenhagen.txt");
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from(getFtpUrl()).to("mock:result");
            }
        };
    }

    // START SNIPPET: e1
    public class MyRemoteFileSorter implements Comparator<RemoteFile<?>> {

        @Override
        public int compare(RemoteFile<?> o1, RemoteFile<?> o2) {
            return o1.getFileNameOnly().compareToIgnoreCase(o2.getFileNameOnly());
        }
    }
    // END SNIPPET: e1
}
