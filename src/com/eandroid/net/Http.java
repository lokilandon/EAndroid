/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpRequestRetryHandler;

import com.eandroid.cache.Cache;
import com.eandroid.content.EContext;
import com.eandroid.net.http.DownloadHandler;
import com.eandroid.net.http.HttpConnector;
import com.eandroid.net.http.HttpHandler;
import com.eandroid.net.http.RequestEntity;
import com.eandroid.net.http.UploadHandler;
import com.eandroid.net.http.response.BitmapResponseParser;
import com.eandroid.net.http.response.FileResponseParser;
import com.eandroid.net.http.response.ObjectResponseParser;
import com.eandroid.net.http.response.ResponseParser;
import com.eandroid.net.http.util.HttpAsyncTask;
import com.eandroid.net.http.util.HttpParams;
import com.eandroid.net.http.util.HttpSyncHandler;
import com.eandroid.net.http.util.SerialExecutor;
import com.eandroid.net.http.util.TaskControlCenter;
import com.eandroid.net.impl.BasicNetIOFilterChain;
import com.eandroid.net.impl.http.CurrentThreadResponseHandlerDecorator;
import com.eandroid.net.impl.http.HttpClientConnector;
import com.eandroid.net.impl.http.HttpRequestExecutionException;
import com.eandroid.net.impl.http.HttpRequestSession;
import com.eandroid.net.impl.http.ResponseFuture;
import com.eandroid.net.impl.http.filter.BasicHttpFilter;
import com.eandroid.net.impl.http.filter.HttpResponseDiskCacheFilter;
import com.eandroid.net.impl.http.filter.HttpResponseParseFilter;
import com.eandroid.net.impl.http.response.DefaultBitmapResponseParser;
import com.eandroid.net.impl.http.response.DefaultFileResponseParser;
import com.eandroid.util.ClassUtils;


public class Http {

	private final String TAG;
	private static List<BasicHttpFilter> filters = new ArrayList<BasicHttpFilter>();
	private volatile HttpResponseParseFilter responseParserFilter;
	private volatile HttpResponseDiskCacheFilter responseDiskCacheFilter;
	private volatile NetIOFilterChain filterChain;
	private volatile HttpConnector connector;
	private AtomicBoolean closed = new AtomicBoolean();

	/**应答解析默认设置项*/
	private static ResponseParser<Object> defaultObjectParser;
	private static FileResponseParser defaultFileParser = new DefaultFileResponseParser();
	private static BitmapResponseParser defaultBitmapParser = new DefaultBitmapResponseParser();
	private static Charset defaultResponseContentCharset = Charset.forName("utf-8");

	//应答缓存设置
	private static boolean defaultCacheFile = false;
	private static boolean defaultCacheBitmap = false;
	private static boolean defaultCacheOtherObject = false;
	private static boolean defaultContinueOnCacheHit = true;
	private static long defaultDiskMaxCacheSize =  10*1024*1024;	
	private static long defaultDiskCacheExpired =  10 * 1000;
	private static String defaultCacheDirectoryPath = EContext.getCacheFilePath() + File.separator + "HTTP";

	/**线程池设置*/
	private static int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
	private static int MAXIMUM_POOL_SIZE = CORE_POOL_SIZE * 2;
	private static int KEEP_ALIVE = 1;

	private ThreadFactory defaultThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r, "EAndroid-"+TAG+" #" + mCount.getAndIncrement());
			thread.setDaemon(true);
			thread.setPriority(3);
			return thread;
		}
	};
	private final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(20);
	private volatile ThreadPoolExecutor THREAD_POOL_EXECUTOR;
	public volatile Executor SERIAL_EXECUTOR;


	private volatile static Map<String,Http> instanceMap;

	/**
	 * 打开Http客户端
	 * @return 返回默认的客户端，没有则新建一个
	 */
	public static Http open() {
		return open("DefaultClient",null);
	}

	/**
	 * 打开Http客户端
	 * @param name 客户端名称
	 * @return 返回相应的客户端，没有则新建一个
	 */
	public static Http open(String name){
		return open(name,null,null);
	}
	
	/**
	 * 打开Http客户端
	 * @param name 客户端名称
	 * @return 返回相应的客户端，没有则新建一个
	 */
	public static Http open(String name,HttpConnector connector){
		return open(name,null,connector);
	}

	/**
	 * 打开Http客户端
	 * @param name 客户端名称
	 * @param cookieStore 此客户端需要设置的Cookie 
	 * @return 返回相应的客户端，没有则新建一个
	 */
	public static Http open(String key,CookieStore cookieStore,HttpConnector connector){
		if(instanceMap == null){
			synchronized (Http.class) {
				if(instanceMap == null){
					instanceMap = new HashMap<String, Http>();
				}
			}
		}
		Http client = instanceMap.get(key);
		if(client == null || client.isClosed()){
			synchronized (Http.class) {
				if(instanceMap.get(key) == null){
					client = new Http(key,connector);
					instanceMap.put(key, client);
				}
			}
		}
		if(cookieStore != null){
			client.connector.setCookieStore(cookieStore);
		}
		return client;
	}

	private Http(String key,HttpConnector connector){
		TAG = key;
		filterChain = createFilterChain();
		if(connector == null)
			this.connector = createHttpConnector();
		else 
			this.connector = connector;
		THREAD_POOL_EXECUTOR = createThreadPoolExecutor();
		SERIAL_EXECUTOR = new SerialExecutor(THREAD_POOL_EXECUTOR);
		initFilterChain();
	}

	public HttpConnector getConnector() {
		return connector;
	}

	private void initFilterChain(){
		if(filterChain == null)
			return;
		if(responseDiskCacheFilter != null)
			responseDiskCacheFilter.closeCache();
		responseDiskCacheFilter = createResponseCacheFilter();
		filterChain.addFilter(responseDiskCacheFilter);
		responseParserFilter = createReponseParseFilter();
		filterChain.addFilter(responseParserFilter);
		//		filterChain.addFilter(new LogFilter());
		filterChain.addAllFilters(filters);
	}

	/**
	 * 添加一个默认的过滤器<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param filter
	 */
	public static void addDefaultFilter(BasicHttpFilter filter){
		filters.add(filter);
	}

	/**
	 * 移除一个默认的过滤器<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param filter
	 */
	public static void removeDefaultFilter(BasicHttpFilter filter){
		filters.remove(filter);
	}

	/**
	 * 清楚所有用户添加的默认过滤器<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 */
	public static void clearDefaultFilter(){
		filters.clear();
	}

	/**
	 * 添加过滤器
	 * @param filter
	 * @return
	 */
	public Http addFilter(BasicHttpFilter filter){
		filterChain.addFilter(filter);
		return this;
	}

	/**
	 * 移除过滤器
	 * @param filter
	 * @return
	 */
	public Http removeFilter(BasicHttpFilter filter){
		filterChain.removeFilter(filter);
		return this;
	}

	/**
	 * 设置默认的线程池核心线程数量<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效<br/>
	 * 默认为移动设备核心数的2倍
	 * @param size 核心线程数量
	 */
	public static void configDefaultThreadPoolCoreSize(int size){
		CORE_POOL_SIZE = size;
	}

	/**
	 * 设置默认的线程池最大线程数量<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效<br/>
	 * 默认为移动设备核心数的4倍
	 * @param maxSize 最大线程数量
	 */
	public static void configDefaultThreadPoolMaxSize(int maxSize){
		MAXIMUM_POOL_SIZE = maxSize;
	}

	/**
	 * 设置默认的线程空闲时间，<br/>
	 * 若在线程池的大于核心线程数以外的线程空闲时间超过此设定值，线程会将停止<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效<br/>
	 * 默认为  1s
	 * @param timeInSeconds 线程空闲活跃时间 秒为单位
	 */
	public static void configDefaultThreadPoolKeepAlive(int timeInSeconds){
		KEEP_ALIVE = timeInSeconds;
	}

	/**
	 * 设置默认的超时时间<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效<br/>
	 * 默认连接超时为20s 读取超时为15s
	 * @param connectionTimeout  连接超时时间 s
	 * @param socketTimeout      socket读取超时 s
	 */
	public static void configDefaultTimeoutInSeconds(int connectionTimeout,int socketTimeout){
		HttpClientConnector.connectionTimeout = connectionTimeout;
		HttpClientConnector.socketTimeout = socketTimeout;
	}

	/**
	 * 设置是否使用 Nagle 算法<br/>
	 * Nagle 算法视图通过最小化发送的分组数量来节省带宽。为减少延时增加性能时可设置为true<br/>
	 * 默认不启用<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param tcpNoDelay
	 */
	public static void configDefaultTCPNoDelay(boolean tcpNoDelay){
		HttpClientConnector.tcpNoDelay = tcpNoDelay;
	}

	/**
	 * 设置是否检查连接的有效性<br/>
	 * 设置true的话，会损失一些性能，<br/>
	 * 设置false会导致，偶尔的一些连接异常<br/>
	 * 默认为true<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param staleCheck
	 */
	public static void configDefaultStaleCheck(boolean staleCheck){
		HttpClientConnector.staleCheck = staleCheck;
	}

	/**
	 * 设置socket关闭后，当输出缓冲区中还有内容时，是强制退出，还是等待一段时间看是否能够发送完毕。
	 * @param linger 等待的时间   如果设置为 0 不启用，为 -1 使用dalvik默认规则
	 */
	public static void configDefaultSocketLinger(int linger){
		HttpClientConnector.linger = linger;
	}

	/**
	 * 设置默认的Socket读写缓冲区大小<br/>
	 * 默认为8kb<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param bufferSize 单位Byte
	 */
	public static void configDefaultSocketBuffer(int bufferSize){
		HttpClientConnector.socketBufferSize = bufferSize;
	}

	/**
	 * 设置默认的请求内容编码<br/>
	 * 默认为utf-8<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param charset
	 */
	public static void configDefaultRequestContentCharset(Charset charset){
		HttpClientConnector.contentCharset = charset;
	}

	/**
	 * 设置默认的请求头信息编码<br/>
	 * 默认为utf-8<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param charset
	 */
	public static void configDefaultRequestHeadCharset(Charset charset){
		HttpClientConnector.httpElementCharset = charset;
	}

	/**
	 * 设置默认的head中user-agent信息<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param userAgent
	 */
	public static void configDefaultUserAgent(String userAgent){
		HttpClientConnector.userAgent = userAgent;
	}

	/**
	 * 设置默认的并发http connection数量<br/>
	 * 默认为20<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param int maxTotalConns
	 */
	public static void configDefaultMaxTotalConnections(int maxTotalConns){
		HttpClientConnector.maxTotalConnections = maxTotalConns;
	}

	/**
	 * 设置默认的RetryHandler<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param HttpRequestRetryHandler retryHandler
	 */
	public static void configDefaultRetryHandlers(HttpRequestRetryHandler retryHandler){
		HttpClientConnector.retryHandler = retryHandler;
	}

	/**
	 * 设置默认的File对象解析类<br/>
	 * 此类将直接操作Http应答返回的输入流，并将其转换为File对象返回<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param FileResponseParser parser
	 */
	public static void configDefaultFileResponseParser(FileResponseParser parser){
		defaultFileParser = parser;
	}

	/**
	 * 设置默认的通用Object对象解析类<br/>
	 * 此类将直接操作Http应答返回的输入流，并将其转换为请求时设置的返回类型<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param ObjectResponseParser parser
	 */
	public static void configDefaultObjectResponseParser(ResponseParser<Object> parser){
		defaultObjectParser = parser;
	}

	/**
	 * 设置默认的Bitmap对象解析类<br/>
	 * 此类将直接操作Http应答返回的输入流，并将其转换为Bitmap类型返回<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param BitmapResponseParser parser
	 */
	public static void configDefaultBitmapResponseParser(BitmapResponseParser parser){
		defaultBitmapParser = parser;
	}

	/**
	 * 设置默认的应答返回编码<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param responseContentCharset
	 */
	public static void configDefaultResponseContentCharset(Charset responseContentCharset){
		defaultResponseContentCharset = responseContentCharset;
	}

	/**
	 * 设置默认需要缓存的类型<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param cacheBitmap
	 * @param cacheFile
	 * @param cacheOtherObject
	 */
	public static void configDefaultResponseCache(boolean cacheBitmap,boolean cacheFile,boolean cacheOtherObject){
		defaultCacheBitmap = cacheBitmap;
		defaultCacheFile = cacheFile;
		defaultCacheOtherObject = cacheOtherObject;
	}

	/**
	 * 设置缓存参数<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param continueOnCacheHit 是否在缓存命中后继续请求，并返回应答
	 * 		  (如果设置为true，那么每次请求都会先得到缓存中的结果，待请求完成后再收到本次请求的结果，并会更新缓存)
	 * @param cacheExpiredTime 缓存的过期时间
	 */
	public static void configDefaultResponseCache(boolean continueOnCacheHit,long cacheExpiredTime){
		defaultContinueOnCacheHit = continueOnCacheHit;
		defaultDiskCacheExpired = cacheExpiredTime;
	}

	/**
	 * 设置默认的磁盘缓存大小<br/>
	 * 默认为10mb<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param maxCacheSize 单位 byte
	 */
	public static void configDefaultMaxCacheSize(long maxCacheSize){
		defaultDiskMaxCacheSize = maxCacheSize;
	}

	/**
	 * 设置默认的缓存文件夹路径<br/>
	 * 如果更改了缓存目录，并且之前的目录已经有过缓存内容，建议先调用clearCache()方法清楚之前目录的缓存<br/>
	 * 对所有之后新生成的Http客户端都会生效，对于已打开过的Http客户端，需要调用reset()方法才会生效
	 * @param cacheDirectoryName
	 */
	public static void configDefaultCacheDirectoryPath(String cacheDirectoryPath){
		Http.defaultCacheDirectoryPath = cacheDirectoryPath;
	}




	/**
	 * 设置线程池核心线程数量<br/>
	 * 默认为移动设备核心数的2倍
	 * @param size 核心线程数量
	 */
	public Http configThreadPoolCoreSize(int size){
		THREAD_POOL_EXECUTOR.setCorePoolSize(size);
		return this;
	}

	/**
	 * 设置线程池最大线程数量<br/>
	 * 默认为移动设备核心数的4倍
	 * @param maxSize 最大线程数量
	 */
	public Http configThreadPoolMaxSize(int maxSize){
		THREAD_POOL_EXECUTOR.setMaximumPoolSize(maxSize);
		return this;
	}

	/**
	 * 设置线程空闲时间，<br/>
	 * 线程池中，大于核心线程数以外的线程的空闲时间超过此设定值，线程会将停止<br/>
	 * 默认为  1s
	 * @param timeInSeconds 线程空闲活跃时间 秒为单位
	 */
	public Http configThreadPoolKeepAlive(int timeInSeconds){
		THREAD_POOL_EXECUTOR.setKeepAliveTime(timeInSeconds, TimeUnit.SECONDS);
		return this;
	}

	/**
	 * 设置请求头信息，Accecpt-Encoding,对应于应答时的Content-Encoding<br/>
	 * 默认为gzip
	 * @param acceptEncoding
	 * @return
	 */
	public Http configAcceptEncoding(String acceptEncoding){
		((HttpClientConnector)connector).ACCEPT_ENCODING = acceptEncoding;
		return this;
	}

	/**
	 * 设置Http超时时间<br/>
	 * 默认连接超时为20s 读取超时为15s
	 * @param connectionTimeout  连接超时时间 s
	 * @param socketTimeout      socket读取超时 s
	 */
	public Http configTimeoutInSeconds(int connectionTimeout,int socketTimeout){
		((HttpClientConnector)connector).configTimeoutInSeconds(connectionTimeout, socketTimeout);
		return this;
	}

	/**
	 * 设置请求内容编码
	 * 默认为utf-8
	 * @param charset
	 */
	public Http configRequestContentCharset(Charset charset){
		((HttpClientConnector)connector).configContentCharset(charset);
		return this;
	}

	/**
	 * 设置请求头信息编码<br/>
	 * 默认为utf-8
	 * @param charset
	 */
	public Http configRequestHeadCharset(Charset charset){
		((HttpClientConnector)connector).configHeadCharset(charset);
		return this;
	}

	/**
	 * 设置head中user-agent信息
	 * @param userAgent
	 */
	public Http configUserAgent(String userAgent){
		((HttpClientConnector)connector).configUserAgent(userAgent);
		return this;
	}

	/**
	 * 设置RetryHandler
	 * @param retryHandler
	 * @return
	 */
	public Http configRetryHandlers(HttpRequestRetryHandler retryHandler){
		((HttpClientConnector)connector).configRetryHandlers(retryHandler);
		return this;
	}

	/**
	 * 设置默认RetryHandler的配置，
	 * @param retryCount 重试次数
	 * @param requestSentRetryEnabled 是否开启
	 * @param retrySpacingTime 每次重试之间的等待时间 单位：ms
	 * @return
	 */
	public Http configRetryHandlers(int retryCount,boolean requestSentRetryEnabled,int retrySpacingTime){
		((HttpClientConnector)connector).configRetryHandlers(retryCount, requestSentRetryEnabled, retrySpacingTime);
		return this;
	}

	/**
	 * 设置File对象解析类<br/>
	 * 此类将直接操作Http应答返回的输入流，并将其转换为File对象返回<br/>
	 * 默认实现是：<br/>
	 * 如果请求时提供了下载路径，会将文件保存到路径中，在返回File对象<br/>
	 * 如果无下载路径，会将文件保存到临时目录中，返回临时目录中的File对象<br/>
	 * 如果需要更改临时路径的相对位置，请修改EContext.TEMP_PATH<br/>
	 * 临时文件每次启动时Http服务时会清除
	 * @param FileResponseParser parser
	 */
	public Http configFileResponseParser(FileResponseParser parser){
		responseParserFilter.setFileParser(parser);
		return this;
	}

	/**
	 * 设置通用Object对象解析类<br/>
	 * 默认只解析String类型，如果请求时设置为其他类型会抛出无法解析的异常<br/>
	 * 此类将直接操作Http应答返回的输入流，并将其转换为请求时设置的返回类型
	 * @param ObjectResponseParser parser
	 */
	public Http configObjectResponseParser(ObjectResponseParser parser){
		responseParserFilter.setObjectParser(parser);
		return this;
	}

	/**
	 * 设置Bitmap对象解析类<br/>
	 * 此类将直接操作Http应答返回的输入流，并将其转换为Bitmap类型返回<br/>
	 * 默认实现是：<br/>
	 * 如果开启了文件缓存，会将流保存到缓存路径中，再编码成Bitmap对象返回<br/>
	 * 如果未开启缓存，会将文件保存到临时目录中，再编码成Bitmap对象返回<br/>
	 * 如果需要更改临时路径的位置，请修改EContext.TEMP_PATH<br/>
	 * 临时文件每次启动时Http服务时会清除
	 * @param BitmapResponseParser parser
	 */
	public Http configBitmapResponseParser(BitmapResponseParser parser){
		responseParserFilter.setBitmapParser(parser);
		return this;
	}

	/**
	 * 设置应答返回编码
	 * @param responseContentCharset
	 */
	public Http configResponseContentCharset(Charset responseContentCharset){
		responseParserFilter.setDefaultCharset(responseContentCharset);
		return this;
	}

	/**
	 * 设置需要缓存的类型<br/>
	 * 默认均不缓存
	 * @param cacheBitmap 
	 * @param cacheFile
	 * @param cacheOtherObject
	 */
	public Http configResponseCache(boolean cacheBitmap,boolean cacheFile,boolean cacheOtherObject){
		if(responseDiskCacheFilter != null){
			responseDiskCacheFilter.setCacheBitmap(cacheBitmap);
			responseDiskCacheFilter.setCacheFile(cacheFile);
			responseDiskCacheFilter.setCacheOtherObject(cacheOtherObject);
		}
		return this;
	}

	/**
	 * 设置缓存参数
	 * @param continueOnCacheHit 是否在缓存命中后继续请求，并返回应答
	 * 		  (如果设置为true，那么每次请求都会先得到缓存中的结果，待请求完成后再收到本次请求的结果，并会更新缓存)
	 * @param cacheExpiredTime 缓存的过期时间
	 */
	public Http configResponseCache(boolean continueOnCacheHit,long cacheExpiredTime){
		if(responseDiskCacheFilter != null){
			responseDiskCacheFilter.setCacheExpired(cacheExpiredTime);
			responseDiskCacheFilter.setContinueOnCacheHit(continueOnCacheHit);
		}
		return this;
	}

	/**
	 * 设置默认的缓存文件夹路径<br/>
	 * 这个设置如果在磁盘缓存已经初始化后调用（磁盘缓存会在第一次使用到缓存时初始化），会抛出相关异常。<br/>
	 * 如果需要设置，请在Http被打开后，还未进行过请求之前设置
	 * @param cacheDirectoryName
	 */
	public Http configResponseCacheDirectoryPath(String diskCacheDirectoryPath){
		if(responseDiskCacheFilter != null){
			responseDiskCacheFilter.setDiskCacheDirectoryPath(diskCacheDirectoryPath);
		}
		return this;
	}

	/**
	 * 设置默认的磁盘缓存大小<br/>
	 * 默认为20mb<br/>
	 * 这个设置如果在磁盘缓存已经初始化后调用（磁盘缓存会在第一次使用到缓存时初始化），会抛出相关异常。<br/>
	 * 如果更改了缓存目录，建议先调用clearCache()方法清楚之前目录的缓存<br/>
	 * 如果需要设置，请在Http被打开后，还未进行过请求之前设置
	 * @param maxCacheSize 单位 byte
	 */
	public Http configResponseCacheMaxSize(long size){
		if(responseDiskCacheFilter != null){
			responseDiskCacheFilter.setDiskCacheSize(size);
		}
		return this;
	}

	public CookieStore getCookieStore(){
		return connector.getCookieStore();
	}

	public Http resetFilterChain(){
		if(responseDiskCacheFilter != null)
			responseDiskCacheFilter.closeCache();
		filterChain = createFilterChain();
		initFilterChain();
		return this;
	}

	public Http resetConnector(){
		connector.close();
		connector = createHttpConnector();
		return this;
	}

	public Http resetThreadPool(){
		THREAD_POOL_EXECUTOR.shutdownNow();
		THREAD_POOL_EXECUTOR = createThreadPoolExecutor();
		SERIAL_EXECUTOR = new SerialExecutor(THREAD_POOL_EXECUTOR);
		return this;
	}

	public Http reset() {
		return resetConnector().resetThreadPool().resetFilterChain();
	}

	public Cache<String, InputStream> getCache(){
		if(responseDiskCacheFilter == null)
			return null;
		return responseDiskCacheFilter.getCache();
	}

	public Http clearCache() {
		if(responseDiskCacheFilter != null)
			responseDiskCacheFilter.clearCache();
		return this;
	}

	public Http flushCache() {
		if(responseDiskCacheFilter != null)
			responseDiskCacheFilter.flushCache();
		return this;
	}

	public Http closeCache() {
		if(responseDiskCacheFilter != null)
			responseDiskCacheFilter.closeCache();
		return this;
	}

	public boolean isClosed(){
		return closed.get();
	}

	public void close() {
		closed.set(true);
		instanceMap.remove(TAG);
		Connector connector = this.connector;
		connector.close();
		closeCache();
		THREAD_POOL_EXECUTOR.shutdown();
	}

	public String generateRequestKey(String url,Map<String , ? extends Object> paramMap){
		return connector.generateRequestKey(url, paramMap);
	}

	/**
	 * 同步get请求
	 * @param url
	 * @return
	 */
	public <T> T getSync(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz) throws HttpRequestExecutionException{
		if(isClosed())
			throw new IllegalStateException("Http has been Closed");

		if(responseClazz == null){
			throw new IllegalArgumentException("Unknow response class,please set the reponse class ");
		}

		HttpRequestSession session = createRequestSession(connector,filterChain,null);
		HttpSyncHandler<T> requesthandler = new HttpSyncHandler<T>(session);
		RequestEntity requestEntity = session.generateGetEntity(url,
				null,
				param,
				session,
				parser,
				responseClazz);
		return requesthandler.request(requestEntity);
	}

	/**
	 * 异步get请求（串行）
	 * @param url
	 * @return
	 */
	public <T> ResponseFuture<T> getSerial(String url,
			HttpParams param,
			HttpHandler<T> handler){
		return get(url,param,  null,null,handler,true,null);
	}

	public <T> ResponseFuture<T> getSerial(String url,
			HttpParams param,
			ResponseParser<T> parser,
			HttpHandler<T> handler){
		return get(url,param, parser, null,handler,true,null);
	}

	public <T> ResponseFuture<T> getSerial(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler){
		return get(url,param,parser, responseClazz,handler,true,null);
	}

	/**
	 * 异步get请求（并行）
	 * @param url
	 * @return
	 */
	public ResponseFuture<String> get(String url,
			HttpParams param){
		return get(url,param, null, String.class,null,false,null);
	}

	public <T> ResponseFuture<T> get(String url,
			HttpParams param,
			HttpHandler<T> handler){
		return get(url,param,  null,null,handler,false,null);
	}

	public <T> ResponseFuture<T> get(String url,
			HttpHandler<T> handler,
			TaskControlCenter tcc){
		return get(url,null,null, null,handler,false,tcc);
	}

	public <T> ResponseFuture<T> get(String url,
			HttpParams params,
			HttpHandler<T> handler,   
			TaskControlCenter tcc){
		return get(url,params,null, null,handler,false,tcc);
	}

	public <T> ResponseFuture<T> get(String url,
			HttpParams param,
			ResponseParser<T> parser,
			HttpHandler<T> handler){
		return get(url,param, parser, null,handler,false,null);
	}

	public <T> ResponseFuture<T> get(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler){
		return get(url,param,parser, responseClazz,handler,false,null);
	}

	public <T> ResponseFuture<T> get(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler,
			boolean isSerial,
			TaskControlCenter controlCenter){
		return get(url,null, param, parser,responseClazz,handler,isSerial,controlCenter);
	}

	public <T> ResponseFuture<T> get(String url,
			String downPath,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler,
			boolean isSerial,
			TaskControlCenter controlCenter){
		if(isClosed())
			throw new IllegalStateException("Http has been Closed");

		if(responseClazz == null){
			if(handler == null){
				throw new IllegalArgumentException(TAG+"get - handler and responseClazz can not both be null");
			}
			@SuppressWarnings("unchecked")
			Class<T> genericResponseClazz = (Class<T>)ClassUtils.loadGenericSuperClass(handler.getClass());
			if(genericResponseClazz != null)
				responseClazz = genericResponseClazz;
			else{
				throw new IllegalArgumentException(TAG+"get - Unknow response class,please set the reponse class " +
						"or extends abstract handler(like AbstractResponseHandler、Downloadhandler)");
			}
		}
		Executor executor = THREAD_POOL_EXECUTOR;
		if(isSerial)
			executor = SERIAL_EXECUTOR;

		HttpRequestSession session = createRequestSession(connector,filterChain,createResponseHandler(handler));
		HttpAsyncTask<T> task = new HttpAsyncTask<T>(session,controlCenter);
		RequestEntity requestEntity = session.generateGetEntity(url,
				downPath,
				param,
				session,
				parser,
				responseClazz);

		ResponseFuture<T> future = new ResponseFuture<T>(task,requestEntity.key());
		task.requestOnExecutor(executor,requestEntity);

		return future;
	}

	/**
	 * 同步post请求
	 * @param url
	 * @return
	 */
	public <T> T postSync(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz) throws HttpRequestExecutionException {
		if(isClosed())
			throw new IllegalStateException("Http has been Closed");
		HttpRequestSession session = createRequestSession(connector,filterChain,null);
		HttpSyncHandler<T> requesthandler = new HttpSyncHandler<T>(session);
		if(responseClazz == null){
			throw new IllegalArgumentException("Unknow response class,please set the reponse class ");
		}
		RequestEntity requestEntity = session.generatePostEntity(url,
				null,
				param,
				session,
				parser,
				responseClazz);
		return requesthandler.request(requestEntity);
	}

	/**
	 * 异步post请求(串行)
	 * @param url
	 * @return
	 */
	public <T> ResponseFuture<T> postSerial(String url,
			HttpParams param,
			HttpHandler<T> handler){
		return post(url,param, null, null,handler,true);
	}

	public <T> ResponseFuture<T> postSerial(String url,
			HttpParams param,
			ResponseParser<T> parser,
			HttpHandler<T> handler){
		return post(url,param,parser, null,handler,true);
	}

	public <T> ResponseFuture<T> postSerial(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler){
		return post(url,param, parser, responseClazz,handler,true);
	}

	/**
	 * 异步post请求(并行)
	 * @param url
	 * @return
	 */
	public ResponseFuture<String> post(String url,
			HttpParams param){
		return post(url,param, null, String.class,null,false);
	}

	public <T> ResponseFuture<T> post(String url,
			HttpParams param,
			HttpHandler<T> handler){
		return post(url,param, null, null,handler,false);
	}

	public <T> ResponseFuture<T> post(String url,
			HttpParams param,
			ResponseParser<T> parser,
			HttpHandler<T> handler){
		return post(url,param,parser, null,handler,false);
	}

	public <T> ResponseFuture<T> post(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler){
		return post(url,param, parser, responseClazz,handler,false);
	}

	public <T> ResponseFuture<T> post(String url,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler,
			boolean isSerial){
		return post(url,null, param, parser,responseClazz,handler,isSerial,null);
	}

	public <T> ResponseFuture<T> post(String url,
			String downPath,
			HttpParams param,
			ResponseParser<T> parser,
			Class<?> responseClazz,
			HttpHandler<T> handler,
			boolean isSerial,
			TaskControlCenter controlCenter){
		if(isClosed())
			throw new IllegalStateException("Http has been Closed");
		if(responseClazz == null){
			@SuppressWarnings("unchecked")
			Class<T> genericResponseClazz = (Class<T>)ClassUtils.loadGenericSuperClass(handler.getClass());
			if(genericResponseClazz != null)
				responseClazz = genericResponseClazz;
			else{
				throw new IllegalArgumentException("Unknow response class,please set the reponse class " +
						"or extends abstract handler(like AbstractResponseHandler、Downloadhandler)");
			}
		}
		Executor executor = THREAD_POOL_EXECUTOR;
		if(isSerial)
			executor = SERIAL_EXECUTOR;

		HttpRequestSession session = createRequestSession(connector,filterChain,createResponseHandler(handler));
		HttpAsyncTask<T> task = new HttpAsyncTask<T>(session,controlCenter);
		RequestEntity requestEntity = session.generatePostEntity(url,
				downPath,
				param,
				session,
				parser,
				responseClazz);

		ResponseFuture<T> future = new ResponseFuture<T>(task,requestEntity.key());
		task.requestOnExecutor(executor, requestEntity);

		return future;
	}

	/**
	 * 上传下载请求
	 * @param url
	 * @return
	 */
	public <T> ResponseFuture<T> upload(String url,
			HttpParams param,
			UploadHandler<T> handler) {
		return post(url, null, param, null,null,handler,false,null);
	}

	public ResponseFuture<File> download(String url,
			String path,
			HttpParams param,
			DownloadHandler<File> handler){
		return get(url, path, param, null, File.class,handler,false,null);
	}

	public ResponseFuture<File> download(String url,
			String path,
			HttpParams param,
			ResponseParser<File> parser,
			DownloadHandler<File> handler){
		return get(url, path, param, parser,File.class,handler,false,null);
	}

	public File downloadSync(String url,
			String path,
			HttpParams param,
			ResponseParser<File> parser) throws HttpRequestExecutionException{
		if(isClosed())
			throw new IllegalStateException("Http has been Closed");

		HttpRequestSession session = createRequestSession(connector,filterChain,null);
		HttpSyncHandler<File> requesthandler = new HttpSyncHandler<File>(session);
		RequestEntity requestEntity = session.generateGetEntity(url,
				null,
				param,
				session,
				parser,
				File.class);
		return requesthandler.request(requestEntity);
	}

	protected <T> HttpRequestSession createRequestSession(HttpConnector connector,
			NetIOFilterChain filterChain,HttpHandler<T> handler) {
		return new HttpRequestSession(connector, filterChain, handler);
	}

	protected <T> HttpHandler<T> createResponseHandler(HttpHandler<T> handler){
		return new CurrentThreadResponseHandlerDecorator<T>(handler);
	}

	protected HttpConnector createHttpConnector(){
		return new HttpClientConnector();
	}

	protected NetIOFilterChain createFilterChain() {
		return new BasicNetIOFilterChain();
	}

	protected HttpResponseParseFilter createReponseParseFilter(){
		return new HttpResponseParseFilter(defaultObjectParser,
				defaultFileParser,
				defaultBitmapParser,
				defaultResponseContentCharset);
	}

	protected HttpResponseDiskCacheFilter createResponseCacheFilter() {
		return new HttpResponseDiskCacheFilter(defaultCacheFile,
				defaultCacheBitmap,
				defaultCacheOtherObject,
				defaultDiskMaxCacheSize,
				defaultContinueOnCacheHit,
				defaultDiskCacheExpired,
				defaultCacheDirectoryPath + File.separator + TAG);
	}

	protected ThreadPoolExecutor createThreadPoolExecutor() {
		return new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
				TimeUnit.SECONDS, sPoolWorkQueue, defaultThreadFactory,
				new ThreadPoolExecutor.DiscardOldestPolicy());
	}
}
