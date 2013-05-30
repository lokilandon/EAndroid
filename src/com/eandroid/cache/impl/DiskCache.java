/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 1.0.0
 */
package com.eandroid.cache.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import com.eandroid.cache.Cache;
import com.eandroid.cache.util.DiskLruCache;
import com.eandroid.content.EContext;
import com.eandroid.util.EALog;
import com.eandroid.util.EncoderUtils;
import com.eandroid.util.FileUtils;

public class DiskCache<K,V> implements Cache<K, V> {

	private static final String TAG = "DiskCache";
	private static final int DISK_CACHE_INDEX = 0;

	public static final int MIN_DISK_CACHE_SIZE = 1024 * 1024 * 1; // 5MB

	private DiskLruCache mDiskLruCache;
	private final DiskCacheParams mCacheParams;

	private final Object cacheLock = new Object();

	private AtomicBoolean closed = new AtomicBoolean(false);
	private AtomicBoolean inited = new AtomicBoolean(false);
	/**
	 * Create a new BitmapCacheLoader object using the specified parameters. This should not be
	 * called directly by other classes, instead use
	 * {@link DiskCache#getInstance()} to fetch an BitmapCacheLoader
	 * instance.
	 *
	 * @param cacheParams The cache parameters to use to initialize the cache
	 * @throws NotEnoughSpaceException 
	 */
	public DiskCache(DiskCacheParams cacheParams) throws NotEnoughSpaceException {
		this.mCacheParams = cacheParams;
		if(mCacheParams.initDiskCacheOnCreate)
			initCache();
	}

	/**
	 * Initializes the disk cache.  Note that this includes disk access so this should not be
	 * executed on the main/UI thread. By default an ImageCache does not initialize the disk
	 * cache when it is created, instead you should call initDiskCache() to initialize it on a
	 * background thread.
	 */
	public void initCache() throws NotEnoughSpaceException{
		if (mDiskLruCache != null || isClose()) {
			return;
		}

		File diskCacheDir = mCacheParams.diskCacheDir;
		if (diskCacheDir != null) {
			synchronized (cacheLock) {
				if (mDiskLruCache != null || isClose()) {
					return;
				}
				
				if (!diskCacheDir.exists()) {
					diskCacheDir.mkdirs();
				}
				long diskCacheSize = mCacheParams.diskCacheSize;
				long usableSpace = FileUtils.getUsableSpace(diskCacheDir);

				if(usableSpace < MIN_DISK_CACHE_SIZE){
					throw new NotEnoughSpaceException(TAG + " initCache - Disk cache init failed,not enough space");
				}
				if (usableSpace < diskCacheSize) {
					EALog.d(TAG, "initCache - Disk cache size is not greater than setting,reset disk cache size to:"+usableSpace);
					diskCacheSize = usableSpace;
				}

				try {
					mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, diskCacheSize);
					inited.set(true);
					EALog.d(TAG, "Disk cache initialized");
				} catch (final IOException e) {
					EALog.e(TAG, "initCache - " + e);
				}
			}
		}else {
			throw new IllegalArgumentException(TAG + " initCache - Disk cache init failed,disk cache dir must not be null");
		}
	}

	/**
	 * Get from disk cache.
	 *
	 * @param data Unique identifier for which item to get
	 * @return The bitmap if found in cache, null otherwise
	 */
	public V load(K k) {
		return load(k,-1);
	}

	@SuppressWarnings("unchecked")
	public V load(K k,long expiredTime) {
		if(k == null)
			return null;

		if(!checkCacheState(true)){
			return null;
		}

		final String key = EncoderUtils.MD5HEX(String.valueOf(k));
		V v = null;
		InputStream inputStream = null;

		DiskLruCache.Snapshot snapshot = null;
		synchronized (cacheLock) {
			if(!checkCacheState(false)){
				return null;
			}
			try {
				snapshot = mDiskLruCache.get(key);
			} catch (IOException e) {
				EALog.e(TAG, "getBitmapFromDiskCache - " + e);
			}
		}
		if (snapshot != null) {
			try {
				long curTime = System.currentTimeMillis();
				long cacheTime = snapshot.getCacheTimestamp();
				inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
				if(expiredTime < 0 || (curTime - cacheTime) <= expiredTime){
					EALog.d(TAG, "Disk cache hit");
					if (inputStream == null)
						return v;
					if(mCacheParams.resolver == null){
						v = (V)inputStream;
						inputStream = null;
					}else  {
						v = (V) mCacheParams.resolver.toObject(inputStream);
					}
				}else{
					remove(k);
				}
			} finally {
				try {
					if (inputStream != null) {
						inputStream.close();
					}
				} catch (IOException e) {}
			}
		}
		return v;
	}

	@Override
	public V put(K data, V value) {
		if (data == null || value == null) {
			return null;
		}

		if(!checkCacheState(true)){
			return null;
		}

		final String key = EncoderUtils.MD5HEX(String.valueOf(data));
		InputStream in = null;
		BufferedOutputStream bos = null;
		synchronized (cacheLock) {
			try {
				if (!checkCacheState(false)) {
					return null;
				}
				final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
				if (editor != null) {
					try {
						OutputStream out = editor.newOutputStream(DISK_CACHE_INDEX);
						bos = new BufferedOutputStream(out,8*1024);
						if(value instanceof InputStream){
							in = (InputStream)value;
						}else if(mCacheParams.resolver != null){
							in = mCacheParams.resolver.toInputStream(value);
						}else {
							return null;
						}
						byte[] buf = new byte[8 * 1024];
						int readLength; 
						while ((readLength = in.read(buf)) != -1) {
							bos.write(buf,0,readLength);
						}
					} finally{
						editor.commit();
						if(value instanceof InputStream){
							in = null;
						}
					}
				} 
			} catch (final IOException e) {
				EALog.e(TAG, "addBitmapToCache - " + e);
			} catch (Exception e) {
				EALog.e(TAG, "addBitmapToCache - " + e);
			} finally {
				try {
					if (bos != null) {
						bos.close();
					}
				} catch (IOException e) {}
				try {
					if(in != null)
						in.close();
				} catch (Exception e2) {}
			}
			return null;
		}
	}

	@Override
	public boolean remove(K data) {
		if (data == null) {
			return false;
		}

		if(!checkCacheState(true)){
			return false;
		}

		boolean res = false;
		final String key = EncoderUtils.MD5HEX(String.valueOf(data));
		synchronized (cacheLock) {
			if(!checkCacheState(false)){
				return false;
			}
			try {
				res = mDiskLruCache.remove(key);
			} catch (final IOException e) {
				EALog.e(TAG, "remove - " + e);
			} 
			return res;
		}
	}

	@Override
	public CacheParams getCacheParams() {
		return mCacheParams;
	}

	/**
	 * Clears both the memory and disk cache associated with this ImageCache object. Note that
	 * this includes disk access so this should not be executed on the main/UI thread.
	 */
	public void clear() {
		if(checkCacheState(false))
			return;
		synchronized (cacheLock) {
			if(checkCacheState(false))
				return;

			try {
				try {
					mDiskLruCache.delete();
					EALog.d(TAG, "Disk cache cleared");
				} catch (IOException e) {
					EALog.e(TAG, "clearCache - " + e);
				}
				mDiskLruCache = null;
				initCache();
			} catch (Exception e) {
			} 
		}
	}

	/**
	 * Flushes the disk cache associated with this ImageCache object. Note that this includes
	 * disk access so this should not be executed on the main/UI thread.
	 */
	public void flush() {
		if (checkCacheState(false)) {
			return;
		}

		synchronized (cacheLock) {
			try {
				if (checkCacheState(false)) {
					return;
				}
				mDiskLruCache.flush();
				EALog.d(TAG, "Disk cache flushed");
			} catch (IOException e) {
				EALog.e(TAG, "flush - " + e);
			}
		}
	}



	/**
	 * Closes the disk cache associated with this ImageCache object. Note that this includes
	 * disk access so this should not be executed on the main/UI thread.
	 */
	public void close() {
		if (isClose()) {
			return;
		}

		synchronized (cacheLock) {
			try {
				if (isClose()) {
					return;
				}
				closed.set(true);
				if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
					mDiskLruCache.close();
					mDiskLruCache = null;
					EALog.d(TAG, "Disk cache closed");
				}
			} catch (IOException e) {
				EALog.e(TAG, "close - " + e);
			}
		}
	}

	public boolean isInited(){
		return inited.get();
	}

	public boolean isClose(){
		return closed.get();
	}

	private boolean checkCacheState(boolean autoInit){
		if(isClose())
			throw new IllegalStateException("Disk cache has been closed");
		if(mDiskLruCache == null){
			if(autoInit)
				try {
					initCache();
				} catch (NotEnoughSpaceException e) {
					return false;
				}
			else 
				return false;
		}
		return true;
	}

	/**
	 * A holder class that contains cache parameters.
	 */
	public static class DiskCacheParams implements CacheParams{

		// Default disk cache size in bytes
		public static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
		public static final boolean DEFAULT_INIT_DISK_CACHE_ON_CREATE = false;

		public long diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
		public File diskCacheDir;
		public boolean initDiskCacheOnCreate = DEFAULT_INIT_DISK_CACHE_ON_CREATE;
		public final CacheObjectResolver resolver;

		public DiskCacheParams(CacheObjectResolver resolver) {
			diskCacheDir = new File(EContext.getCacheFilePath() + File.separator + TAG);
			this.resolver = resolver;
		}

		public DiskCacheParams(CacheObjectResolver resolver,String diskCacheDirectoryPath) {
			diskCacheDir = new File(diskCacheDirectoryPath);
			this.resolver = resolver;
		}

		/**
		 * Create a set of image cache parameters that can be provided to
		 * @param diskCacheDirectoryName A unique subdirectory name that will be appended to the
		 *                               application cache directory. Usually "cache" or "images"
		 *                               is sufficient.
		 * @param resolver
		 * @param diskCacheSize
		 * @param initDiskCacheOnCreate
		 */
		public DiskCacheParams(String diskCacheDirectoryPath,CacheObjectResolver resolver,long diskCacheSize,boolean initDiskCacheOnCreate) {
			if(diskCacheSize < MIN_DISK_CACHE_SIZE){
				throw new IllegalArgumentException("Disk cache is too small,min size is "+MIN_DISK_CACHE_SIZE);
			}
			this.diskCacheSize = diskCacheSize;
			this.diskCacheDir = new File(diskCacheDirectoryPath);
			this.resolver = resolver;
			this.initDiskCacheOnCreate = initDiskCacheOnCreate;
		}

		public void setDiskCacheDirectoryPath(String diskCacheDirectoryPath) {
			this.diskCacheDir = new File(diskCacheDirectoryPath);
		}

		public void setDiskCacheSize(long diskCacheSize) {
			if(diskCacheSize < MIN_DISK_CACHE_SIZE){
				throw new IllegalArgumentException("Disk cache is too small,min size is "+MIN_DISK_CACHE_SIZE);
			}
			this.diskCacheSize = diskCacheSize;
		}

		@Override
		public long getCacheSize() {
			return diskCacheSize;
		}

	}

	public interface CacheObjectResolver {

		public InputStream toInputStream(Object object);
		public Object toObject(InputStream in);
	}

}
