/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-20
 * @version 0.1
 */
package com.eandroid.net;

import java.util.Collection;

import com.eandroid.net.NetIOFilter.NextFilterSelector;

public interface NetIOFilterChain extends NextFilterSelector{

	public void addFilter(NetIOFilter filter);
	public void removeFilter(NetIOFilter filter);
	public void addAllFilters(Collection<? extends NetIOFilter> collections);
	
}
