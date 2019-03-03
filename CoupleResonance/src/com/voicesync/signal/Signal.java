package com.voicesync.signal;

import java.util.Arrays;


import com.voicesync.signal.Conf.sex;

public class Signal {
	public static Rec rec=new Rec();
	public static double[]yfft;
	static FFT fft=new FFT();
	static int ifftFrame=0;
	public static double[] sumfft;
	public static boolean hasData=false;
	private static double smv2=(2.*(double)Short.MAX_VALUE), smv=Short.MAX_VALUE;
	static double[]fftMale, fftFemale;

	public static void stopAll() { rec.stopRecording();	}
	public static void recFFT() { // fft from next chunk
		if (yfft==null) {
			yfft=new double[Conf.nFFT+3];
			sumfft=new double[Conf.nFFT];
		}
		for (int i=0; i<Conf.nFFT; i++) yfft[i]=(double)rec.samplesBuffer[i+ifftFrame]; // yfft=rec.SamplesBuffer
		fft.fft(yfft, Conf.nFFT2); 
		for (int i=0; i<FFT.Freq2Index(60, Conf.sampleRate, Conf.nFFT); i++) yfft[i]=0; // filter below 60hz
		fft.AbsScale(yfft, Conf.nFFT, 1);
		for (int i=0; i<Conf.nFFT; i++) sumfft[i]+=yfft[i];
		ifftFrame+=Conf.nFFT;
		if (ifftFrame+Conf.nFFT>=rec.lSampBuff) ifftFrame=0;
		switch (sx) { // required for graphing
		case male	: fftMale=sumfft; 		break;
		case female	: fftFemale=sumfft; 	break;
		}
	}
	public static void reset() {
		ifftFrame=0;
		if (yfft!=null) {Arrays.fill(yfft, 0); Arrays.fill(sumfft, 0);}
	}
	public static void resetListener() {rec.resetListener();}
	public static void addListener(AsyncListener listener) {rec.addListener(listener);	}
	public static void addListener( ){ // default listener -> calc fft from rec chuck
		addListener( new AsyncListener(){	@Override public void onDataReady() {	recFFT();	}});
	}
	public static double[] getFftMale() { 	return fftMale;	}
	public static double[] getFftFemale() {	return fftFemale;	}
	public static void saveMale() 	{fft.AbsScale(sumfft, Conf.nFFT, 1); fftMale=sumfft.clone();}
	public static void saveFemale() {fft.AbsScale(sumfft, Conf.nFFT, 1); fftFemale=sumfft.clone();}

	//---------------- MUSICAL NOTE TABLE 10 octaves x 12 notes
	public static int[][]NoteOctTable;
	public static int[]NoteTable, OctaveTable, noZeroOctaveList, noZeroOctaveTable;
	public static final int nOctaves=18, nNotes=12, octOffset=12;
	public static int firstNoZeroOctave, lastNoZeroOctave, octMax, noteMax;
	private static sex sx;
	
	public static double Index2Freq(int ix) { return FFT.Index2Freq(ix, Conf.sampleRate, Conf.nFFT); }
	public static double freqsInOct(int oct) {return MusicFreq.NoteOct2Freq(11, oct)-MusicFreq.NoteOct2Freq(00, oct);}
	public static int inRange(int i, int n) { return (i<0 || i>=n) ? 0:i;	}
	public static void doNoteOctTable() {

		NoteOctTable=new int[nOctaves][nNotes]; // 10 x 12 note weight 
		NoteTable	=new int[nNotes];				 //
		OctaveTable	=new int[nOctaves];		
		double[][]dOctTab=new double[nOctaves][nNotes];
		firstNoZeroOctave=lastNoZeroOctave=0;

		for (int i=0; i<Conf.nFFT; i++){ // create the table
			double freq=Index2Freq(i);
			int oct=MusicFreq.Freq2Oct(freq)+octOffset, note=MusicFreq.Freq2Note(freq);
			note=inRange(note, nNotes); oct=inRange(oct, nOctaves);
			double fio=freqsInOct(oct)*4; // compensate wider range of freqs in higher octaves
			dOctTab[oct][note]+=sumfft[i] / (fio==0?1:fio);
		}
		double max=-Double.MAX_VALUE, scale=100; // scale 0..100
		for (int i=0; i<nOctaves; i++)	for (int j=0; j<nNotes; j++) {
			if (dOctTab[i][j] > max) {
				max=dOctTab[i][j];
				octMax=i; noteMax=j;
			}
		} scale=100/(max==0?1:max);
		for (int i=0; i<nOctaves; i++)	for (int j=0; j<nNotes; j++) NoteOctTable[i][j]=(int)(scale * dOctTab[i][j]);
		for (int i=0; i<nOctaves; i++)	for (int j=0; j<nNotes; j++) {
			NoteTable[j]+=NoteOctTable[i][j]; OctaveTable[i]+=NoteOctTable[i][j];
		}
		for (int i=0; i<nOctaves; i++) if (OctaveTable[i]!=0) {firstNoZeroOctave=i; break;}
		for (int i=nOctaves-1; i>firstNoZeroOctave & OctaveTable[i]==0; i--) lastNoZeroOctave=i;
		if (firstNoZeroOctave < lastNoZeroOctave) { // create the octave list with no zero content
			noZeroOctaveList=new int[(lastNoZeroOctave - firstNoZeroOctave + 0)*nNotes];
			noZeroOctaveTable=new int[(lastNoZeroOctave - firstNoZeroOctave + 0)];
			for (int i=firstNoZeroOctave; i<lastNoZeroOctave; i++) {
				noZeroOctaveTable[i-firstNoZeroOctave]=OctaveTable[i];
				for (int j=0; j<nNotes; j++) noZeroOctaveList[(i-firstNoZeroOctave)*nNotes+j]=NoteOctTable[i][j];
			}
		}
	}
	public static String freqOct(int pos) { // oct hz in hz or khz.1 format
		double hz=MusicFreq.NoteOct2Freq(0, pos+firstNoZeroOctave-octOffset);
		if (hz<1000) return String.format("%3.0f",hz);
		else if (hz<10000) return String.format("%2.1fk",hz/1000);
		else return String.format("%5.0fk",hz/1000);
	}
	public static void setSex(sex _sx) {
		sx=_sx;
		
	}
}
