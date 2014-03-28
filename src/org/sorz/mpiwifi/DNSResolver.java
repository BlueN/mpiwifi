package org.sorz.mpiwifi;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DNSResolver implements Runnable {
	private String domain;
	private InetAddress inetAddress;

	DNSResolver(String domain) {
		this.domain = domain;
	}

	@Override
	public void run() {
		try {
			setAddr(InetAddress.getByName(domain));
		} catch (UnknownHostException e) {
			inetAddress = null;
		}
	}

	private synchronized void setAddr(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
	}

	public synchronized InetAddress getAddr() {
		return inetAddress;
	}

	public static InetAddress lookup(String host, int timeout)
			throws UnknownHostException {
		DNSResolver dnsResolver = new DNSResolver(host);
		Thread thread = new Thread(dnsResolver);
		thread.start();
		try {
			thread.join(timeout);
		} catch (InterruptedException e) {
			throw new UnknownHostException();
		}
		InetAddress inetAddress = dnsResolver.getAddr();
		if (inetAddress == null)
			throw new UnknownHostException();
		else
			return inetAddress;
	}
}
