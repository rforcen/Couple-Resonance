package com.voicesync.signal;

public class MStimer { // a minute/sec timer class
	long starttime = 0, millis=0;
	public int seconds=0,minutes=0;
	public MStimer() { start(); }
	void update() {
		millis = System.currentTimeMillis() - starttime;
		seconds = (int) (millis / 1000);
		minutes = seconds / 60;
		seconds = seconds % 60;    		
	}
	public void start() { starttime = System.currentTimeMillis(); seconds=0; minutes=0; }
	public String getMMSS() {
		update(); 
		return String.format("%02d:%02d", minutes, seconds);
	}
	public String getSS() {
		update(); 
		return String.format("%02d", seconds);
	}
	public int getSec() { return seconds; 	}
	public int getMin() { return minutes; 	}
}