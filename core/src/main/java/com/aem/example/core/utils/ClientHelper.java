package com.aem.example.core.utils;


import com.day.jcr.vault.packaging.PackageException;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.sling.api.SlingHttpServletResponse;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.client.*;
import javax.net.ssl.*;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;


/**
 * Util to connect to Web services through POOLED HTTP Connections
 *
 * @author Santhoshkumar
 */
public class ClientHelper {

    private static final int RED_TIMEOUT_VAL = 5000;
    private static final int CONNECT_TIMEOUT = 1000;
    private static final int MAX_CONNECTION = 100;


    private static Client unSecureClientObj;
    private static Client secureClientObj;

    public static ClientConfig configureClient() {
        ClientConfig config = new ClientConfig();
        try {
            config.property(ClientProperties.READ_TIMEOUT, RED_TIMEOUT_VAL);
            config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(MAX_CONNECTION);
            connectionManager.setDefaultMaxPerRoute(MAX_CONNECTION);
            config.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager);
            config.connectorProvider(new ApacheConnectorProvider());
        } catch (Exception e) {
            
        }
        return config;

    }

    private static Client createSecureClient() {
        TrustManager[] certs = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                        
                    }

                    public void checkClientTrusted(X509Certificate[] chain, String authType)
                            throws CertificateException {
                       
                    }
                }
        };
        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLSv1.2");
            ctx.init(null, certs, new SecureRandom());
        } catch (java.security.GeneralSecurityException ex) {
           
        }
        Client client = ClientBuilder.newBuilder().sslContext(ctx).hostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        }).withConfig(ClientHelper.configureClient()).build();

        client.register(JacksonFeature.class);
        return client;

    }

    private static Client createNonSecureClient() {
        Client client = ClientBuilder.newClient(ClientHelper.configureClient());
        client.register(JacksonFeature.class);
        return client;
    }

    public static Client getClient(boolean secureFlag) {
        return secureFlag ? getSecureClient() : getNonSecureClient();
    }


    private static Client getSecureClient() {
        if (null == secureClientObj) {
            secureClientObj = createSecureClient();
        }
        return secureClientObj;
    }


    private static Client getNonSecureClient() {
        if (null == unSecureClientObj) {
            unSecureClientObj = createNonSecureClient();
        }
        return unSecureClientObj;
    }

    /**
     * @param targetUrl
     * @param mediaType
     * @param responseType
     * @param secureFlag
     * @param <T>
     * @return
     */
    public static <T> T getData(String targetUrl, String mediaType, Class<T> responseType, boolean secureFlag) {
        if (null != targetUrl && ! (targetUrl == "") ) {
            Response clientResponse = null;
            try {
                clientResponse = getClient(secureFlag).target(targetUrl).request(mediaType).get();
            } catch (ProcessingException processingException) {
            }
            if (clientResponse != null) {
                if (clientResponse.getStatus() == SlingHttpServletResponse.SC_OK) {
                    return clientResponse.readEntity(responseType);
                } else {
                    // Write about your error
                }

            }
        }
        return null;
    }

}
