package com.eandroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.eandroid.ioc.AutowireHelper;

public class AutowireActivity extends Activity{
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		onActivityAutowired(savedInstanceState,onAutowire());
		super.onCreate(savedInstanceState);
	}

	/**
	 * 在activity启动时，自动装载时调用
	 * 在onCreate中调用，开始进入此生命周期之前，应先调用setContentView方法，或者通过Annotation对ContentView进行设置。
	 * 如果子类要重载此方法，必须调用super.onAutowire()方法来进行自动装载。
	 * @param savedInstanceState
	 * @return boolean true 使用了自动装载并成功装载  false 未使用自动装载，或者装载失败
	 */
	protected boolean onAutowire() {
		return AutowireHelper.autowireContentView(this) 
				| AutowireHelper.autowireField(this);
	}

	/**
	 * 在activity启动时，自动装载后调用
	 * onCreate中触发
	 * @param savedInstanceState 
	 * @param autowireResult 自动装载是否 使用并且成功了
	 */
	protected void onActivityAutowired(Bundle savedInstanceState,boolean autowireResult) {}

	public void setContentView(int layoutResID,boolean autowired) {
		super.setContentView(layoutResID);
		if(autowired)
			AutowireHelper.autowireField(this);
	}

	public void setContentView(View view, LayoutParams params,boolean autowired) {
		super.setContentView(view, params);
		if(autowired)
			AutowireHelper.autowireField(this);
	}

	public void setContentView(View view,boolean autowired) {
		super.setContentView(view);
		if(autowired)
			AutowireHelper.autowireField(this);
	}

}
