package ml.melun.mangaview.mangaview;


import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import ml.melun.mangaview.Preference;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CustomHttpClient {
    OkHttpClient client;
    Preference p;

    public CustomHttpClient(Preference p){
        this.p = p;
        this.client = getUnsafeOkHttpClient().followRedirects(false).followSslRedirects(false).build();
        //this.cfc = new HashMap<>();
        //this.client = new OkHttpClient.Builder().build();
    }

    public Response get(String url, Map<String, String> headers){
//        if(!isloaded){
//            cloudflareDns.init();
//            isloaded = true;
//        }
        Response response = null;
        try {
//            for(String key : cfc.keySet()){
//                cookie +=  key + '=' + cfc.mget(key)+ "; ";
//            }
            Request.Builder builder = new Request.Builder()
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
                    .url(url)
                    .get();

            for(String k : headers.keySet()){
                builder.addHeader(k, headers.get(k));
            }

            Request request = builder.build();
            response = this.client.newCall(request).execute();
//            if(response != null){
//                if(response.code()>=500){
//                    System.out.println("cf");
//                }
//            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

    public Response mget(String url, Boolean doLogin){
        return mget(url, doLogin, new HashMap<>());
    }
    public Response mget(String url){
        return mget(url,true);
    }

    public Response mget(String url, Boolean doLogin, Map<String, String> customCookie){
        if(doLogin && p.getSession().length()>0){
            customCookie.put("PHPSESSID", p.getSession());
        }
        String cookie = "";
        for(String key : customCookie.keySet()){
            cookie += key + '=' + customCookie.get(key) + "; ";
        }
        Map headers = new HashMap<String, String>();
        headers.put("Cookie", cookie);

        return get(p.getUrl()+url, headers);
    }

    public Response post(String url, RequestBody body, Map<String,String> headers){

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

        }
        return response;

    }


    public Response post(String url, RequestBody body){
//        if(!isloaded){
//            cloudflareDns.init();
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
