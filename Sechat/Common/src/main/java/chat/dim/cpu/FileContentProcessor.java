/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.cpu;

import java.util.List;

import chat.dim.Facebook;
import chat.dim.common.Messenger;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class FileContentProcessor extends ContentProcessor {

    public FileContentProcessor(Facebook facebook, chat.dim.Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    protected Messenger getMessenger() {
        return (Messenger) super.getMessenger();
    }

    /**
     *  Encrypt data in file content with the password, and upload to CDN
     *
     * @param content  - file content
     * @param password - symmetric key
     * @param iMsg - plain message
     * @return false on error
     */
    public boolean uploadFileContent(final FileContent content, final EncryptKey password, final InstantMessage iMsg) {
        final byte[] data = content.getData();
        if (data == null || data.length == 0) {
            // FIXME: already uploaded?
            //throw new NullPointerException("failed to get file data: " + content);
            return false;
        }
        // encrypt and upload file data onto CDN and save the URL in message content
        final byte[] encrypted = password.encrypt(data);
        if (encrypted == null || encrypted.length == 0) {
            throw new NullPointerException("failed to encrypt file data with key: " + password);
        }
        final String url = getMessenger().uploadData(encrypted, iMsg);
        if (url == null) {
            return false;
        } else {
            // replace 'data' with 'URL'
            content.setURL(url);
            content.setData(null);
            return true;
        }
    }

    /**
     *  Download data for file content from CDN, and decrypt it with the password
     *
     * @param content  - file content
     * @param password - symmetric key
     * @param sMsg     - encrypted message
     * @return false on error
     */
    public boolean downloadFileContent(final FileContent content, final DecryptKey password, final SecureMessage sMsg) {
        final String url = content.getURL();
        if (url == null || !url.contains("://")) {
            // download URL not found
            return false;
        }
        final InstantMessage iMsg = InstantMessage.create(sMsg.getEnvelope(), content);
        // download from CDN
        final byte[] encrypted = getMessenger().downloadData(url, iMsg);
        if (encrypted == null || encrypted.length == 0) {
            // save symmetric key for decrypting file data after download from CDN
            content.setPassword(password);
            return false;
        } else {
            // decrypt file data
            final byte[] fileData = password.decrypt(encrypted);
            if (fileData == null || fileData.length == 0) {
                throw new NullPointerException("failed to decrypt file data with key: " + password);
            }
            content.setData(fileData);
            content.setURL(null);
            return true;
        }
    }

    @Override
    public List<Content> process(final Content content, final ReliableMessage rMsg) {
        assert content instanceof FileContent : "file content error: " + content;
        // TODO: process file content

        return null;
    }
}
