package org.sorz.mpiwifi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sorz.mpiwifi.exceptions.AlreadyConnectedException;
import org.sorz.mpiwifi.exceptions.LoginFailException;
import org.sorz.mpiwifi.exceptions.NetworkException;
import org.sorz.mpiwifi.exceptions.NoNetworkAccessException;
import org.sorz.mpiwifi.exceptions.UnknownNetworkException;

public class WifiLoginer {
	private final static int TIMEOUT = 2000;
	private final static String REGEXP_ARGS = "id=\"(\\w+?)\" value='(\\w*?)'";
	private final static String REGEXP_IPADDRESS = "(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d?\\d)";
	private final static String FIX_ARGS = "username=%s&usernameHidden=%s&pwd=%s&seczone&validcode=no_check";


	private static String readPage(InputStream inputStream, int bufferSize)
			throws IOException {
		Reader reader = new InputStreamReader(inputStream);
		char[] buffer = new char[bufferSize];
		int length = reader.read(buffer);
		return new String(buffer, 0, length);
	}

	private static String getRedirectPage() throws AlreadyConnectedException,
			NoNetworkAccessException {
		HttpURLConnection conn = null;
		try {
			DNSResolver.lookup("client3.google.com", TIMEOUT);
			// HttpURLConnection has not a custom timeout during NS lookup.
			URL url = new URL("http://client3.google.com/generate_204");
			conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT);

			if (conn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT)
				throw new AlreadyConnectedException();

			return readPage(conn.getInputStream(), 1024);

		} catch (MalformedURLException e) {
			return ""; // Cannot occur
		} catch (IOException e) {
			throw new NoNetworkAccessException(e);
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	private static URL parseUrlFromRedirectPage(String page)
			throws UnknownNetworkException {
		Matcher matcher = Pattern.compile("'(http://.*)'").matcher(page);
		if (!matcher.find())
			throw new UnknownNetworkException(
					"URL not found on the redirect page.");
		try {
			URL url = new URL(matcher.group(1) + "&flag=location");
			return url;
		} catch (MalformedURLException e) {
			throw new UnknownNetworkException(
					"Malformed URL on the redirect page.");
		}
	}

	private static String getLoginPage(URL loginUrl) throws NetworkException {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) loginUrl.openConnection();
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT * 2);

			return readPage(conn.getInputStream(), 10240);

		} catch (IOException e) {
			throw new NetworkException(e);
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	private static String parseLoginArgs(String loginPage) {
		Matcher matcher = Pattern.compile(REGEXP_ARGS).matcher(loginPage);
		String args = "";
		while (matcher.find())
			args += String.format("%s=%s&", matcher.group(1), matcher.group(2));
		return args;
	}

	private static String postLoginPage(URL postUrl, String args)
			throws NetworkException {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) postUrl.openConnection();
			conn.setConnectTimeout(TIMEOUT);
			conn.setReadTimeout(TIMEOUT * 2);
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setInstanceFollowRedirects(true);
			conn.connect();
			Writer writer = new OutputStreamWriter(conn.getOutputStream());
			writer.write(args);
			writer.flush();
			writer.close();

			return readPage(conn.getInputStream(), 10240);
		} catch (IOException e) {
			throw new NetworkException();
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public static String login(String netId, String pwd)
			throws NetworkException, UnknownNetworkException,
			LoginFailException, AlreadyConnectedException,
			NoNetworkAccessException {

		// Read redirect page, get login page URL and post URL:
		String redirectPage = getRedirectPage();
		URL loginPageUrl = parseUrlFromRedirectPage(redirectPage);
		URL postUrl;
		try {
			postUrl = new URL(loginPageUrl, "user.do?method=login_ajax");
		} catch (MalformedURLException e) {
			throw new UnknownNetworkException(e);
		}

		// Read login page, get POST arguments:
		String loginPage = getLoginPage(loginPageUrl);
		String args = parseLoginArgs(loginPage);

		String netIdEncoded;
		String pwdEncoded;
		try {
			netIdEncoded = URLEncoder.encode(netId, "utf-8");
			pwdEncoded = URLEncoder.encode(pwd, "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new LoginFailException("Net-ID or password encoding error.",
					e);
		}
		args += String.format(FIX_ARGS, netIdEncoded, netIdEncoded, pwdEncoded);

		// Send the POST:
		String resultPage = postLoginPage(postUrl, args);

		// Check (find IP address):
		Matcher matcher = Pattern.compile(REGEXP_IPADDRESS).matcher(resultPage);
		if (!matcher.find())
			throw new LoginFailException(
					"Can't found IP address on result page.");
		return matcher.group();
	}

}
