# Secure Chat (Android version)
Demo project of DIM Client, just for study purpose.

Dependencies:

- [DIM SDK (sdk-java)](https://github.com/dimchat/sdk-java)
	- [DIM Core (core-java)](https://github.com/dimchat/core-java)
		- [DaoKeDao (dkd-java)](https://github.com/dimchat/dkd-java)
		- [MingKeMing (mkm-java)](https://github.com/dimchat/mkm-java)
		- [Crypto](https://github.com/dimchat/mkm-java/tree/master/Crypto) digest, coders & keys
- Plugins
	- [Core Plugins](https://github.com/dimchat/sdk-java/tree/master/Plugins)
	- [Crypto Plugins](https://github.com/dimchat/sdk-java/tree/master/CryptoPlugins) - key plugins powered by [bouncy-castle](https://www.bouncycastle.org/)
	- [Native Plugins](https://github.com/dimchat/sdk-java/tree/master/NativePlugins) - key plugins based on JNI
- [Network Module](https://github.com/dimchat/sdk-java/tree/master/StarGate)
	- [Star Trek](https://github.com/moky/StarTrek) channel, connection, hub, ...
	- [TCP](https://github.com/moky/wormhole/tree/master/tcp-java)
	- [UDP](https://github.com/moky/wormhole/tree/master/udp-java)
	- [State Machine](https://github.com/moky/FiniteStateMachine/tree/master/fsm-java)
- Demo Libs
	- [Common](https://github.com/dimchat/demo-java/tree/main/Common) - common module for station/client
	- [Network](https://github.com/dimchat/demo-java/tree/main/Network) - network module for station/client
	- [Database](https://github.com/dimchat/demo-java/tree/main/SQLite) - database module powered by [SQLite](https://xerial.org/)
	- [Client](https://github.com/dimchat/demo-java/tree/main/Client) - client module

## Sub Modules

### SDK Modules

|   Module   | Version |  Description     |
|------------|---------|------------------|
| Crypto     | 0.6.2   | Keys             |
| MingKeMing | 0.10.2  | Account Module   |
| DaoKeDao   | 0.10.2  | Message Module   |
| DIMP       | 0.10.3  | Core Protocols   |
| SDK        | 0.8.6   |                  |
| Plugins    | 0.4.4   |                  |

### Network Modules

|   Module   | Version |  Description     |
|------------|---------|------------------|
| ByteArray  | 0.1.2   | Byte Buffer      |
| TLV        | 0.1.4   | Tag Length Value |
| STUN       | 0.1.5   |                  |
| TURN       | 0.1.5   |                  |
| MTP        | 0.1.7   | Network Packing  |
| StarTrek   | 0.3.8   | Transport        |
| TCP        | 0.3.8   |                  |
| UDP        | 0.3.8   |                  |
| DMTP       | 0.2.3   | Binary Messaging |

### Others

|   Module   | Version |  Description     |
|------------|---------|------------------|
| FSM        | 0.2.5   | State Machine    |
| StarGate   | 0.4.1   | Transport        |
| LNC        | 0.1.1   | Notification     |
| DOS        | 0.1.3   | File System      |


## Architecture Diagram

<style>
pre code {
    font-family: "Lucida Console", "Consolas", Monaco, monospace;
    line-height: 0px;
}
</style>

```
          
          
                 +-------+        +--------+        +-------+
          .....> |  SDK  | .....> |  DIMP  | .....> |  DKD  |
          :      +-------+        +--------+        +-------+
          :                           ^                 :
          :                           :                 V
          :          +-----------+    :             +-------+      +--------+
          :........> |  Plugins  | ...:             |  MKM  | ...> | Crypto |
          :          +-----------+                  +-------+      +--------+
          :
    +==========+
    |  SEChat  |
    +==========+
          :
          :     +-------+
          :...> |  LNC  |
          :     +-------+
          :     
          :     +-------+
          :...> |  DOS  |
          :     +-------+
          :
          :     +------------+       +-------+     +----------+     +-------+
          :...> |  StarGate  | ....> |  TCP  | ..> | StarTrek | ..> |  FSM  |
          :     +------------+       +-------+     +----------+     +-------+
          :            :                                ^
          :            :             +-------+          :
          :            :...........> |  UDP  | .........:
          :                          +-------+          :
          :                                             V
          :     +--------+                          +-------+           
          :...> |  DMTP  | .......................> |  MTP  | ...........
                +--------+            :             +-------+           :
                                      V                                 V
                +--------+        +--------+        +-------+        +------+
                |  TURN  | .....> |  STUN  | .....> |  TLV  | .....> |  BA  |
                +--------+        +--------+        +-------+        +------+


```

--
<i>Edited by [Alber Moky](https://twitter.com/AlbertMoky) @ 2023-3-25</i>