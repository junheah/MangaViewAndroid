package net.jhavar.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
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

import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpPost {

	private final String host;

	private String url;
	private RequestBody postContent;

	private boolean storeCookies;
	private HashMap<String, String> cookies;

	public HttpPost(String url, RequestBody postContent, boolean storeCookies) throws MalformedURLException {

		this.url = url;

		this.host = new URL(url).getHost();

		this.postContent = postContent;
		this.storeCookies = storeCookies;

		if (this.storeCookies)
			cookies = new HashMap<String, String>();

	}

	public String post(boolean getResponseString) {

		URL obj;
		Map<String, String> headers = new HashMap<>();

		// Form URL
		System.out.println("pppp   "+url);
		try {
			obj = new URL(url);
		} catch (MalformedURLException e) {
			System.out.println("Bad URL in HttpPost");
			return null;
		}

		// If connection is open, proceed. Otherwise, return null.

		try {

			// Set headers to represent a Chrome browser request
			setHeaders(headers);

			// Write POST content

			// Receive HTML response
			Response r = httpClient.post(url, this.postContent);
			if (getResponseString == true) {
				return readAndGetResponse(r);
			} else {
				return r.header("Location");
			}

		} catch (IOException e) {
			System.out.println(e.getMessage());
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
	public String readAndGetResponse(Response r) throws IOException {
		String html = r.body().string();

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

	private void setHeaders(Map<String, String> headers) {

		headers.put("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		headers.put("Accept-Encoding", "gzip, deflate");
		headers.put("Accept-Language", "en-US,en;q=0.9");
		headers.put("Cache-Control", "no-cache");
		headers.put("Connection", "keep-alive");
		headers.put("Content-Type", "application/x-www-form-urlencoded");
		headers.put("Pragma", "no-cache");
		headers.put("Upgrade-Insecure-Requests", "1");
		headers.put("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
		String cookies;
		if((cookies = getCookiesAsString()) != null)
			headers.put("Cookie", cookies);
	}

//	private InputStream getValidStream(Response r) throws IOException {
//		if (con.getHeaderField("content-encoding") != null && con.getHeaderField("content-encoding").equals("gzip")) {
//			return new GZIPInputStream(con.getInputStream());
//		} else {
//			return con.getInputStream();
//		}
//	}


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

	public void setUrl(String url) throws MalformedURLException, NotSameHostException {
		// Cannot change host.
		if (new URL(url).getHost().equals(this.host)) {
			this.url = url;
		} else {
			throw new NotSameHostException("Cannot change the host in HttpPost class.");
		}
	}

	public String getUrl() {
		return this.url;
	}

	public HashMap<String, String> getCookies() {
		return this.cookies;
	}

	public void setCookies(HashMap<String, String> cookies) {
		this.cookies = cookies;
	}
}
