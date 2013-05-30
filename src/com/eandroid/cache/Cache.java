/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-5-10
 * @version 1.0.0
 */
package com.eandroid.cache;


public interface Cache<K,V> {

    public V put(K key, V value);

    public V load(K key);
    
    public boolean remove(K key);

    public void clear();

    public void flush();
    
    public void close();
    
	public boolean isClose();
	
	public CacheParams getCacheParams();
	
	public interface CacheParams {
		public long getCacheSize();
	}
	
}
