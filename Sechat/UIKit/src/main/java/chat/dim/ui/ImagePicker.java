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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

public class ImagePicker implements DialogInterface.OnClickListener {

    private final Activity activity;

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
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        activity.startActivityForResult(intent, RequestCode.Album.value);
    }

    public void cropPicture(Intent data) {
        if (data == null) {
            return;
        }
        cropPicture(data.getData());
    }
    public void cropPicture(Uri data) {
        if (data == null) {
            return;
        }
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(data, "image/*");

        intent.putExtra("crop", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 512);
        intent.putExtra("outputY", 512);
        intent.putExtra("scale", true);

        intent.putExtra("return-data", true);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, "");
        //intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        activity.startActivityForResult(intent, RequestCode.Crop.value);
    }

    public enum RequestCode {

        Album  (0x95270001),
        Camera (0x95270002),
        Crop   (0x95270004);

        public final int value;

        RequestCode(int value) {
            this.value = value;
        }
    }
}
