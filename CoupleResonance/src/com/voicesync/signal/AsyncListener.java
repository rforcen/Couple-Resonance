package com.voicesync.signal;

import java.util.EventListener;

public interface AsyncListener extends EventListener {
	void onDataReady();
}
