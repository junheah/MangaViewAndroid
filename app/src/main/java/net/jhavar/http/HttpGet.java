package net.jhavar.http;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import static ml.melun.mangaview.MainApplication.httpClient;

import net.jhavar.exceptions.NotSameHostException;

import okhttp3.Response;

public class HttpGet {

	private final String host;
	private String url;
	private boolean storeCookies;
	private HashMap<String, String> cookies;

	public HttpGet(String url, boolean storeCookies) throws MalformedURLException {

		this.url = url;
		this.host = new URL(url).getHost();
		this.storeCookies = storeCookies;

		if (this.storeCookies)
			cookies = new HashMap<String, String>();
	}

	public String get() {

		URL obj;
		Map headers = new HashMap<String,String>();

		// Form URL
		try {
			obj = new URL(url);
		} catch (MalformedURLException e) {
			System.out.println("Bad URL in HttpGet");
			return null;
		}

		try {
			// Set headers to represent a Chrome browser request

			setHeaders(headers);
			// Return HTML content
			return readAndGetResponse(httpClient.get(url, headers), false);

			// Error is expected to occur due to 403, start receiving error stream.
		} catch (IOException e) {
			try {
				return readAndGetResponse(httpClient.get(url, headers), true);
			} catch (IOException e1) {
				System.out.println(e1.getMessage());
			}
		}
		// Return null. If there was valid content, it would have been returned in
		// return html.toString();
		return null;
	}

	/*
	 * 
	 * The error response is compressed using GZIP. It is originally Brotli, but the
	 * Accept-Encoding header has been altered to only accept gzip
	 * 
	 */
	public String readAndGetResponse(Response r, boolean errorStream) throws IOException {
		String html = r.body().string();
		System.out.println(r.code());

		// Store cookies, if required.
		if (this.storeCookies) {
			Map<String, List<String>> rp = r.headers().toMultimap();
			for (String key : rp.keySet()) {
				// If the header type is set cookie, set the cookie
				if (key != null && key.equals("Set-Cookie")) {

					for (String s : rp.get(key)) {
						// Split at ';'
						String[] content = s.split(";");
						// Then, split the first bit at "=" to mget key, value
						content = content[0].split("=");
						// Set in hashmap
						cookies.put(content[0], content[1]);
					}

				}
			}
		}

		return html;
		// return html.toString();
	}

	private void setHeaders(Map<String,String> headers) {

		headers.put("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("Accept-Language", "en-US,en;q=0.9");
		headers.put("Cache-Control", "no-cache");
		headers.put("Connection", "keep-alive");
		headers.put("Pragma", "no-cache");
		headers.put("Upgrade-Insecure-Requests", "1");
		headers.put("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
		String cookies;
		if((cookies = getCookiesAsString()) != null)
			headers.put("Cookie", cookies);
	}

	public void setUrl(String url) throws MalformedURLException, NotSameHostException {
		// Cannot change host.
		if (new URL(url).getHost().equals(this.host)) {
			this.url = url;
		} else {
			throw new NotSameHostException("Cannot change the host in HttpGet class.");
		}

	}

	public String getUrl() {
		return this.url;
	}
	
	public void setStoreCookies(boolean storeCookies) {
		this.storeCookies = storeCookies;
		if(this.storeCookies)
			this.cookies = new HashMap<String, String>();
	}

	public HashMap<String, String> getCookies() {
		return this.cookies;
	}

	public void setCookies(HashMap<String, String> cookies) {
		this.cookies = cookies;
	}

	public String getCookiesAsString() {
		if (this.storeCookies && this.cookies.isEmpty() == false) {
			// Build cookies
			StringBuilder s = new StringBuilder();
			for (String key : cookies.keySet()) {
				s.append(key + "=" + cookies.get(key) + "; ");
			}

			String finalCookies = s.toString();
			// Remove last character which is ';' as the last cookie does not have a ';'
			finalCookies = finalCookies.substring(0, finalCookies.length() - 2);
			// Set the cookie property
			return finalCookies;
		}

		return null;
	}

}
