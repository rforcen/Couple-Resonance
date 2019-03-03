package ogl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

public class ibIndex {

	ShortBuffer fb;		int cnt, size;
	public ibIndex(int n) 		{ alloc(n); }
	public ibIndex(short[]v)		{ put(v);}
	public ibIndex(short[][]v) 	{
		int sz=0; for (short[]vv:v) sz+=vv.length; // calc sizeof v 
		alloc(sz); 
		for (short[]vv:v) add(vv);
	}
	public ibIndex(ArrayList<Integer>indices) { 
		alloc(indices.size());
		for (int i=0; i<indices.size(); i++) _add((short)(int)indices.get(i));
		reset();
	}
	public ibIndex() 		{ }

	public ShortBuffer alloc(int n)	{fb = (ByteBuffer.allocateDirect( n * 2 ).order(ByteOrder.nativeOrder())).asShortBuffer(); cnt=0; size=n; return fb;}
	public void add(short[]coords)		{for (short i=0; i<coords.length; i++) _add(coords[i]); reset(); }
	public void add(Integer[]coords)		{for (short i=0; i<coords.length; i++) _add(coords[i]); reset(); }
	public void put(short[]coords)		{fb=alloc(coords.length); fb.put(coords).position(0); }
	public void put(int i, int v) 	{fb.position(i);	fb.put((short)v); cnt=i;}
	public void add(int v) 			{fb.position(cnt++); fb.put((short)v); reset(); }
	public void add(int u, int v) 	{_add(u); _add(v); reset();}
	public void _add(int  v) 			{fb.position(cnt++); fb.put((short)v); } // no reset

	public void put(short i, short v)	{fb.position(i);	fb.put(v); cnt=i;}
	public void add(short v) 			{fb.position(cnt++); fb.put(v); reset(); }
	public void add(short u, short v)	{_add(u); _add(v); reset();}
	public void _add(short v) 			{fb.position(cnt++); fb.put(v); } // no reset

	public void reset() 				{fb.position(0); } // must reset to position(0) before use!!
	public ShortBuffer getBuffer() 		{return fb; }
	public int getSize() 				{return size; }
	public int getCnt() 				{return cnt; }	
}
