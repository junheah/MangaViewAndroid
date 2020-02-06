package net.jhavar.http;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import ml.melun.mangaview.mangaview.CustomHttpClient;

import net.jhavar.exceptions.NotSameHostException;

public class HttpGet {

	private final String host;

	private String url;

	private boolean storeCookies;
	private HashMap<String, String> cookies;
	private String proxyIp;
	private int proxyPort;

	public HttpGet(String url, boolean storeCookies) throws MalformedURLException {

		this.url = url;
		this.host = new URL(url).getHost();
		this.storeCookies = storeCookies;

		if (this.storeCookies)
			cookies = new HashMap<String, String>();
	}

	public HttpGet(String url, boolean storeCookies, String proxyIp, int proxyPort) throws MalformedURLException {

		this(url, storeCookies);

		this.proxyIp = proxyIp;
		this.proxyPort = proxyPort;
	}

	public String get() {

		URL obj;

		// Form URL
		try {
			obj = new URL(url);
		} catch (MalformedURLException e) {
			System.out.println("Bad URL in HttpGet");
			return null;
		}

		HttpURLConnection con = null;
		// Open connection
		try {
			if (this.proxyIp != null) {
				Proxy p = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(this.proxyIp, this.proxyPort));
				con = (HttpURLConnection) obj.openConnection(p);
			} else {
				con = (HttpURLConnection) obj.openConnection();
			}
		} catch (IOException e) {
			System.out.println("Could not establish connection to destination URL in HttpGet");
		}

		// If connection is open, proceed. Otherwise, return null.
		if (con != null) {

			try {
				// Set method as GET
				con.setRequestMethod("GET");
				// 30s timeout for both connecting and reading.
				con.setConnectTimeout(30000);
				con.setReadTimeout(30000);
				con.setInstanceFollowRedirects(false);
				// Set headers to represent a Chrome browser request
				setHeaders(con);
				// Return HTML content
				return readAndGetResponse(con, false);

				// Error is expected to occur due to 403, start receiving error stream.
			} catch (IOException e) {
				try {
					return readAndGetResponse(con, true);
				} catch (IOException e1) {
					System.out.println(e1.getMessage());
				}
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
	public String readAndGetResponse(HttpURLConnection con, boolean errorStream) throws IOException {

		try (BufferedReader in = new BufferedReader(new InputStreamReader(getValidStream(con, errorStream)))) {
			// Read HTML line by line
			String inputLine;
			StringBuilder html = new StringBuilder();
			// Append htmlContent line by line
			while ((inputLine = in.readLine()) != null) {
				html.append(inputLine);
			}

			// Store cookies, if required.
			if (this.storeCookies) {
				Map<String, List<String>> rp = con.getHeaderFields();
				for (String key : rp.keySet()) {
					// If the header type is set cookie, set the cookie
					if (key != null && key.equals("Set-Cookie")) {

						for (String s : rp.get(key)) {
							// Split at ';'
							String[] content = s.split(";");
							// Then, split the first bit at "=" to get key, value
							content = content[0].split("=");
							// Set in hashmap
							cookies.put(content[0], content[1]);
						}

					}
				}
			}

			return html.toString();
			// return html.toString();
		}
	}

	public String getProxyIp() {
		return proxyIp;
	}

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyIp(String proxyIp) {
		this.proxyIp = proxyIp;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	private void setHeaders(HttpURLConnection con) {

		con.setRequestProperty("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
		con.setRequestProperty("Accept-Encoding", "gzip, deflate");
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
		con.setRequestProperty("Cache-Control", "no-cache");
		con.setRequestProperty("Connection", "keep-alive");
		con.setRequestProperty("Pragma", "no-cache");
		con.setRequestProperty("Upgrade-Insecure-Requests", "1");
		con.setRequestProperty("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
		String cookies;
		if((cookies = getCookiesAsString()) != null)
			con.setRequestProperty("Cookie", cookies);
	}

	public void setUrl(String url) throws MalformedURLException, NotSameHostException {
		// Cannot change host.
		if (new URL(url).getHost().equals(this.host)) {
			this.url = url;
		} else {
			throw new NotSameHostException("Cannot change the host in HttpGet class.");
		}

	}

	private InputStream getValidStream(HttpURLConnection con, boolean error) throws IOException {
		if (con.getHeaderField("content-encoding") != null && con.getHeaderField("content-encoding").equals("gzip")) {
			if(error) {
				return new GZIPInputStream(con.getErrorStream());
			} else {
				return new GZIPInputStream(con.getInputStream());
			}
			
		} else {
			if(error) {
				return con.getErrorStream();
			} else {
				return con.getInputStream();
			}
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
