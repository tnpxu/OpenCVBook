package com.example.tnpxu.opencvbook;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.example.tnpxu.opencvbook.api.SendingPhotoApi;
import com.example.tnpxu.opencvbook.api.ServiceGenerator;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import org.opencv.core.Mat;

import java.io.File;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

public final class LabActivity extends ActionBarActivity {
    
    public static final String PHOTO_FILE_EXTENSION = ".jpg";
    public static final String PHOTO_MIME_TYPE = "image/jpg";
    
    public static final String EXTRA_PHOTO_URI =
            "com.nummist.secondsight.LabActivity.extra.PHOTO_URI";
    public static final String EXTRA_PHOTO_DATA_PATH =
            "com.nummist.secondsight.LabActivity.extra.PHOTO_DATA_PATH";
    
    private Uri mUri;
    private String mDataPath;

    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        final Intent intent = getIntent();
        mUri = intent.getParcelableExtra(EXTRA_PHOTO_URI);
        mDataPath = intent.getStringExtra(EXTRA_PHOTO_DATA_PATH);
        
        final ImageView imageView = new ImageView(this);
        imageView.setImageURI(mUri);
        
        setContentView(imageView);

        autoSendPhoto();
    }
    
    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_lab, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_delete:
            deletePhoto();
            return true;
            case R.id.menu_send:
                sendPhoto();
                return true;
//        case R.id.menu_edit:
//            editPhoto();
//            return true;
//        case R.id.menu_share:
//            sharePhoto();
//            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    /*
     * Show a confirmation dialog. On confirmation ("Delete"), the
     * photo is deleted and the activity finishes.
     */
    private void deletePhoto() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(
                LabActivity.this);
        alert.setTitle(R.string.photo_delete_prompt_title);
        alert.setMessage(R.string.photo_delete_prompt_message);
        alert.setCancelable(false);
        alert.setPositiveButton(R.string.delete,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog,
                            final int which) {
                        getContentResolver().delete(
                                Images.Media.EXTERNAL_CONTENT_URI,
                                MediaStore.MediaColumns.DATA + "=?",
                                new String[] { mDataPath });
                        CameraActivity.isChecking = false;
                        finish();
                    }
                });
        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();
    }

    private void sendPhoto() {
        final File photo = new File(mDataPath);

        final AlertDialog.Builder alert = new AlertDialog.Builder(
                LabActivity.this);
        alert.setTitle("Sending Photo");
        alert.setMessage("This photo will sending to your server");
        alert.setCancelable(false);
        alert.setPositiveButton("Sending",
                new DialogInterface.OnClickListener() {

                    ProgressDialog pDialog;

                    @Override
                    public void onClick(final DialogInterface dialog,
                                        final int which) {

                        pDialog = ProgressDialog.show(LabActivity.this, "Sending", "Please wait");

                        String description = "Hello, Team alpha this is Drone eiei";
                        SendingPhotoApi service = ServiceGenerator.createService(SendingPhotoApi.class);
                        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), photo);

                        Call<String> call = service.upload(requestBody, description);
                        call.enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(Response<String> response, Retrofit retrofit) {
                                Log.v("Upload", "success");
                                pDialog.dismiss();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                Log.e("Upload", t.getMessage());
                                pDialog.dismiss();
                            }
                        });

                    }
                });
        alert.setNegativeButton(android.R.string.cancel, null);
        alert.show();

    }

    private void autoSendPhoto() {
        final File photo = new File(mDataPath);

        final ProgressDialog pDialog;
        pDialog = ProgressDialog.show(LabActivity.this, "Sending", "Please wait");

        String description = "Hello, Team alpha this is Drone eiei";
        SendingPhotoApi service = ServiceGenerator.createService(SendingPhotoApi.class);
        RequestBody requestBody = RequestBody.create(MediaType.parse("multipart/form-data"), photo);

        Call<String> call = service.upload(requestBody, description);
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Response<String> response, Retrofit retrofit) {
                Log.v("Upload", "success");
                pDialog.dismiss();
                CameraActivity.isChecking = false;
                finish();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("Upload", t.getMessage());
                pDialog.dismiss();
                CameraActivity.isChecking = false;
                finish();
            }
        });
    }
    
    /*
     * Show a chooser so that the user may pick an app for editing
     * the photo.
     */
    private void editPhoto() {
        final Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(mUri, PHOTO_MIME_TYPE);
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_edit_chooser_title)));
    }
    
    /*
     * Show a chooser so that the user may pick an app for sending
     * the photo.
     */
    private void sharePhoto() {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(PHOTO_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, mUri);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.photo_send_extra_subject));
        intent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.photo_send_extra_text));
        startActivity(Intent.createChooser(intent,
                getString(R.string.photo_send_chooser_title)));
    }
}
