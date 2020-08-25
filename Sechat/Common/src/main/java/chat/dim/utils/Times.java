/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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
package chat.dim.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Times {

    /**
     *  Fuzzy comparing times
     *
     * @param time1 - date 1
     * @param time2 - date 2
     * @return -1 on time1 before than time2, 1 on time1 after than time2
     */
    public static int fuzzyCompare(Date time1, Date time2) {
        long t1 = time1.getTime();
        long t2 = time2.getTime();
        if (t1 < (t2 - 60 * 1000)) {
            return -1;
        }
        if (t1 > (t2 + 60 * 1000)) {
            return 1;
        }
        return 0;
    }

    /**
     *  Get readable time string
     *
     * @param now - time
     * @return readable string
     */
    public static String getTimeString(Date now) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        long timestamp = now.getTime();
        long time = calendar.getTimeInMillis();

        SimpleDateFormat formatter;
        if (timestamp >= time) {
            // today
            formatter = new SimpleDateFormat("a HH:mm", Locale.CHINA);
        } else if (timestamp >= (time - 72 * 3600 * 1000)) {
            // recently
            formatter = new SimpleDateFormat("EEEE HH:mm", Locale.CHINA);
        } else {
            calendar.set(Calendar.MONTH, 0);
            calendar.set(Calendar.DAY_OF_MONTH, 0);
            time = calendar.getTimeInMillis();
            if (timestamp >= time) {
                // this year
                formatter = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
            } else {
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
            }
        }
        return formatter.format(now);
    }
}
