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
package chat.dim.crypto;

import java.util.HashMap;
import java.util.Map;

import chat.dim.compat.CommonExtensionLoader;
import chat.dim.compat.CommonPluginLoader;
import chat.dim.format.Base64;
import chat.dim.format.UTF8;
import chat.dim.plugins.CryptoPluginLoader;
import chat.dim.protocol.Password;
import chat.dim.utils.Log;

/**
 *  This is for generating symmetric key with a text string
 */
public final class PasswordTest {

   /**
    *  Test case
    *
    * @param args - command arguments
    */
   public static void main(String[] args) {

      (new CommonExtensionLoader()).run();
      (new CommonPluginLoader()).run();
      (new CryptoPluginLoader()).run();

      Log.LEVEL = Log.DEBUG;


      String text = "Hello world!";
      String password = "12345";

      SymmetricKey key1 = Password.generate(password);
      SymmetricKey key2 = Password.generate(password);

      System.out.println("key1: " + key1);
      System.out.println("key2: " + key2);

      if (key1 == null || !key1.equals(key2)) {
         throw new AssertionError("keys not equals: " + key1 + ", " + key2);
      }
      Map<String, Object> extra = new HashMap<>();

      byte[] data = UTF8.encode(text);
      byte[] ct = key1.encrypt(data, extra);
      byte[] pt = key2.decrypt(ct, extra);

      String base64 = Base64.encode(ct);
      String res = UTF8.decode(pt);
      System.out.println(text + " -> " + base64 + " -> " + res);

      if (!base64.equals("Ty9C/v1XVW8IWbNxgpdg8Q==")) {
         Log.error("cipher text not match: " + base64);
      }
      if (res.equals(text)) {
         Log.info("Password test OK: \"" + text + "\"");
      } else {
         throw new AssertionError("failed to en/decrypt: " + text + " -> " + res);
      }

      Log.info("key1: " + key1);
      Log.info("key2: " + key2);

      SymmetricKey key = Password.generate("moky");
      String digest = Password.digest(key);
      Log.info("key digest: " + digest);
      String exp = "rZqXq3mu";
      assert exp.equals(digest) : "key digest error: " + digest + " != " + exp;

   }
}
