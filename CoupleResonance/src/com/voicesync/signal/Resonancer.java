package com.voicesync.signal;

/*
 * calculates a resonance ratio between two sets of fft input in absScale(1) format
 * random input value is a reference 
 */
import java.util.Arrays;
import java.util.Comparator;


public class Resonancer {

	final int nMax=128; // max number of greatest values
	int sampleRate, nFFT;
	double scaler=1e9;
	Wave[]wm,wf;
	double res;
	Tuning tuning=new Tuning();
	private int nhit;
	private double maxObservedResonance=430.;

	class Wave { // amp, hz pair
		public Wave(double amp, double hz) { this.amp=amp; this.hz=hz; }
		double amp, hz;
	}
	class Tuning { // oct 0 tuning 
		//                    C        C#       D        D#       E        F        F#       G        G#       A        A#       B
		double[]temperate={523.251, 554.365, 587.330, 622.254, 659.255, 698.456, 739.989, 783.991, 830.609, 440.000, 466.164, 493.883}, 
				harrison5={511.134, 548.622, 570.749, 612.610, 657.541, 684.060, 734.232, 763.844, 819.868, 440.000, 457.746, 491.318},
				harrison4={527.354, 566.032, 588.861, 632.050, 678.407, 705.768, 757.532, 788.084, 845.885, 440.000, 472.271, 506.910},
				//           C    D    E    G    A    B
				solfeggio={ 396, 417, 528, 639, 741, 852};
		double[][]tuning={temperate, harrison5, harrison4, solfeggio }; 
		double[][]ratio;
		int[]ratioIx={0, -1,7,5,4}, ratioSolf={0, -1,3,2,1}; // ratios: eq, octave, fifth, fourth, third
		private double maxWeight=10;

		double weight[]={maxWeight, 1, 0.7, 0.3, 0.5}; // weight per ratio
		int tl=tuning.length, tl1=tuning.length-1, rl=ratioIx.length;
		double deltaRes=1e-6, deltaHit=1e-3;
		int nHit;

		
		Tuning() { 	calcRatio(); 	}
		private void calcRatio() {
			ratio=new double[tl][rl];
			for (int i=0; i<tl1; i++) // temp-like tuning
				for (int j=0; j<rl; j++) 
					ratio[i][j]=(ratioIx[j]==-1) ? 0.5 : tuning[i][0] / tuning[i][ratioIx[j]];
			for (int j=0; j<ratioSolf.length; j++) // solfeggio
				ratio[tl1][j]=(ratioSolf[j]==-1) ? 0.5 : tuning[tl1][0] / tuning[tl1][ratioSolf[j]];
		}
		public double resonate(double r) {
			double res=0;
			for (int i=0; i<tl; i++) {
				for (int j=0; j<rl; j++) {
					res += weight[i] * 1 / ( Math.abs( r - ratio[i][j]) + deltaRes );
				}
			}
			return res;
		}
		public void init() {nHit=0;}
		double harmHit(double r) { // sum of diff from ratios when in deltaHit range
			double hh=0; int nh=0;
			for (int i=0; i<tl; i++) {
				for (int j=0; j<rl; j++) {
					double diff=Math.abs(r - ratio[i][j]);
					if (diff==0) {  hh+=weight[i]; nh++;}// exact hit of ratio
					else 
						if ( diff <= deltaHit ) { hh += weight[i] * (1 - diff); nh++; } 
				}
			}
			nHit+=nh/tl;
			return hh/tl;
		}
		public double getMaxWeight() { 			return maxWeight;		}
		public int getnHit() { 	return nHit;	}
	}

	public void setParams(int sampleRate, int nFFT) { this.sampleRate = sampleRate;	this.nFFT = nFFT; }
	public double calcResonance(double[] fm, double[] ff) {
		wm=getMaxSet(fm);	scale2Oct0(wm);		
		wf=getMaxSet(ff);	scale2Oct0(wf);

		res = calcRes();   res /= ( fm.length * ff.length * tuning.getMaxWeight() / scaler );
		wm=wf=null;
		nhit=tuning.getnHit();
		return res;
	}
	public double calcDifference(double[] fm, double[] ff) { // assuming ff.len==fm.len
		double d=0;
		for (int i=0; i<fm.length; i++) d+=Math.abs(ff[i]-fm[i]);
		return 100. * (1. - d / fm.length);
	}
	double calcSame(double[] fm, double[] ff) {	// compare against itself (fm, ff), return avg of both 
		double rm, rf, divisor=fm.length * fm.length * tuning.getMaxWeight() / scaler; int nhm, nhf;
		wm=wf=getMaxSet(fm); scale2Oct0(wm);	rm = calcRes();  rm /= divisor; nhm=tuning.getnHit();
		wm=wf=getMaxSet(ff); scale2Oct0(wm);	rf = calcRes();  rf /= divisor; nhf=tuning.getnHit();
		wm=wf=null;
		nhit=(nhm+nhf)/2;
		return (rm+rf)/2;
	}
	double calcRes() { // between wm, wf
		double res=0; int iter=0; 
		tuning.init(); // call in each tuning calc cycle
		for (int i=0; i<wm.length; i++) {
			double hzm=wm[i].hz, hzf, ampm=wm[i].amp;
			for (int j=0; j<wf.length; j++, iter++) {
				hzf=wf[j].hz;
				res += tuning.harmHit( ratio(hzm, hzf) ) * ampm * wf[j].amp;
				if (iter % 1000==0) dispProgress();
			}
		}
		return res;
	}
	public double getResonance() {return res;}
	public int getnHit() { 	return nhit;	}
	Wave[]getMaxSet(double[]fs) {				// get nMax max values os 'fs'
		int lfs=fs.length, nm=Math.min(lfs, nMax);
		Wave[]w=new Wave[lfs]; for (int i=0; i<lfs; i++) w[i]=new Wave(fs[i], ix2Freq(i));
		Arrays.sort(w, new Comparator<Wave>(){
			public int compare(Wave w1, Wave w2) {
				return (w1.amp==w2.amp) ? 0 : ((w1.amp>w2.amp) ? -1:1); // descending order
			}
		});
		Wave[]wmx=new Wave[nm];	for (int i=0; i<nm; i++) wmx[i]=w[i]; // only need nm so copy nm values
		return wmx;
	}
	void scale2Oct0(Wave[]w) {	for (int i=0; i<w.length; i++) w[i].hz=MusicFreq.FreqInOctave(w[i].hz, 0);	}
	double ix2Freq(int i) { return (double) i * ((double)sampleRate / (double)nFFT / 2.); }
	private void dispProgress() {}
	private double ratio(double hzm, double hzf) { 
		if (hzm==0 | hzf==0) return 0;
		return (hzm>hzf) ? hzf/hzm : hzm/hzf;
	}
	public double[]randVect(int n) {
		double[]v=new double[n];
		for (int i=0; i<n; i++) v[i]=Math.random();
		return v;
	}
	double randResonance() { // calc the resonance  of a randon vector
		wm=getMaxSet(randVect(nFFT));	scale2Oct0(wm);
		wf=getMaxSet(randVect(nFFT));	scale2Oct0(wf);	

		res = calcRes();   res /= ( nFFT * nFFT * tuning.getMaxWeight() );
		wm=wf=null;
		return res;
	}
	public double scaleRes() {
		double r = res/maxObservedResonance;
		return (r>=1) ? 1:r;
	}
}
