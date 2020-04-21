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
package chat.dim.cpu;

import java.util.List;

import chat.dim.Content;
import chat.dim.Facebook;
import chat.dim.ID;
import chat.dim.ReliableMessage;
import chat.dim.Messenger;
import chat.dim.User;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.extension.Password;
import chat.dim.format.JSON;
import chat.dim.protocol.StorageCommand;

public class StorageCommandProcessor extends CommandProcessor {

    public StorageCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Object decryptData(StorageCommand cmd, SymmetricKey password) {
        // 1. get encrypted data
        byte[] data = cmd.getData();
        if (data == null) {
            throw new NullPointerException("data not found: " + cmd);
        }
        // 2. decrypt data
        data = password.decrypt(data);
        if (data == null) {
            throw new NullPointerException("failed to decrypt data: " + cmd);
        }
        // 3. decode data
        return JSON.decode(data);
    }

    private Object decryptData(StorageCommand cmd) throws ClassNotFoundException {
        // 1. get encrypt key
        byte[] key = cmd.getKey();
        if (key == null) {
            throw new NullPointerException("key not found: " + cmd);
        }
        // 2. get user ID
        String identifier = cmd.getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("ID not found: " + cmd);
        }
        // 3. decrypt key
        Facebook facebook = getFacebook();
        User user = facebook.getUser(facebook.getID(identifier));
        key = user.decrypt(key);
        if (key == null) {
            throw new NullPointerException("failed to decrypt key: " + cmd);
        }
        // 4. decode key
        Object dict = JSON.decode(key);
        SymmetricKey password = SymmetricKey.getInstance(dict);
        // 5. decrypt data
        return decryptData(cmd, password);
    }

    //---- Contacts

    private Content saveContacts(List<String> contacts, ID user) {
        // TODO: save contacts when import your account in a new app
        return null;
    }

    @SuppressWarnings("unchecked")
    private Content processContacts(StorageCommand cmd) {
        // decrypt and save contacts for user
        Object contacts = cmd.get("contacts");
        if (contacts == null) {
            try {
                contacts = decryptData(cmd);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (contacts == null) {
                throw new NullPointerException("failed to decrypt contacts: " + cmd);
            }
        }
        Facebook facebook = getFacebook();
        ID identifier = facebook.getID(cmd.getIdentifier());
        return saveContacts((List<String>) contacts, identifier);
    }

    //---- Private Key

    private Content savePrivateKey(PrivateKey key, ID user) {
        // TODO: save private key when import your accounts from network
        return null;
    }

    private Content processPrivateKey(StorageCommand cmd) {
        String string = "<TODO: input your password>";
        SymmetricKey password = Password.generate(string);
        Object dict = decryptData(cmd, password);
        PrivateKey key = null;
        try {
            key = PrivateKey.getInstance(dict);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (key == null) {
            throw new NullPointerException("failed to decrypt private key: " + cmd);
        }
        Facebook facebook = getFacebook();
        ID identifier = facebook.getID(cmd.getIdentifier());
        return savePrivateKey(key, identifier);
    }

    @Override
    public Content process(Content content, ID sender, ReliableMessage rMsg) {
        assert content instanceof StorageCommand : "storage command error: " + content;
        StorageCommand cmd = (StorageCommand) content;
        String title = cmd.title;
        if (title.equals(StorageCommand.CONTACTS)) {
            return processContacts(cmd);
        } else if (title.equals(StorageCommand.PRIVATE_KEY)) {
            return processPrivateKey(cmd);
        }
        throw new UnsupportedOperationException("Unsupported storage, title: " + title);
    }
}
