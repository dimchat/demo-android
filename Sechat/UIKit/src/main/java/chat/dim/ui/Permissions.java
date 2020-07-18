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

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class Permissions extends ActivityCompat {

    public enum RequestCode {

        ExternalStorage (1),
        Camera          (2);

        public final int value;

        RequestCode(int value) {
            this.value = value;
        }
    }

    private static final int PERMISSION_GRANTED = PackageManager.PERMISSION_GRANTED;

    //
    //  External Storage
    //

    private static final String READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String WRITE_EXTERNAL_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static String[] EXTERNAL_STORAGE_PERMISSIONS = {READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE};

    public static boolean canWriteExternalStorage(Activity activity) {
        return PERMISSION_GRANTED == checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE);
    }

    public static void requestExternalStoragePermissions(Activity activity) {
        requestPermissions(activity, EXTERNAL_STORAGE_PERMISSIONS, RequestCode.ExternalStorage.value);
    }

    //
    //  Camera
    //

    private static final String CAMERA = Manifest.permission.CAMERA;
    private static String[] CAMERA_PERMISSIONS = {CAMERA};

    public static boolean canAccessCamera(Activity activity) {
        return PERMISSION_GRANTED == checkSelfPermission(activity, CAMERA);
    }

    public static void requestCameraPermissions(Activity activity) {
        requestPermissions(activity, CAMERA_PERMISSIONS, RequestCode.Camera.value);
    }
}
