package com.squareup.okhttp;
import com.squareup.okhttp.internal.http.HttpAuthenticator;
import com.squareup.okhttp.internal.http.HttpURLConnectionImpl;
import com.squareup.okhttp.internal.http.HttpsURLConnectionImpl;
import com.squareup.okhttp.internal.http.OkResponseCache;
import com.squareup.okhttp.internal.http.OkResponseCacheAdapter;
import com.squareup.okhttp.internal.tls.OkHostnameVerifier;
import java.net.CookieHandler;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.ResponseCache;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
/** 
 * Configures and creates HTTP connections. 
 */
public final class OkHttpClient {
  public Proxy proxy;
  public Set<Route> failedRoutes=Collections.synchronizedSet(new LinkedHashSet<Route>());
  public ProxySelector proxySelector;
  public CookieHandler cookieHandler;
  public ResponseCache responseCache;
  public SSLSocketFactory sslSocketFactory;
  public HostnameVerifier hostnameVerifier;
  public OkAuthenticator authenticator;
  public ConnectionPool connectionPool;
  public boolean followProtocolRedirects=true;
  /** 
 * Sets the HTTP proxy that will be used by connections created by this client. This takes precedence over  {@link #setProxySelector}, which is only honored when this proxy is null (which it is by default). To disable proxy use completely, call  {@code setProxy(Proxy.NO_PROXY)}.
 */
  public OkHttpClient setProxy(  Proxy proxy){
    this.proxy=proxy;
    return this;
  }
  public Proxy getProxy(){
    return proxy;
  }
  /** 
 * Sets the proxy selection policy to be used if no  {@link #setProxy proxy}is specified explicitly. The proxy selector may return multiple proxies; in that case they will be tried in sequence until a successful connection is established. <p>If unset, the  {@link ProxySelector#getDefault() system-wide default}proxy selector will be used.
 */
  public OkHttpClient setProxySelector(  ProxySelector proxySelector){
    this.proxySelector=proxySelector;
    return this;
  }
  public ProxySelector getProxySelector(){
    return proxySelector;
  }
  /** 
 * Sets the cookie handler to be used to read outgoing cookies and write incoming cookies. <p>If unset, the  {@link CookieHandler#getDefault() system-wide default}cookie handler will be used.
 */
  public OkHttpClient setCookieHandler(  CookieHandler cookieHandler){
    this.cookieHandler=cookieHandler;
    return this;
  }
  public CookieHandler getCookieHandler(){
    return cookieHandler;
  }
  /** 
 * Sets the response cache to be used to read and write cached responses. <p>If unset, the  {@link ResponseCache#getDefault() system-wide default}response cache will be used.
 */
  public OkHttpClient setResponseCache(  ResponseCache responseCache){
    this.responseCache=responseCache;
    return this;
  }
  public ResponseCache getResponseCache(){
    return responseCache;
  }
  public OkResponseCache okResponseCache(){
    if (responseCache instanceof HttpResponseCache) {
      return ((HttpResponseCache)responseCache).okResponseCache;
    }
 else     if (responseCache != null) {
      return new OkResponseCacheAdapter(responseCache);
    }
 else {
      return null;
    }
  }
  /** 
 * Sets the socket factory used to secure HTTPS connections. <p>If unset, the  {@link HttpsURLConnection#getDefaultSSLSocketFactory() system-wide default} SSL socket factory will be used.
 */
  public OkHttpClient setSSLSocketFactory(  SSLSocketFactory sslSocketFactory){
    this.sslSocketFactory=sslSocketFactory;
    return this;
  }
  public SSLSocketFactory getSslSocketFactory(){
    return sslSocketFactory;
  }
  /** 
 * Sets the verifier used to confirm that response certificates apply to requested hostnames for HTTPS connections. <p>If unset, the  {@link HttpsURLConnection#getDefaultHostnameVerifier() system-wide default} hostname verifier will be used.
 */
  public OkHttpClient setHostnameVerifier(  HostnameVerifier hostnameVerifier){
    this.hostnameVerifier=hostnameVerifier;
    return this;
  }
  public HostnameVerifier getHostnameVerifier(){
    return hostnameVerifier;
  }
  /** 
 * Sets the authenticator used to respond to challenges from the remote web server or proxy server. <p>If unset, the  {@link Authenticator#setDefault system-wide default}authenticator will be used.
 */
  public OkHttpClient setAuthenticator(  OkAuthenticator authenticator){
    this.authenticator=authenticator;
    return this;
  }
  public OkAuthenticator getAuthenticator(){
    return authenticator;
  }
  /** 
 * Sets the connection pool used to recycle HTTP and HTTPS connections. <p>If unset, the  {@link ConnectionPool#getDefault() system-widedefault} connection pool will be used.
 */
  public OkHttpClient setConnectionPool(  ConnectionPool connectionPool){
    this.connectionPool=connectionPool;
    return this;
  }
  public ConnectionPool getConnectionPool(){
    return connectionPool;
  }
  /** 
 * Configure this client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS. <p>If unset, protocol redirects will be followed. This is different than the built-in  {@code HttpURLConnection}'s default.
 */
  public OkHttpClient setFollowProtocolRedirects(  boolean followProtocolRedirects){
    this.followProtocolRedirects=followProtocolRedirects;
    return this;
  }
  public boolean getFollowProtocolRedirects(){
    return followProtocolRedirects;
  }
  public HttpURLConnection open(  URL url){
    String protocol=url.getProtocol();
    OkHttpClient copy=copyWithDefaults();
    if (protocol.equals("http")) {
      return new HttpURLConnectionImpl(url,copy,copy.okResponseCache(),copy.failedRoutes);
    }
 else     if (protocol.equals("https")) {
      return new HttpsURLConnectionImpl(url,copy,copy.okResponseCache(),copy.failedRoutes);
    }
 else {
      throw new IllegalArgumentException("Unexpected protocol: " + protocol);
    }
  }
  /** 
 * Returns a shallow copy of this OkHttpClient that uses the system-wide default for each field that hasn't been explicitly configured.
 */
  public OkHttpClient copyWithDefaults(){
    OkHttpClient result=new OkHttpClient();
    result.proxy=proxy;
    result.failedRoutes=failedRoutes;
    result.proxySelector=proxySelector != null ? proxySelector : ProxySelector.getDefault();
    result.cookieHandler=cookieHandler != null ? cookieHandler : CookieHandler.getDefault();
    result.responseCache=responseCache != null ? responseCache : ResponseCache.getDefault();
    result.sslSocketFactory=sslSocketFactory != null ? sslSocketFactory : HttpsURLConnection.getDefaultSSLSocketFactory();
    result.hostnameVerifier=hostnameVerifier != null ? hostnameVerifier : new OkHostnameVerifier();
    result.authenticator=authenticator != null ? authenticator : HttpAuthenticator.SYSTEM_DEFAULT;
    result.connectionPool=connectionPool != null ? connectionPool : ConnectionPool.getDefault();
    result.followProtocolRedirects=followProtocolRedirects;
    return result;
  }
  public OkHttpClient(){
  }
}
