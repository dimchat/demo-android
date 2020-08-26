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
package chat.dim.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.model.NetworkDatabase;
import chat.dim.sqlite.ans.AddressNameDatabase;
import chat.dim.sqlite.ans.AddressNameTable;
import chat.dim.sqlite.dkd.MessageDatabase;
import chat.dim.sqlite.dkd.MessageTable;
import chat.dim.sqlite.mkm.ContactTable;
import chat.dim.sqlite.mkm.EntityDatabase;
import chat.dim.sqlite.mkm.GroupTable;
import chat.dim.sqlite.mkm.LoginTable;
import chat.dim.sqlite.mkm.UserTable;
import chat.dim.sqlite.sp.ProviderDatabase;
import chat.dim.sqlite.sp.ProviderTable;

public abstract class Database extends SQLiteOpenHelper {

    protected Database(Context context, String name, int version) {
        super(context, name, null, version);
    }

    protected static String getFilePath(String dbName) {
        return Paths.appendPathComponent(ExternalStorage.getRoot(), "sqlite", dbName);
    }

    public static void setContext(Context context) {
        // databases
        ProviderDatabase.setContext(context);
        AddressNameDatabase.setContext(context);
        EntityDatabase.setContext(context);
        MessageDatabase.setContext(context);

        // tables
        NetworkDatabase netDB = NetworkDatabase.getInstance();
        netDB.providerTable = ProviderTable.getInstance();

        Facebook facebook = Facebook.getInstance();
        facebook.userTable = UserTable.getInstance();
        facebook.contactTable = ContactTable.getInstance();
        facebook.groupTable = GroupTable.getInstance();
        facebook.ansTable = AddressNameTable.getInstance();

        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        msgDB.messageTable = MessageTable.getInstance();

        LoginCommandProcessor.dataHandler = LoginTable.getInstance();
    }
}
