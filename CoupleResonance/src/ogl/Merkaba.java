package ogl;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Color;
import android.opengl.GLSurfaceView;

public class Merkaba extends GLRenderer {
	Polyhedron tetra;
	float ang=0;
	
	public Merkaba(GLSurfaceView glSurface) {	super(glSurface);	}
	@Override public void postInit(GL10 gl) {
		ColorIndex colIx=new ColorIndex();
		int[]cols=new int[4]; for (int i=0; i<4; i++) cols[i]=colIx.getColor((float)(i)/4f) & 0x00ffffff;
		tetra=new Polyhedron().tetrahedron(cols);
		stopAnimation();
		setZoom(-9f);
	}
	@Override public void drawModel(GL10 gl) {
		enableTransparency(gl);	enableColorArray(gl); enableNormalArray(gl); // draw transparent model

		drawModelMerkaba(gl);
	}
	private void drawModelMerkaba(GL10 gl) {
		gl.glRotatef(ang+=3, 0, 1, 0);
		gl.glPushMatrix(); {
			gl.glRotatef(88, 1, 1, 0);
			tetra.drawSolidColor(gl);
			gl.glRotatef(90, 0, 0, 1);
			tetra.drawSolidColor(gl);
			disableColorArray(gl); // so we can draw a wire poly with different color
			setColor(gl, Color.WHITE);
			tetra.drawWire(gl);
		} gl.glPopMatrix();
	}
}