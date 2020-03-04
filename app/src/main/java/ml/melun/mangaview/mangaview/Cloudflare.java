package ml.melun.mangaview.mangaview;

/*
original code: https://github.com/zhkrb/cloudflare-scrape-Android

MIT License

Copyright (c) 2019 zhkrb

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8RuntimeException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ml.melun.mangaview.R;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

import static ml.melun.mangaview.MainApplication.httpClient;

//TODO SSLSocketFactory setEnabledCipherSuites
//ECDHE-ECDSA-AES128-GCM-SHA256

public class Cloudflare {

    private String mUrl;
    private int mRetry_count;
    private URL ConnUrl;
    private Map<String, String> cookies = new HashMap<>();

    private static final int MAX_COUNT = 3;
    private static final int CONN_TIMEOUT = 60000;

    private final Map<String, String> originalHeader = new HashMap<String, String>() {
        {
            put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
            put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
        }
    };

    private boolean canVisit = false;
    private boolean hasNewUrl = false;  //when cf return 301 you need to change old url to new url;

    public Cloudflare(String url) {
        mUrl = url;
    }

    public Map<String,String> getCookies(){
        while (!canVisit){
            if (mRetry_count>MAX_COUNT){
                break;
            }
            try {
                int responseCode = checkUrl();
                if (responseCode==200){
                    canVisit=true;
                    break;
                }else {
                    getVisiteCookie();
                }
            } catch (IOException | RuntimeException | InterruptedException e) {
                if (cookies!=null){
                    cookies.clear();
                }
                e.printStackTrace();
            } finally {
                closeAllConn();
            }
            mRetry_count++;
        }
        if(canVisit)
            return cookies;
        else
            return null;

    }


    private void getVisiteCookie() throws IOException, InterruptedException {
        Map<String, String> headers = new HashMap<String, String>();
        headers.putAll(originalHeader);
        ConnUrl = new URL(mUrl);
        //mGetMainConn.setRequestMethod("GET");

        headers.put("referer", mUrl);
        setCookies(headers);
        for(Map.Entry<String, String> item : headers.entrySet())
            System.out.println("pppp" + item.getKey() +" " + item.getValue());
        Response r = httpClient.get(mUrl, headers);

        switch (r.code()){
            case HttpURLConnection.HTTP_OK:
                e("MainUrl","visit website success");
                updateCookies(r);
                checkCookie(cookies);
                return;
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                mUrl = r.header("Location");
                updateCookies(r);
                checkCookie(cookies);
                e("MainUrl","HTTP 301 :"+mUrl);
                return;
            case HttpURLConnection.HTTP_FORBIDDEN:
                e("MainUrl","IP block or cookie err");
                return;
            case HttpURLConnection.HTTP_UNAVAILABLE:
                updateCookies(r);
                String str = r.body().string();
                System.out.println("PPPPPP" +str);
                getCheckAnswer(str);
                break;
            default:
                e("MainUrl","UnCatch Http code: "+r.header("Location"));
                break;
        }
    }

    /**
     * 获取值并跳转获得cookies
     * @param str
     */
    private void getCheckAnswer(String str) throws InterruptedException, IOException,RuntimeException {
        AnswerBean bean = new AnswerBean();
        if (str.contains("POST")){
            bean.setMethod(AnswerBean.POST);

            ArrayList<String> param = (ArrayList<String>) regex(str,"<form id=\"challenge-form\" action=\"(.+?)\"");
            if (param == null || param.size() == 0){
                e("getPost param error");
                throw new RuntimeException("getPost param error");
            }
            bean.setHost("https://"+ConnUrl.getHost()+param.get(0));
            ArrayList<String> s = (ArrayList<String>) regex(str,"<input type=\"hidden\" name=\"(.+?)\" value=\"(.+?)\">");
            if (s != null && s.size() > 0){
                bean.getFromData().put(s.get(0),s.get(1).contains("input type=\"hidden\"") ? "" : s.get(1));
            }
            String jschl_vc = regex(str,"name=\"jschl_vc\" value=\"(.+?)\"").get(0);
            String pass = regex(str,"name=\"pass\" value=\"(.+?)\"").get(0);
            bean.getFromData().put("jschl_vc",jschl_vc);
            bean.getFromData().put("pass",pass);
            double jschl_answer = get_answer(str);
            e(String.valueOf(jschl_answer));
            Thread.sleep(3000);
            bean.getFromData().put("jschl_answer",String.valueOf(jschl_answer));
        }else {
            bean.setMethod(AnswerBean.GET);

            String s = regex(str,"name=\"s\" value=\"(.+?)\"").get(0);   //正则取值
            String jschl_vc = regex(str,"name=\"jschl_vc\" value=\"(.+?)\"").get(0);
            String pass = regex(str,"name=\"pass\" value=\"(.+?)\"").get(0);            //
            double jschl_answer = get_answer(str);
            e(String.valueOf(jschl_answer));
            Thread.sleep(3000);
            String req = "https://" + ConnUrl.getHost() +"/cdn-cgi/l/chk_jschl?";
            if (!TextUtils.isEmpty(s)){
                s = Uri.encode(s);
                req+="s="+s+"&";
            }
            req+="jschl_vc="+Uri.encode(jschl_vc)+"&pass="+Uri.encode(pass)+"&jschl_answer="+jschl_answer;
            bean.setHost(req);
        }
        e("RedirectUrl",bean.getHost());
        getRedirectResponse(bean);
    }

    private void getRedirectResponse(AnswerBean answerBean) throws IOException {
        //header
        Map<String, String> headers = new HashMap<String, String>();
        headers.putAll(originalHeader);
        headers.put("referer", mUrl);
        setCookies(headers);

        Response r;
        if(answerBean.getMethod() == AnswerBean.GET){
            r = httpClient.get(answerBean.getHost(), headers);
        }else{
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            FormBody.Builder body = new FormBody.Builder();
            for(Map.Entry<String, String> entry :answerBean.getFromData().entrySet()){
                body.add(entry.getKey(), entry.getValue());
            }
            r = httpClient.post(answerBean.getHost(), body.build(), headers);
        }

        switch (r.code()){
            case HttpURLConnection.HTTP_OK:
                updateCookies(r);
                break;
            case HttpURLConnection.HTTP_MOVED_TEMP:
                updateCookies(r);
                checkCookie(cookies);
                break;
            default:throw new IOException("getOtherResponse Code: "+ r.code());
        }
    }

    private void checkCookie(Map<String, String> cookieList) {
        if (cookieList == null || cookieList.size() <= 1){
            return;
        }
        Map<String, String> a = new HashMap<>();
        Map.Entry<String,String> newestCookie = null;
        for (Map.Entry<String, String> item : cookieList.entrySet()){
            if (!item.getKey().equals("_cfduid")){
                continue;
            }
            if (newestCookie == null){
                newestCookie = item;
                continue;
            }
            a.put(newestCookie.getKey(), newestCookie.getValue());
            newestCookie = item;
        }
        if (a.size()>0){
            for(String k: a.keySet()){
                cookieList.remove(k);
            }
        }
    }

    private void setCookies(Map<String, String> header){
        if (cookies!=null&&cookies.size()>0) {
            StringBuilder csb = new StringBuilder();
            for(String k : cookies.keySet()){
                csb.append(k);
                csb.append('=');
                csb.append(cookies.get(k));
                csb.append("; ");
            }
            csb.delete(csb.length()-2, csb.length());
            header.put("cookie",csb.toString());
        }
    }

    private int checkUrl()throws IOException {
        URL ConnUrl = new URL(mUrl);
        Map<String, String> headers = new HashMap<String, String>();
        headers.putAll(originalHeader);
        headers.put("referer", mUrl);
        setCookies(headers);
        return httpClient.get(mUrl, headers).code();
    }

    private void closeAllConn(){
//        if (mCheckConn!=null){
//            mCheckConn.disconnect();
//        }
//        if (mGetMainConn!=null){
//            mGetMainConn.disconnect();
//        }
//        if (mGetRedirectionConn!=null){
//            mGetRedirectionConn.disconnect();
//        }
    }

    private void updateCookies(Response r){
        List<String>cstrs = r.headers("Set-Cookie");
        for(String c : cstrs){
            cookies.put(c.split("=")[0], c.substring(c.indexOf("=")+1,c.indexOf(";")));
        }
    }

    private double get_answer(String str) {  //取值
        double a = 0;

        try {
            List<String> s = regex(str,"var s,t,o,p,b,r,e,a,k,i,n,g,f, " +
                    "(.+?)=\\{\"(.+?)\"");
            String varA = s.get(0);
            String varB = s.get(1);
            String div_cfdn = getCfdnDOM(str);
            List<String> eval_fuc = null;
            if (!TextUtils.isEmpty(div_cfdn)){
                eval_fuc = checkEval(str);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("var t=\"").append(new URL(mUrl).getHost()).append("\";");
            sb.append("var a=");
            sb.append(regex(str,varA+"=\\{\""+varB+"\":(.+?)\\}").get(0));
            sb.append(";");
            List<String> b = regex(str,varA+"\\."+varB+"(.+?)\\;");
            if (b != null) {
                for (int i =0;i<b.size()-1;i++){
                    sb.append("a");
                    if (eval_fuc!=null&&eval_fuc.size()>0){
                        sb.append(replaceEval(b.get(i),div_cfdn,eval_fuc));
                    }else {
                        sb.append(b.get(i));
                    }
                    sb.append(";");
                }
            }

            e("add",sb.toString());
            V8 v8 = V8.createV8Runtime();
            a = v8.executeDoubleScript(sb.toString());
            List<String> fixNum = regex(str,"toFixed\\((.+?)\\)");
            if (fixNum!=null){
                e("toFix",fixNum.get(0));
                a = Double.parseDouble(v8.executeStringScript("String("+ a +".toFixed("+fixNum.get(0)+"));"));
            }
            if (b !=null && b.get(b.size()-1).contains("t.length")){
                a += new URL(mUrl).getHost().length();
            }
            v8.release();
        }catch (IndexOutOfBoundsException e){
            e("answerErr","get answer error");
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (V8RuntimeException e){
            e("scriptRuntimeErr","script runtime error,check the js code");
            e.printStackTrace();
        }
        return a;
    }

    private String replaceEval(String s, String div_cfdn, List<String> eval_fuc) {
        List<String> eval = regex(s,"eval\\(eval\\((.+?)");
        if (eval==null||eval.size()==0){
            return s;
        }
        s = s.replace(eval_fuc.get(0),div_cfdn);
        s+=";"+eval_fuc.get(1);
        return s;
    }

    private List<String> checkEval(String str) {
        List<String> evalDom = regex(str,"function\\(p\\)\\{var p = (.+?)\\;(.+?)\\;");
        if (evalDom==null||evalDom.size()==0){
            return null;
        }else {
            return evalDom;
        }
    }

    private String getCfdnDOM(String str) {
        List<String> dom = regex(str,"k \\= \\'(.+?)\\'\\;");
        if (dom != null && dom.size() > 0){
            String cfdn = regex(str,"id=\""+dom.get(0)+"\">(.+?)</div>").get(0);
            if (!TextUtils.isEmpty(cfdn)){
                return cfdn;
            }else {
                return "";
            }
        }else {
            return "";
        }
    }

    /**
     * 正则
     * @param text 本体
     * @param pattern 正则式
     * @return List<String>
     */
    private List<String> regex(String text, String pattern){
        try {
            Pattern pt = Pattern.compile(pattern);
            Matcher mt = pt.matcher(text);
            List<String> group = new ArrayList<>();

            while (mt.find()) {
                if (mt.groupCount() >= 1) {
                    if (mt.groupCount()>1){
                        group.add(mt.group(1));
                        group.add(mt.group(2));
                    }else group.add(mt.group(1));
                }
            }
            return group;
        }catch (NullPointerException e){
            Log.i("MATCH","null");
        }
        return null;
    }

    /**
     * 转换list为 ; 符号链接的字符串
     * @param list
     * @return
     */
    public static String listToString(List list ) {
        char separator = ";".charAt(0);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(separator);
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }


    /**
     * 转换为jsoup可用的Hashmap
     * @param list  HttpCookie列表
     * @return Hashmap
     */
    public static Map<String,String> List2Map(List<HttpCookie> list){
        Map<String, String> map = new HashMap<>();
        try {
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    String[] listStr = list.get(i).toString().split("=");
                    map.put(listStr[0], listStr[1]);
                }
                Log.i("List2Map", map.toString());
            } else {
                return map;
            }

        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return map;
    }

    private void e(String tag,String content){
        Log.e(tag,content);
    }

    private void e(String content){
        Log.e("cloudflare",content);
    }


    class AnswerBean{

        private String host;
        private int method;
        private Map<String,String> fromData = new HashMap<>();
        private static final int POST = 0x01;
        private static final int GET = 0x02;


        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getMethod() {
            return method;
        }

        public void setMethod(int method) {
            this.method = method;
        }

        public Map<String, String> getFromData() {
            return fromData;
        }

        public void setFromData(Map<String, String> fromData) {
            this.fromData = fromData;
        }
    }


}
