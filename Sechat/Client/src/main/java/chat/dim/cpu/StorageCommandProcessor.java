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
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.User;
import chat.dim.crypto.Password;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
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

    @SuppressWarnings("unchecked")
    private Object decryptData(StorageCommand cmd) {
        // 1. get encrypt key
        byte[] key = cmd.getKey();
        if (key == null) {
            throw new NullPointerException("key not found: " + cmd);
        }
        // 2. get user ID
        ID identifier = cmd.getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("ID not found: " + cmd);
        }
        // 3. decrypt key
        Facebook facebook = getFacebook();
        User user = facebook.getUser(identifier);
        key = user.decrypt(key);
        if (key == null) {
            throw new NullPointerException("failed to decrypt key: " + cmd);
        }
        // 4. decode key
        Object dict = JSON.decode(key);
        SymmetricKey password = SymmetricKey.parse((Map<String, Object>) dict);
        // 5. decrypt data
        return decryptData(cmd, password);
    }

    //---- Contacts

    private List<Content> saveContacts(List<String> contacts, ID user) {
        // TODO: save contacts when import your account in a new app
        return null;
    }

    // decrypt and save contacts for user
    @SuppressWarnings("unchecked")
    private List<Content> processContacts(StorageCommand cmd) {
        List<String> contacts = (List) cmd.get("contacts");
        if (contacts == null) {
            contacts = (List) decryptData(cmd);
            if (contacts == null) {
                throw new NullPointerException("failed to decrypt contacts: " + cmd);
            }
        }
        ID identifier = cmd.getIdentifier();
        return saveContacts(contacts, identifier);
    }

    //---- Private Key

    private List<Content> savePrivateKey(PrivateKey key, ID user) {
        // TODO: save private key when import your accounts from network
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Content> processPrivateKey(StorageCommand cmd) {
        String string = "<TODO: input your password>";
        SymmetricKey password = Password.generate(string);
        Object dict = decryptData(cmd, password);
        PrivateKey key = PrivateKey.parse((Map<String, Object>) dict);
        if (key == null) {
            throw new NullPointerException("failed to decrypt private key: " + cmd);
        }
        ID identifier = cmd.getIdentifier();
        return savePrivateKey(key, identifier);
    }

    @Override
    public List<Content> execute(Command cmd, ReliableMessage rMsg) {
        assert cmd instanceof StorageCommand : "storage command error: " + cmd;
        StorageCommand sCmd = (StorageCommand) cmd;
        String title = sCmd.getTitle();
        if (title.equals(StorageCommand.CONTACTS)) {
            return processContacts(sCmd);
        } else if (title.equals(StorageCommand.PRIVATE_KEY)) {
            return processPrivateKey(sCmd);
        }
        throw new UnsupportedOperationException("Unsupported storage, title: " + title);
    }
}
