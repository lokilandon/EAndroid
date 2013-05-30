/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-11
 * @version 0.1
 */
package com.eandroid.cache.impl;

import java.lang.ref.SoftReference;
import java.util.HashSet;

import android.graphics.Bitmap;

import com.eandroid.cache.Cache;
import com.eandroid.cache.util.LruCache;
import com.eandroid.util.EALog;
import com.eandroid.util.SystemUtils;

public abstract class MemoryCache<K, V> implements Cache<K, V>{

	private LruCache<K, V> memoryCache;

	private static final String TAG = "MemoryCache";
	
	private MemoryCacheParams mCacheParams;

	protected HashSet<SoftReference<Bitmap>> mReusableBitmaps;

	/**
	 * Create a new BitmapMemoryCacheLoader object using the specified parameters. This should not be
	 * called directly by other classes, instead use
	 * {@link MemoryCache#getInstance()} to fetch an BitmapMemoryCacheLoader
	 * instance.
	 *
	 * @param cacheParams The cache parameters to use to initialize the cache
	 */
	public MemoryCache(MemoryCacheParams cacheParams) {
		init(cacheParams);
	}
	
	/**
	 * Initialize the cache, providing all parameters.
	 *
	 * @param cacheParams The cache parameters to initialize the cache
	 */
	private void init(MemoryCacheParams cacheParams) {
		mCacheParams = cacheParams;

		// Set up memory cache
		EALog.d(TAG, "Memory cache created (size = " + mCacheParams.memCacheSize + ")");

		// If we're running on Honeycomb or newer, then
		if (SystemUtils.hasHoneycomb()) {
			mReusableBitmaps = new HashSet<SoftReference<Bitmap>>();
		}

		memoryCache = new LruCache<K, V>(mCacheParams.memCacheSize) {
			/**
			 * Notify the removed entry that is no longer being cached
			 */
			@Override
			protected void entryRemoved(boolean evicted, K key,
					V oldValue, V newValue) {
				MemoryCache.this.entryRemoved(evicted, key, oldValue, newValue);
			}
			/**
			 * Measure item size in kilobytes rather than units which is more practical
			 * for a bitmap cache
			 */
			@Override
			protected int sizeOf(K key, V value) {
				return MemoryCache.this.sizeOf(key, value);
			}
		};
	}

	@Override
	public V put(K key, V value) {
		if(key == null || value == null)
			return null;
		
		return memoryCache.put(key, value);
	}

	@Override
	public V load(K key) {
		if(key == null)
			return null;
		V cache = memoryCache.get(key);
		
		if(cache != null)
			EALog.d(TAG, "Memory cache hit");
		
		return cache;
		
	}

	@Override
	public CacheParams getCacheParams() {
		return mCacheParams;
	}
	
	protected abstract void entryRemoved(boolean evicted, K key,
			V oldValue, V newValue);

	/**
	 * Measure item size in kilobytes rather than units which is more practical
	 * for a bitmap cache
	 */
	protected abstract int sizeOf(K key, V value);
	
	@Override
	public boolean remove(K key) {
		if(memoryCache == null || key == null)
			return false;
		
		memoryCache.remove(key);
		return true;
	}

	@Override
	public void clear() {
		if (memoryCache != null) {
			memoryCache.evictAll();
			EALog.d(TAG, "Memory cache cleared");
		}
	}

	@Override
	public void flush() {}

	@Override
	public void close(){
		clear();
	}

	@Override
	public boolean isClose(){return false;}

	/**
	 * A holder class that contains cache parameters.
	 */
	public static class MemoryCacheParams implements CacheParams{
		// Default memory cache size in kilobytes
		private static final int DEFAULT_MEM_CACHE_SIZE =  Math.round(0.2f * SystemUtils.getMaxMemory() / 1024); 
		
		public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;

		public MemoryCacheParams() {
		}
		/**
		 * Sets the memory cache size based on a percentage of the max available VM memory.
		 * Eg. setting percent to 0.2 would set the memory cache to one fifth of the available
		 * memory. Throws {@link IllegalArgumentException} if percent is < 0.05 or > .8.
		 * memCacheSize is stored in kilobytes instead of bytes as this will eventually be passed
		 * to construct a LruCache which takes an int in its constructor.
		 *
		 * This value should be chosen carefully based on a number of factors
		 * Refer to the corresponding Android Training class for more discussion:
		 * http://developer.android.com/training/displaying-bitmaps/
		 *
		 * @param percent Percent of available app memory to use to size memory cache
		 */
		public MemoryCacheParams(float cacheMemoryPercent) {
			if (cacheMemoryPercent < 0.05f || cacheMemoryPercent > 0.8f) {
				throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
						+ "between 0.05 and 0.8 (inclusive)");
			}
			memCacheSize = Math.round(cacheMemoryPercent * SystemUtils.getMaxMemory() / 1024);
		}

		@Override
		public long getCacheSize() {
			return memCacheSize;
		}
	}
}
