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
package chat.dim.sqlite.mkm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.format.Base64;
import chat.dim.format.TransportableData;
import chat.dim.log.Log;
import chat.dim.mkm.DocumentUtils;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.sqlite.DataTable;
import chat.dim.sqlite.Database;

public final class DocumentTable extends DataTable implements chat.dim.database.DocumentTable {

    private DocumentTable() {
        super();
    }

    private static DocumentTable ourInstance;
    public static DocumentTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new DocumentTable();
        }
        return ourInstance;
    }

    @Override
    protected Database getDatabase() {
        return EntityDatabase.getInstance();
    }

    // memory caches
    private final Map<String, List<Document>> docsTable = new HashMap<>();

    //
    //  chat.dim.database.UserTable
    //

    @Override
    public List<Document> getDocuments(ID entity) {
        // 1. try from memory cache
        List<Document> documents = docsTable.get(entity.toString());
        if (documents == null) {
            documents = new ArrayList<>();
            String type;
            String data;
            byte[] signature;
            Document doc;
            // 2. try from database
            String[] columns = {"type", "data", "signature"};
            String[] selectionArgs = {entity.toString()};
            try (Cursor cursor = query(EntityDatabase.T_DOCUMENT, columns, "did=?", selectionArgs, null, null, null)) {
                while (cursor.moveToNext()) {
                    type = cursor.getString(0);
                    data = cursor.getString(1);
                    signature = cursor.getBlob(2);
                    doc = Document.create(type, entity, data, TransportableData.create(signature));
                    assert doc != null : "failed to create document: " + type + ", " + entity;
                    documents.add(doc);
                }
            } catch (SQLiteCantOpenDatabaseException e) {
                e.printStackTrace();
            }

            // 3. store into memory cache
            docsTable.put(entity.toString(), documents);
        }
        return documents;
    }

    @Override
    public boolean saveDocument(Document doc) {
        // 0. check valid
        if (!doc.isValid()) {
            Log.error("document not valid: " + doc);
            return false;
        }
        ID identifier = doc.getIdentifier();
        String type = DocumentUtils.getDocumentType(doc);
        if (type == null) {
            type = "";
        }
        boolean exists = false;
        // check old documents
        List<Document> documents = getDocuments(identifier);
        for (Document item : documents) {
            if (identifier.equals(item.getIdentifier()) &&
                    type.equals(DocumentUtils.getDocumentType(item))) {
                // old record found, update it
                exists = true;
                break;
            }
        }
        boolean saved;
        if (exists) {
            saved = updateDocument(doc);
        } else {
            saved = insertDocument(doc);
        }
        if (saved) {
            Log.info("-------- entity document saved: " + identifier);
            // clear to reload
            docsTable.remove(identifier.toString());
        } else {
            Log.error("failed to save document: " + identifier);
        }
        return saved;
    }

    protected boolean updateDocument(Document doc) {
        ID identifier = doc.getIdentifier();
        String type = DocumentUtils.getDocumentType(doc);
        String data = doc.getString("data", "");
        String base64 = doc.getString("signature", "");
        // conditions
        String[] whereArgs = {identifier.toString(), type};
        // fill values
        ContentValues values = new ContentValues();
        values.put("data", data);
        values.put("signature", Base64.decode(base64));
        return update(EntityDatabase.T_DOCUMENT, values, "did=? AND type=?", whereArgs) > 0;
    }

    protected boolean insertDocument(Document doc) {
        ID identifier = doc.getIdentifier();
        String type = DocumentUtils.getDocumentType(doc);
        String data = doc.getString("data", "");
        String base64 = doc.getString("signature", "");
        // new values
        ContentValues values = new ContentValues();
        values.put("did", identifier.toString());
        values.put("type", type);
        values.put("data", data);
        values.put("signature", Base64.decode(base64));
        return insert(EntityDatabase.T_DOCUMENT, null, values) >= 0;
    }

}
