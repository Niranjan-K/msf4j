/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.msf4j.internal.router;

import com.google.common.io.Resources;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.wso2.carbon.transport.http.netty.config.YAMLTransportConfigurationBuilder;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.ws.rs.HttpMethod;

/**
 * Test the HttpsServer.
 */
public class HttpsServerTest extends HttpServerTest {

    private static SSLClientContext sslClientContext;

    private static final TestMicroservice TEST_MICROSERVICE = new TestMicroservice();
    private static MicroservicesRunner microservicesRunner;

    private static String hostname = Constants.HOSTNAME;
    private static final int port = Constants.PORT + 4;


    @BeforeClass
    public static void setup() throws Exception {
        baseURI = URI.create(String.format("https://%s:%d", hostname, port));
        System.setProperty(YAMLTransportConfigurationBuilder.NETTY_TRANSPORT_CONF,
                Resources.getResource("netty-transports-1.yml").getPath());
        microservicesRunner = new MicroservicesRunner();
        sslClientContext = new SSLClientContext();
        microservicesRunner
                .deploy(TEST_MICROSERVICE)
                .start();
    }

    @AfterClass
    public static void teardown() throws Exception {
        microservicesRunner.stop();
    }

    @Override
    protected HttpURLConnection request(String path, String method, boolean keepAlive) throws IOException {
        URL url = baseURI.resolve(path).toURL();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslClientContext.getClientContext().getSocketFactory());
        HostnameVerifier allHostsValid = (hostname1, session) -> true;

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        HttpURLConnection urlConn = (HttpsURLConnection) url.openConnection();
        if (method == HttpMethod.POST || method == HttpMethod.PUT) {
            urlConn.setDoOutput(true);
        }
        urlConn.setRequestMethod(method);
        if (!keepAlive) {
            urlConn.setRequestProperty(HEADER_KEY_CONNECTION, HEADER_VAL_CLOSE);
        }
        return urlConn;
    }

    @Override
    protected Socket createRawSocket(URL url) throws IOException {
        return sslClientContext.getClientContext().getSocketFactory().createSocket(url.getHost(), url.getPort());
    }

    public static void setSslClientContext(SSLClientContext sslClientContext) {
        HttpsServerTest.sslClientContext = sslClientContext;
    }
}
