# Demo-Android
DIM Client


## Sub Modules

### SDK Modules

|   Module   | Version |  Description     |
|------------|---------|------------------|
| Crypto     | 0.5.3   | Keys             |
| MingKeMing | 0.9.4   | Account Module   |
| DaoKeDao   | 0.9.4   | Message Module   |
| DIMP       | 0.9.6   | Core Protocols   |
| SDK        | 0.5.20  |                  |
| Plugins    | 0.3.5   |                  |

### Network Modules

|   Module   | Version |  Description     |
|------------|---------|------------------|
| ByteArray  | 0.1.0   | Byte Buffer      |
| TLV        | 0.1.2   | Tag Length Value |
| STUN       | 0.1.2   |                  |
| TURN       | 0.1.2   |                  |
| MTP        | 0.1.3   | Network Packing  |
| TCP        | 0.1.7   |                  |
| UDP        | 0.1.1   |                  |
| DMTP       | 0.1.2   | Binary Messaging |
| StarTrek   | 0.1.2   | Transport        |

### Other Modules

|   Module   | Version |  Description     |
|------------|---------|------------------|
| FSM        | 0.1.5   | State Machine    |
| StarGate   | 0.3.3   | Transport        |
| LNC        | 0.1.0   | Notification     |
| DOS        | 0.1.2   | File System      |


## Dependencies

```

                +--------+        +--------+        +-------+        +------+
                |  TURN  | .....> |  STUN  | .....> |  TLV  | .....> |  BA  |
                +--------+        +--------+        +-------+        +------+
                                      ^                                 ^
                                      .                                 .
                             ..........                        ..........
                             .                                 .
                +--------+   .    +-------+        +-------+   .
          ....> |  DMTP  | .....> |  UDP  | .....> |  MTP  | ...
          .     +--------+        +-------+   .    +-------+
          .                                   .
          .                                   .    +-------+        +-------+
          .                                   ...> |  TCP  | .....> |  FSM  |
          .                                   .    +-------+        +-------+
          .                  +------------+   .
          .            ....> |  StarTrek  |   .
          .            .     +------------+   .
          .            .                      .
          .     +------------+                .
          ....> |  StarGate  | ................
          .     +------------+
          .
          .
    +==========+
    |  SEChat  |
    +==========+
          .
          .
          .     +-------+        +--------+        +-------+
          ....> |  SDK  | .....> |  DIMP  | .....> |  DKD  |
          .     +-------+        +--------+        +-------+
          .                                            .
          .                                   ..........
          .                                   .
          .     +-----------+                 .    +-------+       +--------+
          ....> |  Plugins  | ...................> |  MKM  | ....> | Crypto |
          .     +-----------+                      +-------+       +--------+
          .     
          .     +-------+
          ....> |  LNC  |
          .     +-------+
          .     
          .     +-------+
          ....> |  DOS  |
                +-------+


```



Moky @ Jul 5. 2021
