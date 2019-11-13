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
package chat.dim.common;

import java.util.List;
import java.util.Locale;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.crypto.impl.PublicKeyImpl;
import chat.dim.extension.BTCMeta;
import chat.dim.extension.ECCPrivateKey;
import chat.dim.extension.ECCPublicKey;
import chat.dim.extension.ETHMeta;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.mkm.User;

public class Facebook extends chat.dim.Facebook {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    public SocialNetworkDataSource database = null;

    //---- Private Key

    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        return database.savePrivateKey(privateKey, identifier);
    }

    @Override
    protected PrivateKey loadPrivateKey(ID user) {
        // FIXME: which key?
        return database.getPrivateKeyForSignature(user);
    }

    //---- Meta

    public boolean saveMeta(Meta meta, ID entity) {
        return database.saveMeta(meta, entity);
    }

    @Override
    protected Meta loadMeta(ID identifier) {
        return database.getMeta(identifier);
    }

    //---- Profile

    public boolean saveProfile(Profile profile) {
        return database.saveProfile(profile);
    }

    @Override
    protected Profile loadProfile(ID identifier) {
        return database.getProfile(identifier);
    }

    public boolean verifyProfile(Profile profile) {
        return database.verifyProfile(profile);
    }

    //---- Relationship

    public boolean saveContacts(List<ID> contacts, ID user) {
        return database.saveContacts(contacts, user);
    }

    @Override
    protected List<ID> loadContacts(ID user) {
        return database.getContacts(user);
    }

    public boolean addMember(ID member, ID group) {
        return database.addMember(member, group);
    }

    public boolean removeMember(ID member, ID group) {
        return database.removeMember(member, group);
    }

    public boolean saveMembers(List<ID> members, ID group) {
        return database.saveMembers(members, group);
    }

    @Override
    protected List<ID> loadMembers(ID group) {
        return database.getMembers(group);
    }

    //--------

    public String getNickname(ID identifier) {
        assert identifier.getType().isUser();
        User user = getUser(identifier);
        return user == null ? null : user.getName();
    }

    public String getNumberString(ID identifier) {
        long number = identifier.getNumber();
        String string = String.format(Locale.CHINA, "%010d", number);
        string = string.substring(0, 3) + "-" + string.substring(3, 6) + "-" + string.substring(6);
        return string;
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        // get from database
        ID founder = database.getFounder(group);
        if (founder != null) {
            return founder;
        }
        return super.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        // get from database
        ID owner = database.getOwner(group);
        if (owner != null) {
            return owner;
        }
        return super.getOwner(group);
    }

    static {
        // register new asymmetric cryptography key classes
        PrivateKeyImpl.register(PrivateKey.ECC, ECCPrivateKey.class);
        PublicKeyImpl.register(PublicKey.ECC, ECCPublicKey.class);

        // register new address classes
//        Address.register(BTCAddress.class);
//        Address.register(ETHAddress.class);

        // register new meta classes
        Meta.register(Meta.VersionBTC, BTCMeta.class);
        Meta.register(Meta.VersionExBTC, BTCMeta.class);
        Meta.register(Meta.VersionETH, ETHMeta.class);
        Meta.register(Meta.VersionExETH, ETHMeta.class);
    }
}
