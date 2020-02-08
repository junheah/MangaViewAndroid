package net.jhavar.main;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import android.util.Base64;
import java.util.HashMap;

import net.jhavar.exceptions.NotSameHostException;
import net.jhavar.exceptions.NotYetBypassedException;
import net.jhavar.http.HttpGet;
import net.jhavar.http.HttpPost;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class DdosGuardBypass {
	
	private HttpGet hg;
	
	private boolean isBypassed;
	
	private URL url;
	
	public DdosGuardBypass(String url) throws MalformedURLException {
		this.url = new URL(url);
	}
	
	public String bypass() {
		
		try {
			this.hg = new HttpGet(url.toString(), false);
		} catch (MalformedURLException e1) {
			System.out.println("The URL you entered was not proper.");
			return null;
		}
		//Send first get request
		//this.hg.get();
		//Prepare POST request
		//Form parameters
		String h = new String(Base64.encode((url.getProtocol() + "://" + url.getHost()).getBytes(), Base64.DEFAULT));
		String u = new String(Base64.encode("/".getBytes(), Base64.DEFAULT));
		String p = "";
		if(url.getPort() != -1)
			p = new String(Base64.encode((Integer.toString(url.getPort())).getBytes(), Base64.DEFAULT));

		RequestBody postContent = new FormBody.Builder()
				.addEncoded("u", u)
				.addEncoded("h", h)
				.addEncoded("p", p)
				.build();
		try {
			HttpPost hp;
			hp = new HttpPost(url.getProtocol() + "://ddgu.ddos-guard.net/ddgu/", postContent, false);

			//Get the redirect URL
			//hp.post(true) returns the HTML response, while hp.post(false) returns the location from a HTTP 301 code
			String redirUrl = hp.post(false);
			
			//The following request will require cookie storage, so use that.
			this.hg.setStoreCookies(true);
			try {
				this.hg.setUrl(redirUrl);
			} catch (NotSameHostException e) {
				e.printStackTrace();
			}			
			this.hg.get();
			
			//AT THIS STAGE WE HAVE BYPASSED, return the cookie back to the user so they can use it
			this.isBypassed = true;
			return this.hg.getCookiesAsString();
		} catch(MalformedURLException e) {
			System.out.println("The URL you entered was not proper.");
			return null;
		}
	}
	
	public String get(String page) throws MalformedURLException, NotSameHostException, NotYetBypassedException {
		if(!this.isBypassed)
			throw new NotYetBypassedException("You have to bypass before getting a page using the bypass() method.");
		
		this.hg.setUrl(page);
		return hg.get();
	}
	
	public String getCookiesAsString() throws NotYetBypassedException  {
		if(!this.isBypassed)
			throw new NotYetBypassedException("You have to bypass before getting a page using the bypass() method.");		
		
		return this.hg.getCookiesAsString();
	}
	
	public HashMap<String, String> getCookies() throws NotYetBypassedException {
		if(!this.isBypassed)
			throw new NotYetBypassedException("You have to bypass before getting a page using the bypass() method.");
		
		return this.hg.getCookies();
	}

	public URL getUrl() {
		return url;
	}
}
