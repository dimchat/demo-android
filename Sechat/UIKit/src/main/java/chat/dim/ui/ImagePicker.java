/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ImagePicker implements DialogInterface.OnClickListener {

    private final Activity activity;

    public float cropAspectX = 1;
    public float cropAspectY = 1;
    public float cropOutputX = 256;
    public float cropOutputY = 256;
    public boolean cropScale = true;
    public boolean cropScaleUpIfNeeded = true;
    public boolean cropNoFaceDetection = true;

    public ImagePicker(Activity activity) {
        super();
        this.activity = activity;
    }

    public void start() {
        if (!Permissions.canAccessCamera(activity)) {
            Permissions.requestCameraPermissions(activity);
            return;
        }
        CharSequence[] items = {
                Resources.getText(activity, R.string.camera),
                Resources.getText(activity, R.string.album),
        };
        Alert.alert(activity, items, this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 0: {
                openCamera();
                break;
            }

            case 1: {
                openAlbum();
                break;
            }
        }
    }

    private void openCamera() {
        System.out.println("open camera");
    }

    private void openAlbum() {
        System.out.println("open album");
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, DATA_TYPE);
        activity.startActivityForResult(intent, RequestCode.Album.value);
    }

    public Bitmap getBitmap(Intent data) {
        Bitmap bitmap;
        Bundle bundle = data.getExtras();
        if (bundle != null) {
            bitmap = bundle.getParcelable("data");
            if (bitmap != null) {
                return bitmap;
            }
        }
        Uri source = data.getData();
        if (source == null) {
            String action = data.getAction();
            if (action != null) {
                source = Uri.parse(action);
            }
        }
        if (source != null) {
            try {
                ContentResolver resolver = activity.getContentResolver();
                InputStream is = resolver.openInputStream(source);
                bitmap = BitmapFactory.decodeStream(is);
                if (bitmap != null) {
                    return bitmap;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Uri createTempFile(String tempDir) throws IOException {
        String filename = "crop-" + System.currentTimeMillis();
        File dir = new File(tempDir);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File file = File.createTempFile(filename, ".jpeg", dir);
        return Uri.fromFile(file);
    }

    public boolean cropPicture(Uri data, String tempDir) throws IOException {
        Uri output = createTempFile(tempDir);
        if (output == null) {
            return false;
        }

        Intent intent = new Intent(ACTION_CROP);
        intent.setDataAndType(data, DATA_TYPE);

        intent.putExtra("crop", true);
        intent.putExtra("aspectX", cropAspectX);
        intent.putExtra("aspectY", cropAspectY);
        intent.putExtra("outputX", cropOutputX);
        intent.putExtra("outputY", cropOutputY);
        intent.putExtra("scale", cropScale);
        intent.putExtra("scaleUpIfNeeded", cropScaleUpIfNeeded);
        intent.putExtra("noFaceDetection", cropNoFaceDetection);

        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, output);
        intent.putExtra("outputFormat", OUTPUT_FORMAT);

        if (intent.resolveActivity(activity.getPackageManager()) == null) {
            // CROP not support
            return false;
        }

        activity.startActivityForResult(intent, RequestCode.Crop.value);
        return true;
    }

    private static final String ACTION_CROP = "com.android.camera.action.CROP";
    private static final String DATA_TYPE = "image/*";
    private static final String OUTPUT_FORMAT = Bitmap.CompressFormat.JPEG.toString();

    public enum RequestCode {

        Album  (0x0201),
        Camera (0x0202),
        Crop   (0x0204);

        public final int value;

        RequestCode(int value) {
            this.value = value;
        }
    }
}
