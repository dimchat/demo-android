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
package chat.dim.network;

import java.lang.ref.WeakReference;

import chat.dim.database.SocialNetworkDatabase;
import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.Transition;
import chat.dim.mkm.LocalUser;
import chat.dim.stargate.StarStatus;

class ServerStateMachine extends Machine {

    static final String defaultState     = "default";
    static final String connectingState  = "connecting";
    static final String connectedState   = "connected";
    static final String handshakingState = "handshaking";
    static final String runningState     = "running";
    static final String errorState       = "error";
    static final String stoppedState     = "stopped";

    WeakReference<Connection> connection = null;

    public ServerStateMachine() {
        this(defaultState);
    }

    public ServerStateMachine(String defaultStateName) {
        super(defaultStateName);

        // add states
        addState(defaultState, getDefaultState());
        addState(connectingState, getConnectingState());
        addState(connectedState, getConnectedState());
        addState(handshakingState, getHandshakingState());
        addState(runningState, getRunningState());
        addState(errorState, getErrorState());
        //addState(stoppedState, getStoppedState());
    }

    private Connection getConnection(Machine machine) {
        ServerStateMachine ssm = (ServerStateMachine)machine;
        return ssm.connection.get();
    }

    private Server getServer(Machine machine) {
        return getConnection(machine).server;
    }

    private LocalUser getUser(Machine machine) {
        return getServer(machine).currentUser;
    }

    abstract class ServerState extends State {

        public final String name;

        ServerState(String name) {
            super();
            this.name = name;
        }

        @Override
        protected void onExit(Machine machine) {
        }

        @Override
        protected void onPause(Machine machine) {
        }

        @Override
        protected void onResume(Machine machine) {
        }
    }

    private State getDefaultState() {
        State state = new ServerState(defaultState) {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
                System.out.println("onEnter: default state");
            }
        };

        // target state: Connecting
        state.addTransition(new Transition(connectingState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = getServer(machine);
                StarStatus status = server.getStatus();
                return status == StarStatus.Connecting || status == StarStatus.Connected;
            }
        });

        return state;
    }

    private State getConnectingState() {
        State state = new ServerState(connectingState) {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
                System.out.println("onEnter: connecting state");
            }
        };

        // target state: Connected
        state.addTransition(new Transition(connectedState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = getServer(machine);
                StarStatus status = server.getStatus();
                return status == StarStatus.Connected;
            }
        });

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = getServer(machine);
                StarStatus status = server.getStatus();
                return status == StarStatus.Error;
            }
        });

        return state;
    }

    private State getConnectedState() {
        State state = new ServerState(connectedState) {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
                System.out.println("onEnter: connected state");
            }
        };

        // target state: Handshaking
        state.addTransition(new Transition(handshakingState) {
            @Override
            protected boolean evaluate(Machine machine) {
                LocalUser user = SocialNetworkDatabase.getInstance().getCurrentUser();
                return user != null;
            }
        });

        return state;
    }

    private State getHandshakingState() {
        State state = new ServerState(handshakingState) {
            @Override
            protected void onEnter(Machine machine) {
                // start handshake
                System.out.println("onEnter: handshaking state");
                Connection connection = getConnection(machine);
                connection.handshake(null);
            }
        };

        // target state: Running
        state.addTransition(new Transition(runningState) {
            @Override
            protected boolean evaluate(Machine machine) {
                // when user logout/changed, the server's current user will be clear,
                // after handshake accepted with new session key, this will be set again,
                // so if it's not empty, it means handshake accepted
                return getUser(machine) != null;
            }
        });

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = getServer(machine);
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        return state;
    }

    private State getRunningState() {
        State state = new ServerState(runningState) {
            @Override
            protected void onEnter(Machine machine) {
                // TODO: send all packages waiting
                System.out.println("onEnter: running state");
            }
        };

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = getServer(machine);
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        // target state: Default
        state.addTransition(new Transition(defaultState) {
            @Override
            protected boolean evaluate(Machine machine) {
                // user switched?
                return getUser(machine) == null;
            }
        });

        return state;
    }

    private State getErrorState() {
        State state = new ServerState(errorState) {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
                System.out.println("onEnter: error state");
            }
        };

        // target state: Default
        state.addTransition(new Transition(defaultState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = getServer(machine);
                StarStatus status = server.getStatus();
                return status != StarStatus.Error;
            }
        });

        return state;
    }
}
