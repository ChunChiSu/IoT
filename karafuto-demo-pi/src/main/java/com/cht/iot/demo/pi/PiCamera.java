package com.cht.iot.demo.pi;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.FrameGrabber;
import au.edu.jcu.v4l4j.V4L4JConstants;
import au.edu.jcu.v4l4j.VideoDevice;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

public class PiCamera {
	static final Logger LOG = LoggerFactory.getLogger(PiCamera.class);

	public static BufferedImage capture(String dev, int w, int h, int q) throws Exception {
		final Future<BufferedImage> future = new Future<BufferedImage>();
		
		VideoDevice vd = new VideoDevice(dev);
		try {
			int input = 0; // TODO - InputInfo.getIndex()
			int std = V4L4JConstants.STANDARD_WEBCAM; // TODO - NG
			
			FrameGrabber grabber = vd.getJPEGFrameGrabber(w, h, input, std, q);
			try {			
				grabber.setCaptureCallback(new CaptureCallback() {
					@Override
					public void nextFrame(VideoFrame frame) {
						try {
							BufferedImage snapshot = frame.getBufferedImage(); //  ImageIO.read(new ByteArrayInputStream(frame.getBytes()))								
						
							future.set(snapshot);
							
						} catch (Exception e) {
							LOG.error(e.getMessage(), e);
						}
						
						frame.recycle();
					}
					
					@Override
					public void exceptionReceived(V4L4JException e) {
						LOG.error(e.getMessage(), e);
						
						future.set(null);
					}
				});
				
				grabber.startCapture();				
		
				return future.get();
			
			} finally {
				vd.releaseFrameGrabber();
			}
		} finally {
			vd.release();
		}		
	}
	
	public static BufferedImage rotate(BufferedImage snapshot, int w, int h, double angdeg) {
		double theta = Math.toRadians(angdeg);
		
		AffineTransform at = AffineTransform.getRotateInstance(theta, w / 2, h / 2);
		AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
		
		return op.filter(snapshot, null);
	}
	
	public static byte[] toBytes(BufferedImage snapshot) throws IOException {		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(snapshot, "jpg", baos);
		baos.flush();
		
		return baos.toByteArray();
	}
}
