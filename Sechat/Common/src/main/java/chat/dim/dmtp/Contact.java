/* license: https://mit-license.org
 *
 *  DMTP: Direct Message Transfer Protocol
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
package chat.dim.dmtp;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.dmtp.protocol.Command;
import chat.dim.dmtp.protocol.LocationValue;
import chat.dim.net.Connection;
import chat.dim.net.ConnectionState;
import chat.dim.net.Hub;
import chat.dim.stun.valus.MappedAddressValue;
import chat.dim.stun.valus.SourceAddressValue;
import chat.dim.tlv.values.RawValue;
import chat.dim.tlv.values.Value32;
import chat.dim.turn.values.RelayedAddressValue;
import chat.dim.type.ByteArray;

public class Contact {

    public static long EXPIRES = 24 * 3600 * 1000; // 24 hours (milliseconds)

    public final String identifier;

    // locations order by time
    private final List<LocationValue> locations = new ArrayList<>();
    private final ReadWriteLock locationLock = new ReentrantReadWriteLock();

    public Contact(String id) {
        super();
        this.identifier = id;
    }

    /**
     *  Get all locations
     *
     * @return locations ordered by time (reversed)
     */
    public List<LocationValue> allLocations() {
        List<LocationValue> reversed = new ArrayList<>();
        Lock readLock = locationLock.readLock();
        readLock.lock();
        try {
            LocationValue item;
            int index;
            for (index = locations.size() - 1; index >= 0; --index) {
                item = locations.get(index);
                reversed.add(item);
            }
        } finally {
            readLock.unlock();
        }
        return reversed;
    }

    public LocationValue getLocation(SocketAddress address) {
        LocationValue location = null;
        Lock readLock = locationLock.readLock();
        readLock.lock();
        try {
            assert address instanceof InetSocketAddress : "address error: " + address;
            LocationValue item;
            int index;
            for (index = locations.size() - 1; index >= 0; --index) {
                item = locations.get(index);
                // check source address
                if (address.equals(item.getSourceAddress())) {
                    location = item;
                    break;
                }
                // check mapped address
                if (address.equals(item.getMappedAddress())) {
                    location = item;
                    break;
                }
            }
        } finally {
            readLock.unlock();
        }
        return location;
    }

    /**
     *  When received 'HI' command, update the location in it
     *
     * @param location - location info with signature and time
     * @return false on error
     */
    public boolean storeLocation(LocationValue location) {
        if (!verifyLocation(location)) {
            return false;
        }

        boolean ok = true;
        Lock writeLock = locationLock.writeLock();
        writeLock.lock();
        try {
            SocketAddress sourceAddress = location.getSourceAddress();
            SocketAddress mappedAddress = location.getMappedAddress();
            long timestamp = location.getTimestamp();

            LocationValue item;
            int index;
            for (index = locations.size() - 1; index >= 0; --index) {
                item = locations.get(index);
                if (!sourceAddress.equals(item.getSourceAddress())) {
                    continue;
                }
                if (!mappedAddress.equals(item.getMappedAddress())) {
                    continue;
                }
                if (timestamp < item.getTimestamp()) {
                    // this location info is expired
                    ok = false;
                    break;
                }
                // remove location(s) with same addresses
                locations.remove(index);
            }
            if (ok) {
                // insert (ordered by time)
                for (index = locations.size() - 1; index >= 0; --index) {
                    item = locations.get(index);
                    if (timestamp >= item.getTimestamp()) {
                        // got the position
                        break;
                    }
                }
                // insert after it
                locations.add(index + 1, location);
            }
        } finally {
            writeLock.unlock();
        }
        return ok;
    }

    /**
     *  When receive 'BYE' command, remove the location in it
     *
     * @param location - location info with signature and time
     * @return false on error
     */
    public boolean clearLocation(LocationValue location) {
        if (!verifyLocation(location)) {
            return false;
        }

        int count = 0;
        Lock writeLock = locationLock.writeLock();
        writeLock.lock();
        try {
            SocketAddress sourceAddress = location.getSourceAddress();
            SocketAddress mappedAddress = location.getMappedAddress();

            LocationValue item;
            int index;
            for (index = locations.size() - 1; index >= 0; --index) {
                item = locations.get(index);
                if (!sourceAddress.equals(item.getSourceAddress())) {
                    continue;
                }
                if (!mappedAddress.equals(item.getMappedAddress())) {
                    continue;
                }
                // remove location(s) with same addresses
                locations.remove(index);
                ++count;
            }
        } finally {
            writeLock.unlock();
        }
        return count > 0;
    }

    //
    //  Signature
    //

    private static ByteArray getSignData(LocationValue location) {
        MappedAddressValue mappedAddress = (MappedAddressValue) location.get(Command.MAPPED_ADDRESS);
        if (mappedAddress == null) {
            return null;
        }
        SourceAddressValue sourceAddress = (SourceAddressValue) location.get(Command.SOURCE_ADDRESS);
        RelayedAddressValue relayedAddress = (RelayedAddressValue) location.get(Command.RELAYED_ADDRESS);
        Value32 timestamp = (Value32) location.get(Command.TIME);
        // data = "source_address" + "mapped_address" + "relayed_address" + "time"
        ByteArray data = mappedAddress;
        if (sourceAddress != null) {
            data = sourceAddress.concat(data);
        }
        if (relayedAddress != null) {
            data = data.concat(relayedAddress);
        }
        if (timestamp != null) {
            data = data.concat(timestamp);
        }
        return data;
    }

    /**
     *  Sign location addresses and time
     *
     * @param location - location info with 'MAPPED-ADDRESS' from server
     * @return signed location info
     */
    public LocationValue signLocation(LocationValue location) {
        ByteArray data = getSignData(location);
        if (data == null) {
            return null;
        }
        // TODO: sign it with private key
        byte[] sign = ("sign(" + data + ")").getBytes();
        RawValue signature = new RawValue(sign);
        // create
        return LocationValue.create(location.getIdentifier(),
                location.getSourceAddress(),
                location.getMappedAddress(),
                location.getRelayedAddress(),
                location.getTimestamp(),
                signature,
                location.getNat());
    }

    public boolean verifyLocation(LocationValue location) {
        String identifier = location.getIdentifier();
        if (identifier == null) {
            return false;
        }

        ByteArray data = getSignData(location);
        ByteArray signature = location.getSignature();
        if (data == null || signature == null) {
            return false;
        }
        // TODO: verify data and signature with public key
        return true;
    }

    public void purge(Hub peer) {
        Lock writeLock = locationLock.writeLock();
        writeLock.lock();
        try {
            LocationValue item;
            for (int index = locations.size() - 1; index >= 0; --index) {
                item = locations.get(index);
                if (isExpired(item, peer)) {
                    locations.remove(index);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     *  Check connection for client node; check timestamp for server node
     *
     * @param location - user's location info
     * @param peer     - node peer
     * @return true to remove location
     */
    public static boolean isExpired(LocationValue location, Hub peer) {
        // if source-address's connection exists and not error,
        //    location not expired;
        SocketAddress sourceAddress = location.getSourceAddress();
        // if mapped-address's connection exists and not error,
        //    location not expired too.
        SocketAddress mappedAddress = location.getMappedAddress();
        return isExpired(sourceAddress, peer) && isExpired(mappedAddress, peer);
    }
    private static boolean isExpired(SocketAddress address, Hub peer) {
        if (address == null) {
            return true;
        }
        Connection conn = peer.connect(address, null);
        if (conn == null) {
            return true;
        }
        ConnectionState state = conn.getState();
        return state != null && state.equals(ConnectionState.Order.ERROR);
    }

    public static boolean isExpired(LocationValue location) {
        long timestamp = location.getTimestamp();
        if (timestamp <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        return now > (timestamp * 1000 + EXPIRES);
    }
}
