package net.jhavar.main;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Base64;
import java.util.HashMap;

import net.jhavar.exceptions.NotSameHostException;
import net.jhavar.exceptions.NotYetBypassedException;
import net.jhavar.http.HttpGet;
import net.jhavar.http.HttpPost;

public class DdosGuardBypass {
	
	private HttpGet hg;
	
	private String proxyIp;
	private int proxyPort;
	
	private boolean isBypassed;
	
	private URL url;
	
	public DdosGuardBypass(String url) throws MalformedURLException {
		this.url = new URL(url);
	}
	
	public DdosGuardBypass(String url, String proxyIp, int proxyPort) throws MalformedURLException {
		this(url);
		
		this.proxyIp = proxyIp;
		this.proxyPort = proxyPort;
	}
	
	public String bypass() {
		
		try {
			if(proxyIp != null)
				this.hg = new HttpGet(url.toString(), false, this.proxyIp, this.proxyPort);
			else
				this.hg = new HttpGet(url.toString(), false);
		} catch (MalformedURLException e1) {
			System.out.println("The URL you entered was not proper.");
			return null;
		}
		//Send first get request
		this.hg.get();
		//Prepare POST request
		//Form parameters
		String h = Base64.getEncoder().encodeToString((url.getProtocol() + "://" + url.getHost()).getBytes());
		String u = Base64.getEncoder().encodeToString("/".getBytes());
		String p = "";
		if(url.getPort() != -1)
			p = Base64.getEncoder().encodeToString((Integer.toString(url.getPort())).getBytes());
		
		try {
			h = URLEncoder.encode(h, "UTF-8");
			u = URLEncoder.encode(u, "UTF-8");
			p = URLEncoder.encode(p, "UTF-8");	
		} catch(UnsupportedEncodingException e) {
			System.out.println("Internal error occured in bypass. (UTF-8 encoding not supported?)");
		}
		
		String postContent = String.format("u=%s&h=%s&p=%s", u, h, p);
		try {
			HttpPost hp;
			if(this.proxyIp != null) {
				hp = new HttpPost(url.getProtocol() + "://ddgu.ddos-guard.net/ddgu/", postContent, false, this.proxyIp, this.proxyPort);				
			} else {
				hp = new HttpPost(url.getProtocol() + "://ddgu.ddos-guard.net/ddgu/", postContent, false);
			}
			//Sleep 5 seconds
			try {
				Thread.sleep(5000);
			} catch(InterruptedException e) {
				System.out.println("bypass was not given enough time to pause as thread was interrupted.");
			}
			//Get the redirect URL
			//hp.post(true) returns the HTML response, while hp.post(false) returns the location from a HTTP 301 code
			String redirUrl = hp.post(false);
			
			//The following request will require cookie storage, so use that.
			this.hg.setStoreCookies(true);
			try {
				this.hg.setUrl(redirUrl);
			} catch (NotSameHostException e) {
				System.out.println(e.getMessage());
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
