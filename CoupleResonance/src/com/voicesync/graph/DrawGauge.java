package com.voicesync.graph;

import com.voicesync.coupleresonance.R;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

public class DrawGauge extends View {
	Paint paint=new Paint();
	Drawable imgGauge, imgBall;
	int h,w, x,y;
	
	public DrawGauge(Context context, AttributeSet attrs) {
		super(context, attrs);
		imgGauge=getResources().getDrawable(R.drawable.gaugebar);
		imgGauge.setBounds(0, 0, imgGauge.getIntrinsicWidth(), imgGauge.getIntrinsicHeight());
		imgBall=getResources().getDrawable(R.drawable.heart);
	}
	public void getRect(Canvas cnv) {
		Rect rec=cnv.getClipBounds();
		w=rec.width(); 
		h=rec.height();
	}
	public void setBallPos(double r) {r=r>=1?1:r; x=(int)(r*((double)w-h/2)); postInvalidate(); }
	@Override  protected void onDraw(Canvas canvas) { 
//		if (x==0) return;
		getRect(canvas);
		imgBall.setBounds(x, 3, h+x-3, h-3);
		imgBall.draw(canvas);
	}
}
