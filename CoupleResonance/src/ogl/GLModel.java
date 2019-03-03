package ogl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GLModel extends GLSurfaceView  {
	public GLModel(Context context) {    super(context);    initGL();	}
	public GLModel(Context context, AttributeSet attrs) {    super(context, attrs);    initGL(); }
	void initGL() {
		if (isInEditMode()) return;
		setTranslucent();
	}
	private void setTranslucent() {
		setEGLConfigChooser(8,8,8,8,16,0);		// make it transparent
		getHolder().setFormat(PixelFormat.TRANSLUCENT);     
		setZOrderOnTop(true);		
	}
}
