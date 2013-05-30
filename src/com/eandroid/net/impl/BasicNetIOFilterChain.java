/**
 * Copyright (c) 2013,2014 Kain Lu. All rights reserved.
 *
 * @author Kain
 * @date 2013-4-21
 * @version 0.1
 */
package com.eandroid.net.impl;

import java.util.Collection;
import java.util.Iterator;

import com.eandroid.net.NetIOFilter;
import com.eandroid.net.NetIOFilterChain;
import com.eandroid.net.Session;
import com.eandroid.net.NetIOFilter.NextFilterSelector;
import com.eandroid.net.http.SessionClosedException;
import com.eandroid.net.impl.filter.HeadFilter;
import com.eandroid.net.impl.filter.TailFilter;

public class BasicNetIOFilterChain implements NetIOFilterChain{

	protected Entry head;
	protected Entry tail;

	public BasicNetIOFilterChain(){
		head = new Entry(null, null, new HeadFilter());
		tail = new Entry(null,head,new TailFilter());
		head.nextEntry = tail;
	}

	@Override
	public void addFilter(NetIOFilter filter) {
		if(filter ==null)
			return;
		Entry entry = new Entry(tail, tail.prevEntry, filter);
		tail.prevEntry.nextEntry = entry;
		tail.prevEntry = entry;
	}

	@Override
	public void removeFilter(NetIOFilter filter) {
		Entry entry = head;
		while(entry != null){
			if(entry.filter == filter){
				removeEntry(entry);
			}
			entry = entry.nextEntry;
		}
	}

	private void removeEntry(Entry entry){
		if(entry == tail){
			//			tail = entry.prevEntry;
			//			tail.nextEntry = null;
			return;
		}else if(entry == head){
			//			head = entry.nextEntry;
			//			head.prevEntry = null;
			return;
		}else{
			entry.prevEntry.nextEntry = entry.nextEntry;
			entry.nextEntry.prevEntry = entry.prevEntry;
		}
	}

	@Override
	public void addAllFilters(Collection<? extends NetIOFilter> collections) {
		if(collections == null || collections.isEmpty())
			return;
		Iterator<? extends NetIOFilter> it = collections.iterator();
		while(it.hasNext()){
			addFilter(it.next());
		}
	}


	@Override
	public void sessionCreated(Session session){
		if(session == null || head == null)
			return;
		Entry nextEntry = head;
		callNextFilterSessionCreated(nextEntry,session);
	}

	@Override
	public void read(Session session,Object message){
		if(session == null || message == null || head == null)
			return;
		Entry nextEntry = head;
		callNextFilterRead(nextEntry,session, message);
	}

	@Override
	public void write(Session session, Object message){
		if(session == null || message == null || tail == null)
			return;
		Entry nextEntry = tail;
		callNextFilterWrite(nextEntry,session, message);
	}

	@Override
	public void catchException(Session session,Exception exception){
		if(session == null || exception == null)
			return;
		Entry nextEntry = head;
		callNextFilterCatchException(nextEntry,session, exception);
	}



	private void callNextFilterSessionCreated(Entry nextEntry,Session session){
		if(session.isClosed())
			throw new SessionClosedException("session has been closed");
		Entry entry = nextEntry;
		NextFilterSelector nextFilter = entry.nextFilter;
		entry.filter.onSessionCreated(nextFilter, session);
	}

	private void callNextFilterRead(Entry nextEntry,Session session,Object message){
		if(session.isClosed())
			throw new SessionClosedException("session has been closed");
		Entry entry = nextEntry;
		NextFilterSelector nextFilter = entry.nextFilter;
		entry.filter.onRead(nextFilter, session, message);
	}

	private void callNextFilterWrite(Entry nextEntry,Session session,Object message){
		if(session.isClosed())
			throw new SessionClosedException("session has been closed");
		Entry entry = nextEntry;
		NextFilterSelector nextFilter = entry.nextFilter;
		entry.filter.onWrite(nextFilter, session, message);
	}

	private void callNextFilterCatchException(Entry nextEntry,Session session,Exception exception){
		Entry entry = nextEntry;
		NextFilterSelector nextFilter = entry.nextFilter;
		entry.filter.onCatchException(nextFilter, session, exception);
	}

	protected class Entry{
		private Entry nextEntry;
		private Entry prevEntry;
		private NetIOFilter filter;
		private NextFilterSelector nextFilter;

		public Entry getNextEntry() {
			return nextEntry;
		}

		public void setNextEntry(Entry nextEntry) {
			this.nextEntry = nextEntry;
		}

		public Entry getPrevEntry() {
			return prevEntry;
		}

		public void setPrevEntry(Entry prevEntry) {
			this.prevEntry = prevEntry;
		}

		public NetIOFilter getFilter() {
			return filter;
		}

		public void setFilter(NetIOFilter filter) {
			this.filter = filter;
		}

		public NextFilterSelector getNextFilter() {
			return nextFilter;
		}

		public void setNextFilter(NextFilterSelector nextFilter) {
			this.nextFilter = nextFilter;
		}

		public Entry(Entry nextEntry,Entry prevEntry,NetIOFilter filter){
			this.nextEntry = nextEntry;
			this.prevEntry = prevEntry;
			this.filter = filter;
			nextFilter = new NextFilterSelector() {
				@Override
				public void sessionCreated(Session session){
					callNextFilterSessionCreated(Entry.this.nextEntry, session);
				}

				@Override
				public void read(Session session, Object message){
					callNextFilterRead(Entry.this.nextEntry, session, message);
				}

				@Override
				public void write(Session session,Object message){
					callNextFilterWrite(Entry.this.prevEntry, session, message);
				}

				@Override
				public void catchException(Session session,
						Exception exception) {
					callNextFilterCatchException(Entry.this.nextEntry, session, exception);
				}

			};
		}
	}

}
