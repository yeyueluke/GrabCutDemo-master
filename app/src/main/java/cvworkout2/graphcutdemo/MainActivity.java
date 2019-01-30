package cvworkout2.graphcutdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import android.graphics.Matrix;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_OPEN_IMAGE = 1;
    private static final int REQUEST_PERMISSION = 233;
    String mCurrentPhotoPath;
    Bitmap mBitmap;
    ImageView mImageView;
    int touchCount = 0;
    Point tl;
    Point br;
    boolean targetChose = false;
    ProgressDialog dlg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE,REQUEST_PERMISSION);
        setContentView(R.layout.activity_main);

        mImageView = (ImageView) findViewById(R.id.imgDisplay);
        dlg = new ProgressDialog(this);
        tl = new Point();
        br = new Point();
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    private boolean checkPermission(String permission,int requestCode){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    //showHint("Camera and SDCard access is required, please grant the permission in settings.");
                    finish();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    permission,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                            requestCode);
                }
                return false;
            }else return true;
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void setPic() {
        int targetW = mImageView.getWidth();
        int targetH = mImageView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        mBitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        mImageView.setImageBitmap(mBitmap);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_OPEN_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imgUri = data.getData();
                    String[] filePathColumn = { MediaStore.Images.Media.DATA };

                    Cursor cursor = getContentResolver().query(imgUri, filePathColumn,
                            null, null, null);
                    cursor.moveToFirst();

                    int colIndex = cursor.getColumnIndex(filePathColumn[0]);
                    mCurrentPhotoPath = cursor.getString(colIndex);
                    cursor.close();
                    setPic();
                }
                break;
        }
    }
    public float[] getPointerCoords(ImageView view, MotionEvent e)
    {
        final int index = e.getActionIndex();
        final float[] coords = new float[] { e.getX(index), e.getY(index) };
        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);
        matrix.postTranslate(view.getScrollX(), view.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_open_img:
                Intent getPictureIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getPictureIntent.setType("image/*");
                Intent pickPictureIntent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent chooserIntent = Intent.createChooser(getPictureIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] {
                        pickPictureIntent
                });
                startActivityForResult(chooserIntent, REQUEST_OPEN_IMAGE);
                return true;
            case R.id.action_choose_target:
                if (mCurrentPhotoPath != null)
                    targetChose = false;
                    mImageView.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                if (touchCount == 0) {
                                    tl.x = event.getX();
                                    tl.y = event.getY();
                                    touchCount++;
                                    float []record = getPointerCoords(mImageView,event);
                                    tl.x = record[0];
                                    tl.y = record[1];
                                }
                                else if (touchCount == 1) {
                                    br.x = event.getX();
                                    br.y = event.getY();
                                    Paint rectPaint = new Paint();
                                    rectPaint.setARGB(255, 255, 0, 0);
                                    rectPaint.setStyle(Paint.Style.STROKE);
                                    rectPaint.setStrokeWidth(3);
                                    Bitmap tmpBm = Bitmap.createBitmap(mBitmap.getWidth(),
                                            mBitmap.getHeight(), Bitmap.Config.RGB_565);
                                    Canvas tmpCanvas = new Canvas(tmpBm);

                                    float []record = getPointerCoords(mImageView,event);
                                    br.x = record[0];
                                    br.y = record[1];
                                    tmpCanvas.drawBitmap(mBitmap, 0, 0, null);
                                    tmpCanvas.drawRect(new RectF((float) tl.x, (float) tl.y, (float) br.x, (float) br.y),
                                            rectPaint);

                                    //tmpCanvas.drawRect(new RectF((float) 68, (float) 194, (float)737, (float) 378),
                                    //        rectPaint);
                                    mImageView.setImageDrawable(new BitmapDrawable(getResources(), tmpBm));

                                    targetChose = true;
                                    touchCount = 0;
                                    mImageView.setOnTouchListener(null);
                                }
                            }

                            return true;
                        }
                    });

                return true;
            case R.id.action_cut_image:
                if (mCurrentPhotoPath != null && targetChose) {
                    new ProcessImageTask().execute();
                    targetChose = false;
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class ProcessImageTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dlg.setMessage("Processing Image...");
            dlg.setCancelable(false);
            dlg.setIndeterminate(true);
            dlg.show();
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            Mat img = Imgcodecs.imread(mCurrentPhotoPath);
            Mat background = new Mat(img.size(), CvType.CV_8UC3,
                    new Scalar(255, 255, 255));
            Mat firstMask = new Mat();
            Mat bgModel = new Mat();
            Mat fgModel = new Mat();
            Mat mask;
            Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(Imgproc.GC_PR_FGD));
            Mat dst = new Mat();
           // tl = new Point(68, 194);
           // br = new Point(737,378);
            Rect rect = new Rect(tl, br);

            Imgproc.grabCut(img, firstMask, rect, bgModel, fgModel,
                    5, Imgproc.GC_INIT_WITH_RECT);
            Core.compare(firstMask, source, firstMask, Core.CMP_EQ);

            Mat foreground = new Mat(img.size(), CvType.CV_8UC3,
                    new Scalar(255, 255, 255));
            img.copyTo(foreground, firstMask);

            Scalar color = new Scalar(255, 0, 0, 255);
            Imgproc.rectangle(img, tl, br, color);

            Mat tmp = new Mat();
            Imgproc.resize(background, tmp, img.size());
            background = tmp;
            mask = new Mat(foreground.size(), CvType.CV_8UC1,
                    new Scalar(255, 255, 255));

            Imgproc.cvtColor(foreground, mask, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(mask, mask, 254, 255, Imgproc.THRESH_BINARY_INV);
            System.out.println();
            Mat vals = new Mat(1, 1, CvType.CV_8UC3, new Scalar(0.0));
            background.copyTo(dst);

            background.setTo(vals, mask);

            Core.add(background, foreground, dst, mask);

            firstMask.release();
            source.release();
            bgModel.release();
            fgModel.release();
            vals.release();

            Imgcodecs.imwrite(mCurrentPhotoPath + ".png", dst);

            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);

            Bitmap jpg = BitmapFactory
                    .decodeFile(mCurrentPhotoPath + ".png");

            mImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            mImageView.setAdjustViewBounds(true);
            mImageView.setPadding(2, 2, 2, 2);
            mImageView.setImageBitmap(jpg);
            mImageView.invalidate();

            dlg.dismiss();
        }
    }
}
