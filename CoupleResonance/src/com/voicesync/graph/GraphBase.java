package com.voicesync.graph;

import com.voicesync.signal.AsyncListener;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class GraphBase extends View {

	public GraphBase(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public void refresh() {  postInvalidate();	}
	public AsyncListener getListener( ){
		return new AsyncListener(){	@Override public void onDataReady() {	refresh();	}};
	}
}
