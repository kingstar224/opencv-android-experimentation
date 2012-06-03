package org.opencv.samples.fd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.objdetect.CascadeClassifier;
import org.rhok.hh.hackingautism.mooddetector.MoodDetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.SurfaceHolder;

class FdView extends SampleCvViewBase {
    private static final String TAG = "Sample::FdView";
    private Mat                 mRgba;
    private Mat                 mGray;

    private CascadeClassifier   mCascade;

    public FdView(Context context) {
        super(context);

        try {
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (mCascade.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mCascade = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

            cascadeFile.delete();
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
        super.surfaceChanged(_holder, format, width, height);

        synchronized (this) {
            // initialize Mats before usage
            mGray = new Mat();
            mRgba = new Mat();
        }
    }

    @Override
    protected Bitmap processFrame(VideoCapture capture) {
        capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
        capture.retrieve(mGray, Highgui.CV_CAP_ANDROID_GREY_FRAME);

        if (mCascade != null) {
            int height = mGray.rows();
            int faceSize = Math.round(height * FdActivity.minFaceSize);
            MatOfRect faces = new MatOfRect();
            mCascade.detectMultiScale(mGray, faces, 1.1, 2, 2 // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    , new Size(faceSize, faceSize), new Size());

            for (Rect r : faces.toArray())
            {
//                Core.rectangle(mRgba, r.tl(), r.br(), new Scalar(0, 255, 0, 255), 3);
            	int y0, y1, x0, x1;
            	x0 = Math.min(r.x, mRgba.cols());
            	y0 = Math.min(r.y, mRgba.rows());
            	x1 = Math.min(r.x+r.width, mRgba.cols());
            	y1 = Math.min(r.y+r.height, mRgba.rows());
            	
//                mRgba = mRgba.submat(y0, y1, x0, x1);
				try {
					FileOutputStream out;
					out = getContext().openFileOutput("temp.png", Context.MODE_PRIVATE);
					Bitmap b = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
					Utils.matToBitmap(mRgba, b);
					b.compress(Bitmap.CompressFormat.PNG, 90, out);
					out.close();
					
					new MoodDetector().detectMood(new File(getContext().getFilesDir(), "temp.png"));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            
            }
        }

        Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.RGB_565/*.ARGB_8888*/);

        try {
        	Utils.matToBitmap(mRgba, bmp);
            return bmp;
        } catch(Exception e) {
        	Log.e("org.opencv.samples.puzzle15", "Utils.matToBitmap() throws an exception: " + e.getMessage());
            bmp.recycle();
            return null;
        }
    }

    @Override
    public void run() {
        super.run();

        synchronized (this) {
            // Explicitly deallocate Mats
            if (mRgba != null)
                mRgba.release();
            if (mGray != null)
                mGray.release();

            mRgba = null;
            mGray = null;
        }
    }
}
