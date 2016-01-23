/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.security.net.config.cts;

import android.net.http.AndroidHttpClient;

import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.ArrayList;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import junit.framework.Assert;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

public final class TestUtils extends Assert {

    private TestUtils() {
    }


    public static void assertTlsConnectionSucceeds(String host, int port) throws Exception {
        assertSslSocketSucceeds(host, port);
        assertHttpClientHttpsSucceeds(host, port);
        assertUrlConnectionHttpsSucceeds(host, port);
    }

    public static void assertTlsConnectionFails(String host, int port) throws Exception {
        assertSslSocketFails(host, port);
        assertHttpClientHttpsFails(host, port);
        assertUrlConnectionHttpsFails(host, port);
    }

    public static X509TrustManager getDefaultTrustManager() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init((KeyStore)null);
        for (TrustManager tm : tmf.getTrustManagers()) {
            if (tm instanceof X509TrustManager) {
                return (X509TrustManager) tm;
            }
        }
        fail("Unable to find X509TrustManager");
        return null;
    }

    public static List<X509Certificate> loadCertificates(InputStream is) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        ArrayList<X509Certificate> result = new ArrayList<>();
        for (Certificate c : factory.generateCertificates(is)) {
            result.add((X509Certificate) c);
        }
        return result;
    }

    private static void assertSslSocketFails(String host, int port)
            throws Exception {
        try {
            Socket s = SSLContext.getDefault().getSocketFactory().createSocket(host, port);
            s.getInputStream();
            fail("Connection to " + host + ":" + port + " succeeded");
        } catch (SSLHandshakeException expected) {
        }
    }

    private static void assertSslSocketSucceeds(String host, int port)
            throws Exception {
        Socket s = SSLContext.getDefault().getSocketFactory().createSocket(host, port);
        s.getInputStream();
    }

    private static void assertUrlConnectionHttpsFails(String host, int port)
            throws Exception {
        URL url = new URL("https://" + host + ":" + port);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        try {
            connection.getInputStream();
            fail("Connection to " + host + ":" + port + " succeeded");
        } catch (SSLHandshakeException expected) {
        }
    }

    private static void assertUrlConnectionHttpsSucceeds(String host, int port)
            throws Exception {
        URL url = new URL("https://" + host + ":" + port);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.getInputStream();
    }

    private static void assertHttpClientHttpsSucceeds(String host, int port) throws Exception {
        URL url = new URL("https://" + host + ":" + port);
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(null);
        try {
            HttpResponse response = httpClient.execute(new HttpGet(url.toString()));
        } finally {
            httpClient.close();
        }
    }

    private static void assertHttpClientHttpsFails(String host, int port) throws Exception {
        URL url = new URL("https://" + host + ":" + port);
        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(null);
        try {
            HttpResponse response = httpClient.execute(new HttpGet(url.toString()));
            fail("Connection to " + host + ":" + port + " succeeded");
        } catch (IOException expected) {
        } finally {
            httpClient.close();
        }
    }
}
