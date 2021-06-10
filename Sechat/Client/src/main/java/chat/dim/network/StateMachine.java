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

import chat.dim.User;
import chat.dim.fsm.AutoMachine;
import chat.dim.fsm.Machine;
import chat.dim.fsm.Transition;
import chat.dim.startrek.Gate;
import chat.dim.startrek.StarGate;

/**
 *  Server state machine
 */
class StateMachine extends AutoMachine<ServerState> {

    private String sessionKey = null;

    StateMachine(Server server) {
        super(ServerState.DEFAULT);

        setDelegate(server);

        // add states
        setState(getDefaultState());
        setState(getConnectingState());
        setState(getConnectedState());
        setState(getHandshakingState());
        setState(getRunningState());
        setState(getErrorState());
    }

    private void setState(ServerState state) {
        addState(state.name, state);
    }

    @Override
    public ServerState getCurrentState() {
        ServerState state = super.getCurrentState();
        if (state == null) {
            state = getState(ServerState.DEFAULT);
        }
        return state;
    }

    private Server getServer() {
        return (Server) getDelegate();
    }

    private User getCurrentUser() {
        Server server = getServer();
        if (server == null) {
            return null;
        }
        return server.getCurrentUser();
    }

    private StarGate.Status getStatus() {
        Server server = getServer();
        if (server == null) {
            return StarGate.Status.ERROR;
        }
        return server.getStatus();
    }

    private String getSessionKey() {
        return sessionKey;
    }
    public void setSessionKey(String session) {
        sessionKey = session;
    }

    //---- States

    private ServerState getDefaultState() {
        ServerState state = new ServerState(ServerState.DEFAULT);

        // target state: Connecting
        state.addTransition(new Transition(ServerState.CONNECTING) {
            @Override
            protected boolean evaluate(Machine machine) {
                if (getCurrentUser() == null) {
                    return false;
                }
                StarGate.Status status = getStatus();
                return status == StarGate.Status.CONNECTING || status == StarGate.Status.CONNECTED;
            }
        });

        return state;
    }

    private ServerState getConnectingState() {
        ServerState state = new ServerState(ServerState.CONNECTING);

        // target state: Connected
        state.addTransition(new Transition(ServerState.CONNECTED) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert getCurrentUser() != null : "server/user error";
                StarGate.Status status = getStatus();
                return status == StarGate.Status.CONNECTED;
            }
        });

        // target state: Error
        state.addTransition(new Transition(ServerState.ERROR) {
            @Override
            protected boolean evaluate(Machine machine) {
                StarGate.Status status = getStatus();
                return status == StarGate.Status.ERROR;
            }
        });

        return state;
    }

    private ServerState getConnectedState() {
        ServerState state = new ServerState(ServerState.CONNECTED);

        // target state: Handshaking
        state.addTransition(new Transition(ServerState.HANDSHAKING) {
            @Override
            protected boolean evaluate(Machine machine) {
                return getCurrentUser() != null;
            }
        });

        return state;
    }

    private ServerState getHandshakingState() {
        ServerState state = new ServerState(ServerState.HANDSHAKING);

        // target state: Running
        state.addTransition(new Transition(ServerState.RUNNING) {
            @Override
            protected boolean evaluate(Machine machine) {
                // when current user changed, the server will clear this session, so
                // if it's set again, it means handshake accepted
                return getSessionKey() != null;
            }
        });

        // target state: Connected
        state.addTransition(new Transition(ServerState.CONNECTED) {
            @Override
            protected boolean evaluate(Machine machine) {
                ServerState state = (ServerState) machine.getCurrentState();
                Date enterTime = state.enterTime;
                if (enterTime == null) {
                    // not enter yet
                    return false;
                }
                long expired = enterTime.getTime() + 120 * 1000;
                long now = (new Date()).getTime();
                if (now < expired) {
                    return false;
                }
                StarGate.Status status = getStatus();
                return status == StarGate.Status.CONNECTED;
            }
        });

        // target state: Error
        state.addTransition(new Transition(ServerState.ERROR) {
            @Override
            protected boolean evaluate(Machine machine) {
                StarGate.Status status = getStatus();
                return status != StarGate.Status.CONNECTED;
            }
        });

        return state;
    }

    private ServerState getRunningState() {
        ServerState state = new ServerState(ServerState.RUNNING);

        // target state: Error
        state.addTransition(new Transition(ServerState.ERROR) {
            @Override
            protected boolean evaluate(Machine machine) {
                StarGate.Status status = getStatus();
                return status != StarGate.Status.CONNECTED;
            }
        });

        // target state: Default
        state.addTransition(new Transition(ServerState.DEFAULT) {
            @Override
            protected boolean evaluate(Machine machine) {
                // user switched?
                return getSessionKey() == null;
            }
        });

        return state;
    }

    private ServerState getErrorState() {
        ServerState state = new ServerState(ServerState.ERROR);

        // target state: Default
        state.addTransition(new Transition(ServerState.DEFAULT) {
            @Override
            protected boolean evaluate(Machine machine) {
                StarGate.Status status = getStatus();
                return status != StarGate.Status.ERROR;
            }
        });

        return state;
    }
}
