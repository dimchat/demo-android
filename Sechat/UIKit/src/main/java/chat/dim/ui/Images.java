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

import android.graphics.Bitmap;
import android.graphics.Matrix;

import java.io.ByteArrayOutputStream;

public class Images {

    public static class Size {

        public final int width;
        public final int height;

        public Size(int width, int height) {
            super();
            this.width = width;
            this.height = height;
        }
    }

    public static Size getSize(Bitmap bitmap) {
        return new Size(bitmap.getWidth(), bitmap.getHeight());
    }

    //
    //  Scale
    //

    public static Bitmap scale(Bitmap origin, Size size) {
        if (origin == null) {
            return null;
        }
        int w = origin.getWidth();
        int h = origin.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }
        float scaleX = ((float) size.width) / w;
        float scaleY = ((float) size.height) / h;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleX, scaleY);
        return Bitmap.createBitmap(origin, 0, 0, w, h, matrix, false);
    }

    public static byte[] thumbnail(Bitmap big) {
        Size size = getSize(big);
        if (size.width <= MAX_SIZE.width && size.height <= MAX_SIZE.height) {
            // too small, no need to thumbnail
            return compress(big, Bitmap.CompressFormat.JPEG, JPEG_THUMBNAIL_QUALITY);
        }
        size = aspectFit(size, MAX_SIZE);
        Bitmap small = scale(big, size);
        return compress(small, Bitmap.CompressFormat.JPEG, JPEG_THUMBNAIL_QUALITY);
    }

    public static Size aspectFit(Size size, Size boxSize) {
        float x = ((float) boxSize.width) / size.width;
        float y = ((float) boxSize.height) / size.height;
        float ratio = Math.min(x, y);
        int w = Math.round(size.width * ratio);
        int h = Math.round(size.height * ratio);
        return new Size(w, h);
    }

    private static final Size MAX_SIZE = new Size(128, 128);

    private static final int PNG_IMAGE_QUALITY = 100;
    private static final int JPEG_PHOTO_QUALITY = 50;
    private static final int JPEG_THUMBNAIL_QUALITY = 0;

    //
    //  Compress
    //

    public static byte[] jpeg(Bitmap bitmap) {
        return compress(bitmap, Bitmap.CompressFormat.JPEG, JPEG_PHOTO_QUALITY);
    }
    public static byte[] png(Bitmap bitmap) {
        return compress(bitmap, Bitmap.CompressFormat.PNG, PNG_IMAGE_QUALITY);
    }
    private static byte[] compress(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(format, quality, outputStream);
        return outputStream.toByteArray();
    }
}
