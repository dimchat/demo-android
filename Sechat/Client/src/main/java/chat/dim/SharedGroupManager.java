/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim;

import java.util.ArrayList;
import java.util.List;

import chat.dim.crypto.SymmetricKey;
import chat.dim.group.AdminManager;
import chat.dim.group.GroupDelegate;
import chat.dim.group.GroupEmitter;
import chat.dim.group.GroupManager;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.type.Pair;

/**
 *  This is for sending group message, or managing group members
 */
public enum SharedGroupManager implements Group.DataSource {

   INSTANCE;

   public static SharedGroupManager getInstance() {
      return INSTANCE;
   }

   private CommonFacebook getFacebook() {
      GlobalVariable shared = GlobalVariable.getInstance();
      return shared.facebook;
   }
   private CommonMessenger getMessenger() {
      GlobalVariable shared = GlobalVariable.getInstance();
      return shared.messenger;
   }

   //
   //  delegates
   //

   private GroupDelegate groupDelegate = null;
   private GroupManager groupManager = null;
   private AdminManager adminManager = null;
   private GroupEmitter groupEmitter = null;

   private GroupDelegate getDelegate() {
      GroupDelegate delegate = groupDelegate;
      if (delegate == null) {
         groupDelegate = delegate = new GroupDelegate(getFacebook(), getMessenger());
      }
      return delegate;
   }
   private GroupManager getManager() {
      GroupManager manager = groupManager;
      if (manager == null) {
         groupManager = manager = new GroupManager(getDelegate());
      }
      return manager;
   }
   private AdminManager getAdminManager() {
      AdminManager manager = adminManager;
      if (manager == null) {
         adminManager = manager = new AdminManager(getDelegate());
      }
      return manager;
   }
   private GroupEmitter getEmitter() {
      GroupEmitter emitter = groupEmitter;
      if (emitter == null) {
         groupEmitter = emitter = new GroupEmitter(getDelegate()) {
            @Override
            protected boolean uploadFileData(FileContent content, SymmetricKey password, ID sender) {
               GlobalVariable shared = GlobalVariable.getInstance();
               return shared.emitter.uploadFileData(content, password, sender);
            }
         };
      }
      return emitter;
   }

   public String buildGroupName(List<ID> members) {
      GroupDelegate delegate = getDelegate();
      return delegate.buildGroupName(members);
   }

   //
   //  Entity DataSource
   //

   @Override
   public Meta getMeta(ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.getMeta(group);
   }

   @Override
   public List<Document> getDocuments(ID identifier) {
      GroupDelegate delegate = getDelegate();
      return delegate.getDocuments(identifier);
   }

   //
   //  Group DataSource
   //

   @Override
   public ID getFounder(ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.getFounder(group);
   }

   @Override
   public ID getOwner(ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.getOwner(group);
   }

   @Override
   public List<ID> getMembers(ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.getMembers(group);
   }

   @Override
   public List<ID> getAssistants(ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.getAssistants(group);
   }

   public List<ID> getAdministrators(ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.getAdministrators(group);
   }

   public boolean isOwner(ID user, ID group) {
      GroupDelegate delegate = getDelegate();
      return delegate.isOwner(user, group);
   }

   public boolean broadcastDocument(Document doc) {
      AdminManager manager = getAdminManager();
      return manager.broadcastDocument((Bulletin) doc);
   }

   //
   //  Group Manage
   //

   /**
    *  Create new group with members
    *
    * @param members - new group members
    * @return true on success
    */
   public ID createGroup(List<ID> members) {
      GroupManager manager = getManager();
      return manager.createGroup(members);
   }

   /**
    *  Update 'administrators' in bulletin document
    *
    * @param group     - group ID
    * @param newAdmins - new administrator ID list
    * @return true on success
    */
   public boolean updateAdministrators(ID group, List<ID> newAdmins) {
      AdminManager manager = getAdminManager();
      return manager.updateAdministrators(group, newAdmins);
   }

   /**
    *  Reset group members
    *
    * @param group      - group ID
    * @param newMembers - new member ID list
    * @return true on success
    */
   public boolean resetGroupMembers(ID group, List<ID> newMembers) {
      GroupManager manager = getManager();
      return manager.resetMembers(group, newMembers);
   }

   /**
    *  Expel members from this group
    *  (only group owner/assistant can do this)
    *
    * @param expelMembers - members to be removed
    * @return true on success
    */
   public boolean expelGroupMembers(ID group, List<ID> expelMembers) {
      assert group.isGroup() && !expelMembers.isEmpty() : "params error: " + group + ", " + expelMembers;

      User user = getFacebook().getCurrentUser();
      if (user == null) {
         assert false : "failed to get current user";
         return false;
      }
      ID me = user.getIdentifier();

      GroupDelegate delegate = getDelegate();
      List<ID> oldMembers = delegate.getMembers(group);

      boolean isOwner = delegate.isOwner(me, group);
      boolean isAdmin = delegate.isAdministrator(me, group);

      // 0. check permission
      boolean canReset = isOwner || isAdmin;
      if (canReset) {
         // You are the owner/admin, then
         // remove the members and 'reset' the group
         List<ID> members = new ArrayList<>(oldMembers);
         for (ID item : expelMembers) {
            members.remove(item);
         }
         return resetGroupMembers(group, members);
      }

      // not an admin/owner
      throw new SecurityException("Cannot expel members from group: " + group);
   }

   /**
    *  Invite new members to this group
    *  (only existed member/assistant can do this)
    *
    * @param newMembers - new members ID list
    * @return true on success
    */
   public boolean inviteGroupMembers(ID group, List<ID> newMembers) {
      GroupManager manager = getManager();
      return manager.inviteMembers(group, newMembers);
   }

   /**
    *  Quit from this group
    *  (only group member can do this)
    *
    * @return true on success
    */
   public boolean quitGroup(ID group) {
      GroupManager manager = getManager();
      return manager.quitGroup(group);
   }

   //
   //  Sending group message
   //

   public Pair<InstantMessage, ReliableMessage> sendContent(Content content, ID group, int priority) {
      content.setGroup(group);
      GroupEmitter emitter = getEmitter();
      return emitter.sendContent(content, priority);
   }

}
