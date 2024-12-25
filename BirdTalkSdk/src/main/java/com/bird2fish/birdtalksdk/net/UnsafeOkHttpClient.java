package com.bird2fish.birdtalksdk.net;

import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class UnsafeOkHttpClient {
    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // 创建一个信任所有证书的 TrustManager
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // 初始化一个 SSLContext，并使用我们创建的 TrustManager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // 创建一个 SSLSocketFactory，它将信任所有证书
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            //builder.readTimeout(10, TimeUnit.SECONDS);

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true; // 总是信任主机名
                }
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

