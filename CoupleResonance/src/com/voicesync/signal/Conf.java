package com.voicesync.signal;

public class Conf {
	static public int maxRate=44100, sampleRate=maxRate/4;
	static public int maxSecs=12;
	static public int nForm=8;
	static public int nFFT2=12, nFFT=1<<nFFT2;
	static public int hvLow=80, hvHigh=1100, hvDiff=hvHigh-hvLow; // human voice range
	public enum sex {male, female};
}
