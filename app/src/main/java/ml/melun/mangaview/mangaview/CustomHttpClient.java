package ml.melun.mangaview.mangaview;


import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;

public class CustomHttpClient {
    public OkHttpClient client;
    Map<String, String> cookies;

    public CustomHttpClient(){
        System.out.println("http client create");
        this.cookies = new HashMap<>();
        if(android.os.Build.VERSION.SDK_INT < CODE_SCOPED_STORAGE) {
            // Necessary because our servers don't have the right cipher suites.
            // https://github.com/square/okhttp/issues/4053
            List<CipherSuite> cipherSuites = new ArrayList<>();
            cipherSuites.addAll(ConnectionSpec.MODERN_TLS.cipherSuites());
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA);
            cipherSuites.add(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA);

            ConnectionSpec legacyTls = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .cipherSuites(cipherSuites.toArray(new CipherSuite[0]))
                    .build();

            this.client = getUnsafeOkHttpClient()
                    .connectionSpecs(Arrays.asList(legacyTls, ConnectionSpec.CLEARTEXT))
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
        }else {
            this.client = getUnsafeOkHttpClient()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();
        }

        //this.cfc = new HashMap<>();
        //this.client = new OkHttpClient.Builder().build();
    }

    public void setCookie(String k, String v){
        cookies.put(k, v);
    }
    public void resetCookie(){
        this.cookies = new HashMap<>();
    }



    public String getCookie(String k){
        return cookies.get(k);
    }

    public Response get(String url, Map<String, String> headers){
//        System.out.println(url);
        Response response = null;
        try {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .get();
            if(headers !=null)
                for(String k : headers.keySet()){
                    builder.addHeader(k, headers.get(k));
                }

            Request request = builder.build();
            response = this.client.newCall(request).execute();
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
        return response;
    }

    public Response mget(String url, Boolean doLogin){
        return mget(url, doLogin, new HashMap<>());
    }
    public Response mget(String url){
        return mget(url,true);
    }


    public String getUrl(){
        return p.getUrl();
    }


    public Response mget(String url, Boolean doLogin, Map<String, String> customCookie){
        if(customCookie==null)
            customCookie = new HashMap<>();
        if(doLogin && p.getLogin() != null && p.getLogin().cookie != null && p.getLogin().cookie.length()>0){
            customCookie.put("PHPSESSID", p.getLogin().cookie);
        }
        Map<String,String> cookie = new HashMap<>();
        cookie.putAll(this.cookies);
        if(customCookie != null) cookie.putAll(customCookie);

        StringBuilder cbuilder = new StringBuilder();
        for(String key : cookie.keySet()){
            cbuilder.append(key);
            cbuilder.append('=');
            cbuilder.append(cookie.get(key));
            cbuilder.append("; ");
        }
        if(cbuilder.length()>2)
            cbuilder.delete(cbuilder.length()-2,cbuilder.length());

        Map headers = new HashMap<String, String>();
        headers.put("Cookie", cbuilder.toString());
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
        headers.put("Referer",p.getUrl());

        return get(p.getUrl()+url, headers);
    }

    public Response post(String url, RequestBody body, Map<String,String> headers){
        return post(url,body,headers,false);
    }

    public Response post(String url, RequestBody body, Map<String,String> headers, boolean localCookies){

        String cs = "";
        //get cookies from headers
        if(headers.get("Cookie") != null)
            cs += headers.get("Cookie");

        // add local cookies
        if(localCookies)
            for(String key : this.cookies.keySet()){
                cs += key + '=' + this.cookies.get(key) + "; ";
            }

        headers.put("Cookie", cs);


        Response response = null;
        try {
            Request.Builder builder = new Request.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                    .url(url)
                    .post(body);

            for(String key: headers.keySet()){
                builder.addHeader(key, headers.get(key));
            }

            Request request = builder.build();
            response = this.client.newCall(request).execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;

    }


    public Response post(String url, RequestBody body){
//        if(!isloaded){
//            cloudflareDns.create();
//            isloaded = true;
//        }
        return post(url, body, new HashMap<>());
    }

    /*
    code source : https://gist.github.com/chalup/8706740
     */

    private static OkHttpClient.Builder getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
                                                       String authType) throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
