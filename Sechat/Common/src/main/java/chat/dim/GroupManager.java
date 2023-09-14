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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.dbi.AccountDBI;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.port.Departure;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.type.Pair;

/**
 *  This is for sending group message, or managing group members
 */
public enum GroupManager implements Group.DataSource {

   INSTANCE;

   public static GroupManager getInstance() {
      return INSTANCE;
   }

   private final Map<ID, ID>         cachedGroupFounders = new HashMap<>();
   private final Map<ID, ID>           cachedGroupOwners = new HashMap<>();
   private final Map<ID, List<ID>>    cachedGroupMembers = new HashMap<>();
   private final Map<ID, List<ID>> cachedGroupAssistants = new HashMap<>();
   private final List<ID>              defaultAssistants = new ArrayList<>();

   public ClientMessenger messenger;

   private CommonFacebook getFacebook() {
      return messenger.getFacebook();
   }

   /**
    *  Send group message content
    *
    * @param content - message content
    * @return false on no bots found
    */
   public boolean sendContent(Content content, ID group) {
      assert group.isGroup() : "group ID error: " + group;
      ID gid = content.getGroup();
      if (gid == null) {
         content.setGroup(group);
      } else if (!gid.equals(group)) {
         throw new IllegalArgumentException("group ID not match: " + gid + ", " + group);
      }
      List<ID> assistants = getAssistants(group);
      Pair<InstantMessage, ReliableMessage> result;
      for (ID bot : assistants) {
         // send to any bot
         result = messenger.sendContent(null, bot, content, Departure.Priority.NORMAL.value);
         if (result.second != null) {
            // only send to one bot, let the bot to split and
            // forward this message to all members
            return true;
         }
      }
      return false;
   }

   private void sendCommand(Command content, ID receiver) {
      assert receiver != null : "receiver should not be empty";
      messenger.sendContent(null, receiver, content, Departure.Priority.NORMAL.value);
   }
   private void sendCommand(Command content, List<ID> members) {
      assert members != null : "receivers should not be empty";
      for (ID receiver : members) {
         sendCommand(content, receiver);
      }
   }

   /**
    *  Invite new members to this group
    *  (only existed member/assistant can do this)
    *
    * @param newMembers - new members ID list
    * @return true on success
    */
   public boolean invite(List<ID> newMembers, ID group) {
      assert group.isGroup() : "group ID error: " + group;

      // TODO: make sure group meta exists
      // TODO: make sure current user is a member

      // 0. build 'meta/document' command
      CommonFacebook facebook = getFacebook();
      Meta meta = facebook.getMeta(group);
      if (meta == null) {
         throw new NullPointerException("failed to get meta for group: " + group);
      }
      Document doc = facebook.getDocument(group, "*");
      Command command;
      if (doc == null) {
         // empty document
         command = MetaCommand.response(group, meta);
      } else {
         command = DocumentCommand.response(group, meta, doc);
      }
      List<ID> bots = getAssistants(group);
      // 1. send 'meta/document' command
      sendCommand(command, bots);                // to all assistants

      // 2. update local members and notice all bots & members
      List<ID> members = getMembers(group);
      if (members.size() <= 2) { // new group?
         // 2.0. update local storage
         members = addMembers(newMembers, group);
         // 2.1. send 'meta/document' command
         sendCommand(command, members);         // to all members
         // 2.3. send 'invite' command with all members
         command = GroupCommand.invite(group, members);
         sendCommand(command, bots);            // to group assistants
         sendCommand(command, members);         // to all members
      } else {
         // 2.1. send 'meta/document' command
         //sendGroupCommand(command, members);  // to old members
         sendCommand(command, newMembers);      // to new members
         // 2.2. send 'invite' command with new members only
         command = GroupCommand.invite(group, newMembers);
         sendCommand(command, bots);            // to group assistants
         sendCommand(command, members);         // to old members
         // 3. update local storage
         members = addMembers(newMembers, group);
         // 2.4. send 'invite' command with all members
         command = GroupCommand.invite(group, members);
         sendCommand(command, newMembers);      // to new members
      }

      return true;
   }
   public boolean invite(ID member, ID group) {
      List<ID> array = new ArrayList<>();
      array.add(member);
      return invite(array, group);
   }

   /**
    *  Expel members from this group
    *  (only group owner/assistant can do this)
    *
    * @param outMembers - existed member ID list
    * @return true on success
    */
   public boolean expel(List<ID> outMembers, ID group) {
      assert group.isGroup() : "group ID error: " + group;
      ID owner = getOwner(group);
      List<ID> bots = getAssistants(group);

      // TODO: make sure group meta exists
      // TODO: make sure current user is the owner

      // 0. check permission
      for (ID assistant : bots) {
         if (outMembers.contains(assistant)) {
            throw new RuntimeException("Cannot expel group assistant: " + assistant);
         }
      }
      if (outMembers.contains(owner)) {
         throw new RuntimeException("Cannot expel group owner: " + owner);
      }

      // 1. update local storage
      List<ID> members = removeMembers(outMembers, group);

      // 2. send 'expel' command
      Command command = GroupCommand.expel(group, outMembers);
      sendCommand(command, bots);        // to assistants
      sendCommand(command, members);     // to new members
      sendCommand(command, outMembers);  // to expelled members

      return true;
   }
   public boolean expel(ID member, ID group) {
      List<ID> array = new ArrayList<>();
      array.add(member);
      return expel(array, group);
   }

   /**
    *  Quit from this group
    *  (only group member can do this)
    *
    * @return true on success
    */
   public boolean quit(ID group) {
      assert group.isGroup() : "group ID error: " + group;

      CommonFacebook facebook = getFacebook();
      User user = facebook.getCurrentUser();
      if (user == null) {
         throw new NullPointerException("failed to get current user");
      }
      ID me = user.getIdentifier();

      ID owner = getOwner(group);
      List<ID> bots = getAssistants(group);
      List<ID> members = getMembers(group);

      // 0. check permission
      if (bots.contains(me)) {
         throw new RuntimeException("group assistant cannot quit: " + me + ", group: " + group);
      } else if (me.equals(owner)) {
         throw new RuntimeException("group owner cannot quit: " + owner + ", group: " + group);
      }

      // 1. update local storage
      boolean ok = false;
      if (members.remove(me)) {
         ok = saveMembers(members, group);
         //} else {
         //    // not a member now
         //    return false;
      }

      // 2. send 'quit' command
      Command command = GroupCommand.quit(group);
      sendCommand(command, bots);     // to assistants
      sendCommand(command, members);  // to new members

      return ok;
   }

   /**
    *  Query group info
    *
    * @return false on error
    */
   public boolean query(ID group) {
      return messenger.queryMembers(group);
   }

   //-------- Data Source

   @Override
   public Meta getMeta(ID group) {
      CommonFacebook facebook = getFacebook();
      return facebook.getMeta(group);
   }

   @Override
   public Document getDocument(ID group, String type) {
      CommonFacebook facebook = getFacebook();
      return facebook.getDocument(group, type);
   }

   @Override
   public ID getFounder(ID group) {
      ID founder = cachedGroupFounders.get(group);
      if (founder == null) {
         CommonFacebook facebook = getFacebook();
         founder = facebook.getFounder(group);
         if (founder == null) {
            // place holder
            founder = ID.FOUNDER;
         }
         cachedGroupFounders.put(group, founder);
      }
      if (founder.isBroadcast()) {
         // founder not found
         return null;
      }
      return founder;
   }

   @Override
   public ID getOwner(ID group) {
      ID owner = cachedGroupOwners.get(group);
      if (owner == null) {
         CommonFacebook facebook = getFacebook();
         owner = facebook.getOwner(group);
         if (owner == null) {
            // place holder
            owner = ID.ANYONE;
         }
         cachedGroupOwners.put(group, owner);
      }
      if (owner.isBroadcast()) {
         // owner not found
         return null;
      }
      return owner;
   }

   @Override
   public List<ID> getMembers(ID group) {
      List<ID> members = cachedGroupMembers.get(group);
      if (members == null) {
         CommonFacebook facebook = getFacebook();
         members = facebook.getMembers(group);
         if (members == null) {
            // place holder
            members = new ArrayList<>();
         }
         cachedGroupMembers.put(group, members);
      }
      return members;
   }

   @Override
   public List<ID> getAssistants(ID group) {
      List<ID> assistants = cachedGroupAssistants.get(group);
      if (assistants == null) {
         CommonFacebook facebook = getFacebook();
         assistants = facebook.getAssistants(group);
         if (assistants == null) {
            // placeholder
            assistants = new ArrayList<>();
         }
         cachedGroupAssistants.put(group, assistants);
      }
      if (assistants.size() > 0) {
         return assistants;
      }
      // get from global setting
      if (defaultAssistants.size() == 0) {
         // get from ANS
         ID bot = ClientFacebook.ans.identifier("assistant");
         if (bot != null) {
            defaultAssistants.add(bot);
         }
      }
      return defaultAssistants;
   }

   //
   //  MemberShip
   //

   public boolean isFounder(ID member, ID group) {
      ID founder = getFounder(group);
      if (founder != null) {
         return founder.equals(member);
      }
      // check member's public key with group's meta.key
      CommonFacebook facebook = getFacebook();
      Meta gMeta = facebook.getMeta(group);
      assert gMeta != null : "failed to get meta for group: " + group;
      Meta mMeta = facebook.getMeta(member);
      assert mMeta != null : "failed to get meta for member: " + member;
      return gMeta.matchPublicKey(mMeta.getPublicKey());
   }

   public boolean isOwner(ID member, ID group) {
      ID owner = getOwner(group);
      if (owner != null) {
         return owner.equals(member);
      }
      if (EntityType.GROUP.equals(group.getType())) {
         // this is a polylogue
         return isFounder(member, group);
      }
      throw new UnsupportedOperationException("only Polylogue so far");
   }

   //
   //  members
   //

   public boolean containsMember(ID member, ID group) {
      assert member.isUser() && group.isGroup() : "ID error: " + member + ", " + group;
      List<ID> allMembers = getMembers(group);
      int pos = allMembers.indexOf(member);
      if (pos >= 0) {
         return true;
      }
      ID owner = getOwner(group);
      return owner != null && owner.equals(member);
   }

   public boolean addMember(ID member, ID group) {
      assert member.isUser() && group.isGroup() : "ID error: " + member + ", " + group;
      List<ID> allMembers = getMembers(group);
      int pos = allMembers.indexOf(member);
      if (pos >= 0) {
         // already exists
         return false;
      }
      allMembers.add(member);
      return saveMembers(allMembers, group);
   }
   public boolean removeMember(ID member, ID group) {
      assert member.isUser() && group.isGroup() : "ID error: " + member + ", " + group;
      List<ID> allMembers = getMembers(group);
      int pos = allMembers.indexOf(member);
      if (pos < 0) {
         // not exists
         return false;
      }
      allMembers.remove(pos);
      return saveMembers(allMembers, group);
   }

   private List<ID> addMembers(List<ID> newMembers, ID group) {
      List<ID> members = getMembers(group);
      int count = 0;
      for (ID member : newMembers) {
         if (members.contains(member)) {
            continue;
         }
         members.add(member);
         ++count;
      }
      if (count > 0) {
         saveMembers(members, group);
      }
      return members;
   }
   private List<ID> removeMembers(List<ID> outMembers, ID group) {
      List<ID> members = getMembers(group);
      int count = 0;
      for (ID member : outMembers) {
         if (!members.contains(member)) {
            continue;
         }
         members.remove(member);
         ++count;
      }
      if (count > 0) {
         saveMembers(members, group);
      }
      return members;
   }

   public boolean saveMembers(List<ID> members, ID group) {
      CommonFacebook facebook = getFacebook();
      AccountDBI db = facebook.getDatabase();
      if (db.saveMembers(members, group)) {
         // erase cache for reload
         cachedGroupMembers.remove(group);
         return true;
      } else {
         return false;
      }
   }

   //
   //  assistants
   //

   public boolean containsAssistant(ID user, ID group) {
      List<ID> assistants = getAssistants(group);
      if (assistants == defaultAssistants) {
         // assistants not found
         return false;
      }
      return assistants.contains(user);
   }

   public boolean addAssistant(ID bot, ID group) {
      if (group == null) {
         defaultAssistants.add(0, bot);
         return true;
      }
      List<ID> assistants = getAssistants(group);
      if (assistants == defaultAssistants) {
         // assistants not found
         assistants = new ArrayList<>();
      } else if (assistants.contains(bot)) {
         // already exists
         return false;
      }
      assistants.add(0, bot);
      return saveAssistants(assistants, group);
   }

   public boolean saveAssistants(List<ID> bots, ID group) {
      CommonFacebook facebook = getFacebook();
      AccountDBI db = facebook.getDatabase();
      if (db.saveAssistants(bots, group)) {
         // erase cache for reload
         cachedGroupAssistants.remove(group);
         return true;
      } else {
         return false;
      }
   }

   public boolean removeGroup(ID group) {
      // TODO: remove group completely
      //return groupTable.removeGroup(group);
      return false;
   }
}
