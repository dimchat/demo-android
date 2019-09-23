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
package chat.dim.client;

import java.util.List;
import java.util.Set;

import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.mkm.UserDataSource;

public interface SocialNetworkDataSource extends UserDataSource, GroupDataSource {

    boolean savePrivateKey(PrivateKey privateKey, ID identifier);

    //-------- Meta

    boolean saveMeta(Meta meta, ID identifier);

    //-------- Profile

    boolean verifyProfile(Profile profile);

    boolean saveProfile(Profile profile);

    //-------- Address Name Service

    boolean saveAnsRecord(String name, ID identifier);

    ID ansRecord(String name);

    Set<String> ansNames(String identifier);

    //-------- User

    LocalUser getCurrentUser();

    void setCurrentUser(LocalUser user);

    List<ID> allUsers();

    boolean addUser(ID user);

    boolean removeUser(ID user);

    boolean addContact(ID contact, ID user);

    boolean removeContact(ID contact, ID user);

    //-------- Group

    boolean existsMember(ID member, ID group);
}
