package net.jhavar.http;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

		Map headers = new HashMap<String,String>();

		try {
			// Set headers to represent a Chrome browser request

			setHeaders(headers);
			// Return HTML content
			return readAndGetResponse(httpClient.get(url, headers));

			// Error is expected to occur due to 403, start receiving error stream.
		} catch (IOException e) {
			e.printStackTrace();
			try {
				return readAndGetResponse(httpClient.get(url, headers));
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		// Return null. If there was valid content, it would have been returned in
		// return html.toString();
		return null;
	}

	public String readAndGetResponse(Response r) throws IOException {
		String html = r.body().string();
		// Store cookies, if required.
		if (this.storeCookies) {
			Map<String, List<String>> rp = r.headers().toMultimap();
			for (String key : rp.keySet()) {
				// If the header type is set cookie, set the cookie
				if (key != null && key.toLowerCase().equals("set-cookie")) {

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
//
//		headers.put("Accept",
//				"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
//		headers.put("Accept-Encoding", "gzip, deflate, br");
//		headers.put("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
//		headers.put("Cache-Control", "no-cache");
//		headers.put("Connection", "keep-alive");
//		headers.put("Pragma", "no-cache");
//		headers.put("Upgrade-Insecure-Requests", "1");
//		headers.put("User-Agent",
//				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36");
		String cookies = getCookiesAsString();
		if(cookies!= null)
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
