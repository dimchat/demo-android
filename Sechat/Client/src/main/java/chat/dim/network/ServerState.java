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
package chat.dim.network;

import java.util.Date;

import chat.dim.fsm.BaseState;
import chat.dim.fsm.State;
import chat.dim.port.Docker;
import chat.dim.utils.Log;

public class ServerState extends BaseState<StateMachine, StateTransition> {

    public static final String DEFAULT     = "default";
    public static final String CONNECTING  = "connecting";
    public static final String CONNECTED   = "connected";
    public static final String HANDSHAKING = "handshaking";
    public static final String RUNNING     = "running";
    public static final String ERROR       = "error";

    public final String name;
    private Date enterTime;

    ServerState(String stateName) {
        super();
        name = stateName;
        enterTime = null;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof ServerState) {
            return ((ServerState) other).name.equals(name);
        } else if (other instanceof String) {
            return other.equals(name);
        } else {
            return false;
        }
    }
    public boolean equals(String other) {
        return name.equals(other);
    }

    @Override
    public void onEnter(State<StateMachine, StateTransition> previous,
                        StateMachine machine) {
        // do nothing
        Log.info("onEnter: " + name + " state");
        enterTime = new Date();
    }

    @Override
    public void onExit(State<StateMachine, StateTransition> next,
                       StateMachine machine) {
        enterTime = null;
    }

    @Override
    public void onPause(StateMachine machine) {
    }

    @Override
    public void onResume(StateMachine machine) {
    }

    //
    //  Factories
    //

    static ServerState getDefaultState() {
        ServerState state = new ServerState(DEFAULT);

        // Default -> Connecting
        state.addTransition(new StateTransition(CONNECTING) {
            @Override
            public boolean evaluate(StateMachine machine) {
                if (machine.getCurrentUser() == null) {
                    return false;
                }
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.PREPARING || status == Docker.Status.READY;
            }
        });

        return state;
    }

    static ServerState getConnectingState() {
        ServerState state = new ServerState(CONNECTING);

        // Connecting -> Connected
        state.addTransition(new StateTransition(CONNECTED) {
            @Override
            public boolean evaluate(StateMachine machine) {
                assert machine.getCurrentUser() != null : "server/user error";
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.READY;
            }
        });

        // Connecting -> Error
        state.addTransition(new StateTransition(ERROR) {
            @Override
            public boolean evaluate(StateMachine machine) {
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.ERROR;
            }
        });

        return state;
    }

    static ServerState getConnectedState() {
        ServerState state = new ServerState(CONNECTED);

        // Connected -> Handshaking
        state.addTransition(new StateTransition(HANDSHAKING) {
            @Override
            public boolean evaluate(StateMachine machine) {
                return machine.getCurrentUser() != null;
            }
        });

        // Connected -> Error
        state.addTransition(new StateTransition(ERROR) {
            @Override
            public boolean evaluate(StateMachine machine) {
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.ERROR;
            }
        });

        return state;
    }

    static ServerState getHandshakingState() {
        ServerState state = new ServerState(HANDSHAKING);

        // Handshaking -> Running
        state.addTransition(new StateTransition(RUNNING) {
            @Override
            public boolean evaluate(StateMachine machine) {
                // when current user changed, the server will clear this session, so
                // if it's set again, it means handshake accepted
                return machine.getSessionKey() != null;
            }
        });

        // Handshaking -> Connected
        state.addTransition(new StateTransition(CONNECTED) {
            @Override
            public boolean evaluate(StateMachine machine) {
                ServerState state = machine.getCurrentState();
                Date enterTime = state.enterTime;
                if (enterTime == null) {
                    // not enter yet
                    return false;
                }
                long expired = enterTime.getTime() + 16 * 1000;
                long now = (new Date()).getTime();
                if (now < expired) {
                    // not expired yet
                    return false;
                }
                // handshake expired, return to 'connect' to do it again
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.READY;
            }
        });

        // Handshaking -> Error
        state.addTransition(new StateTransition(ERROR) {
            @Override
            public boolean evaluate(StateMachine machine) {
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.ERROR;
            }
        });

        return state;
    }

    static ServerState getRunningState() {
        ServerState state = new ServerState(RUNNING);

        // Running -> Default
        state.addTransition(new StateTransition(DEFAULT) {
            @Override
            public boolean evaluate(StateMachine machine) {
                // user switched?
                return machine.getSessionKey() == null;
            }
        });

        // Running -> Error
        state.addTransition(new StateTransition(ERROR) {
            @Override
            public boolean evaluate(StateMachine machine) {
                Docker.Status status = machine.getStatus();
                return status == Docker.Status.ERROR;
            }
        });

        return state;
    }

    static ServerState getErrorState() {
        ServerState state = new ServerState(ERROR);

        // Error -> Default
        state.addTransition(new StateTransition(DEFAULT) {
            @Override
            public boolean evaluate(StateMachine machine) {
                Docker.Status status = machine.getStatus();
                return status != Docker.Status.ERROR;
            }
        });

        return state;
    }
}

