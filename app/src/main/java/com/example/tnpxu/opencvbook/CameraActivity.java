package com.example.tnpxu.opencvbook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.hardware.Camera.AutoFocusCallback;

import com.example.tnpxu.opencvbook.Transform.PointsTransForm;
import com.example.tnpxu.opencvbook.filters.Filter;
import com.example.tnpxu.opencvbook.filters.NoneFilter;
import com.example.tnpxu.opencvbook.filters.convolution.StrokeEdgesFilter;
import com.example.tnpxu.opencvbook.filters.curve.CrossProcessCurveFilter;
import com.example.tnpxu.opencvbook.filters.curve.PortraCurveFilter;
import com.example.tnpxu.opencvbook.filters.curve.ProviaCurveFilter;
import com.example.tnpxu.opencvbook.filters.curve.VelviaCurveFilter;
import com.example.tnpxu.opencvbook.filters.mixer.RecolorCMVFilter;
import com.example.tnpxu.opencvbook.filters.mixer.RecolorRCFilter;
import com.example.tnpxu.opencvbook.filters.mixer.RecolorRGVFilter;

// Use the deprecated Camera class.
@SuppressWarnings("deprecation")
public final class CameraActivity extends ActionBarActivity
        implements CvCameraViewListener2 {

    //checking asynctask tranfrom flag
    public static boolean isChecking = false;
    //delaying checking to focus
    public static int delayChecking = 0;
    //frame arraylist
    public List<Mat> frameBuffer = new ArrayList<>();
    // A tag for log output.
    private static final String TAG =
            CameraActivity.class.getSimpleName();

    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // A key for storing the index of the active image size.
    private static final String STATE_IMAGE_SIZE_INDEX =
            "imageSizeIndex";

    // Keys for storing the indices of the active filters.
    private static final String STATE_CURVE_FILTER_INDEX =
            "curveFilterIndex";
    private static final String STATE_MIXER_FILTER_INDEX =
            "mixerFilterIndex";
    private static final String STATE_CONVOLUTION_FILTER_INDEX =
            "convolutionFilterIndex";

    // An ID for items in the image size submenu.
    private static final int MENU_GROUP_ID_SIZE = 2;

    // The filters.
    private Filter[] mCurveFilters;
    private Filter[] mMixerFilters;
    private Filter[] mConvolutionFilters;
    private PointsTransForm mTransform;

    // The indices of the active filters.
    private int mCurveFilterIndex;
    private int mMixerFilterIndex;
    private int mConvolutionFilterIndex;

    // The index of the active camera.
    private int mCameraIndex;

    // The index of the active image size.
    private int mImageSizeIndex;

    // Whether the active camera is front-facing.
    // If so, the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;

    // The number of cameras on the device.
    private int mNumCameras;

    // The image sizes supported by the active camera.
    private List<Size> mSupportedImageSizes;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // Whether the next camera frame should be saved as a photo.
    private boolean mIsPhotoPending;

    // A matrix that is used when saving photos.
    private Mat mBgr;

    // Whether an asynchronous menu action is in progress.
    // If so, menu interaction should be disabled.
    private boolean mIsMenuLocked;

    //count detect
    private int countDetect = 0;

    // The OpenCV loader callback.
    private BaseLoaderCallback mLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(final int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV loaded successfully");
                            mCameraView.enableView();
                            //mCameraView.enableFpsMeter();
                            mBgr = new Mat();
                            mCurveFilters = new Filter[]{
                                    new NoneFilter(),
                                    new PortraCurveFilter(),
                                    new ProviaCurveFilter(),
                                    new VelviaCurveFilter(),
                                    new CrossProcessCurveFilter()
                            };
                            mMixerFilters = new Filter[]{
                                    new NoneFilter(),
                                    new RecolorRCFilter(),
                                    new RecolorRGVFilter(),
                                    new RecolorCMVFilter()
                            };
                            mConvolutionFilters = new Filter[]{
                                    new NoneFilter(),
                                    new StrokeEdgesFilter()
                            };
                            mTransform = new PointsTransForm();
                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };

    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(
                    STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(
                    STATE_IMAGE_SIZE_INDEX, 0);
            mCurveFilterIndex = savedInstanceState.getInt(
                    STATE_CURVE_FILTER_INDEX, 0);
            mMixerFilterIndex = savedInstanceState.getInt(
                    STATE_MIXER_FILTER_INDEX, 0);
            mConvolutionFilterIndex = savedInstanceState.getInt(
                    STATE_CONVOLUTION_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mCurveFilterIndex = 0;
            mMixerFilterIndex = 0;
            mConvolutionFilterIndex = 0;
        }

        final Camera camera;
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.GINGERBREAD) {
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(mCameraIndex, cameraInfo);
            mIsCameraFrontFacing =
                    (cameraInfo.facing ==
                            CameraInfo.CAMERA_FACING_FRONT);
            mNumCameras = Camera.getNumberOfCameras();
            camera = Camera.open(mCameraIndex);
        } else { // pre-Gingerbread
            // Assume there is only 1 camera and it is rear-facing.
            mIsCameraFrontFacing = false;
            mNumCameras = 1;
            camera = Camera.open();
        }
        final Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes =
                parameters.getSupportedPreviewSizes();
        final Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = new JavaCameraView(this, mCameraIndex);
        mCameraView.setMaxFrameSize(size.width, size.height);
        mCameraView.setCvCameraViewListener(this);
        setContentView(mCameraView);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // Save the current image size index.
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX,
                mImageSizeIndex);

        // Save the current filter indices.
        savedInstanceState.putInt(STATE_CURVE_FILTER_INDEX,
                mCurveFilterIndex);
        savedInstanceState.putInt(STATE_MIXER_FILTER_INDEX,
                mMixerFilterIndex);
        savedInstanceState.putInt(STATE_CONVOLUTION_FILTER_INDEX,
                mConvolutionFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }

    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    public void onPause() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9,
                this, mLoaderCallback);
        mIsMenuLocked = false;
    }

    @Override
    public void onDestroy() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        if (mNumCameras < 2) {
            // Remove the option to switch cameras, since there is
            // only 1.
            //menu.removeItem(R.id.menu_next_camera);
        }
        int numSupportedImageSizes = mSupportedImageSizes.size();
        if (numSupportedImageSizes > 1) {
            final SubMenu sizeSubMenu = menu.addSubMenu(
                    R.string.menu_image_size);
            for (int i = 0; i < numSupportedImageSizes; i++) {
                final Size size = mSupportedImageSizes.get(i);
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE,
                        String.format("%dx%d", size.width,
                                size.height));
            }
        }
        return true;
    }

    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks (for recreate).
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mIsMenuLocked) {
            return true;
        }
        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            mImageSizeIndex = item.getItemId();
            recreate();

            return true;
        }
        switch (item.getItemId()) {
//        case R.id.menu_next_curve_filter:
//            mCurveFilterIndex++;
//            if (mCurveFilterIndex == mCurveFilters.length) {
//                mCurveFilterIndex = 0;
//            }
//            return true;
//        case R.id.menu_next_mixer_filter:
//            mMixerFilterIndex++;
//            if (mMixerFilterIndex == mMixerFilters.length) {
//                mMixerFilterIndex = 0;
//            }
//            return true;
//        case R.id.menu_next_convolution_filter:
//            mConvolutionFilterIndex++;
//            if (mConvolutionFilterIndex ==
//                    mConvolutionFilters.length) {
//                mConvolutionFilterIndex = 0;
//            }
//            return true;
//        case R.id.menu_next_camera:
//            mIsMenuLocked = true;
//
//            // With another camera index, recreate the activity.
//            mCameraIndex++;
//            if (mCameraIndex == mNumCameras) {
//                mCameraIndex = 0;
//            }
//            recreate();
//
//            return true;
//
//        case R.id.menu_take_photo:
//            mIsMenuLocked = true;
//
//            // Next frame, take the photo.
//            mIsPhotoPending = true;
//
//            return true;
//
//            case R.id.menu_transform:
//                mToTransforming = true;
//                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }


    @Override
    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

//        mTransform.checkTransform(rgba);

//        if (frameBuffer.size() <= 30) {
//            Mat matAdding = new Mat();
//            rgba.copyTo(matAdding);
//            frameBuffer.add(matAdding);
//        }


        if (!isChecking /**&& frameBuffer.size() == 30**/ ) {

            /***********************************/
            /******* Detect to Capture **********/
            /***********************************/

            AsyncTask<String, Void, String> asyncBackground = new AsyncTask<String, Void, String>() {

                private Mat checkMat = new Mat();

                @Override
                protected void onPreExecute() {
                    isChecking = true;
                    rgba.copyTo(checkMat);
                }

                @Override
                protected String doInBackground(String... params) {
//                    if (
//                            mTransform.checkTransform(frameBuffer.get(0)) &&
////                                    mTransform.checkTransform(frameBuffer.get(1)) &&
////                                    mTransform.checkTransform(frameBuffer.get(3)) &&
////                                    mTransform.checkTransform(frameBuffer.get(4)) &&
//                                    mTransform.checkTransform(frameBuffer.get(5)) &&
////                                    mTransform.checkTransform(frameBuffer.get(6)) &&
////                                    mTransform.checkTransform(frameBuffer.get(7)) &&
////                                    mTransform.checkTransform(frameBuffer.get(8)) &&
////                                    mTransform.checkTransform(frameBuffer.get(9)) &&
//                                    mTransform.checkTransform(frameBuffer.get(10)) &&
////                                    mTransform.checkTransform(frameBuffer.get(11)) &&
////                                    mTransform.checkTransform(frameBuffer.get(12)) &&
////                                    mTransform.checkTransform(frameBuffer.get(13)) &&
////                                    mTransform.checkTransform(frameBuffer.get(14)) &&
//                                    mTransform.checkTransform(frameBuffer.get(15)) &&
////                                    mTransform.checkTransform(frameBuffer.get(16)) &&
////                                    mTransform.checkTransform(frameBuffer.get(17)) &&
////                                    mTransform.checkTransform(frameBuffer.get(18)) &&
////                                    mTransform.checkTransform(frameBuffer.get(19)) &&
//                                    mTransform.checkTransform(frameBuffer.get(20)) &&
////                                    mTransform.checkTransform(frameBuffer.get(21)) &&
////                                    mTransform.checkTransform(frameBuffer.get(22)) &&
////                                    mTransform.checkTransform(frameBuffer.get(23)) &&
//                                    mTransform.checkTransform(frameBuffer.get(24)) &&
//                                    mTransform.checkTransform(frameBuffer.get(29))
//                            )
                    if(mTransform.checkTransform(checkMat)) {
                        countDetect++;
                        if (countDetect == 10) {
                            return "T";
                        } else {
                            return "F1";
                        }
                    }

                    return "F2";

                }

                @Override
                protected void onPostExecute(String res) {
                    if (res.equals("T")) {
                        takePhoto(checkMat);
                        countDetect = 0;
                        //waiting
//                        try {
//                            Thread.sleep(2000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                    } else if(res.equals("F1")) {
                        isChecking = false;
                    } else {
                        countDetect = 0;
                        isChecking = false;
                    }
//                    frameBuffer.clear();
                }

            };


            if (Build.VERSION.SDK_INT >= 11)
                asyncBackground.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            else
                asyncBackground.execute();


            /*************************************************************************/

        }

        //4points-transform
//        if (mTransform.checkTransform(rgba)) {
//            takePhoto(rgba);
//        }

        // Apply the active filters.
        mCurveFilters[mCurveFilterIndex].apply(rgba, rgba);
        mMixerFilters[mMixerFilterIndex].apply(rgba, rgba);
        mConvolutionFilters[mConvolutionFilterIndex].apply(rgba, rgba);

//        if (mIsPhotoPending) {
//            mIsPhotoPending = false;
//            takePhoto(rgba);
//        }

        if (mIsCameraFrontFacing) {
            // Mirror (horizontally flip) the preview.
            Core.flip(rgba, rgba, 1);
        }

        return rgba;
    }

    private void takePhoto(final Mat rgba) {


        // Determine the path and metadata for the photo.
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator +
                appName;
        final String photoPath = albumPath + File.separator +
                currentTimeMillis + LabActivity.PHOTO_FILE_EXTENSION;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(Images.Media.MIME_TYPE,
                LabActivity.PHOTO_MIME_TYPE);
        values.put(Images.Media.TITLE, appName);
        values.put(Images.Media.DESCRIPTION, appName);
        values.put(Images.Media.DATE_TAKEN, currentTimeMillis);

        // Ensure that the album directory exists.
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " +
                    albumPath);
            onTakePhotoFailed();
            return;
        }

        // Try to create the photo.
        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Highgui.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }
        Log.d(TAG, "Photo saved successfully to " + photoPath);

        // Try to insert the photo into the MediaStore.
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();

            // Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }

            onTakePhotoFailed();
            return;
        }

        // Open the photo in LabActivity.
        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH,
                photoPath);
        startActivity(intent);
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                startActivity(intent);
//            }
//        });
    }

    private void onTakePhotoFailed() {
        mIsMenuLocked = false;

        // Show an error message.
        final String errorMessage =
                getString(R.string.photo_error_message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(CameraActivity.this, errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
