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

import chat.dim.fsm.AutoMachine;
import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.Transition;
import chat.dim.sg.StarStatus;
import chat.dim.utils.Log;

public class ServerState extends State {

    public static final String DEFAULT     = "default";
    public static final String CONNECTING  = "connecting";
    public static final String CONNECTED   = "connected";
    public static final String HANDSHAKING = "handshaking";
    public static final String RUNNING     = "running";
    public static final String ERROR       = "error";

    public final String name;
    public Date time;

    ServerState(String name) {
        super();
        this.name = name;
        this.time = null;
    }

    @Override
    protected void onEnter(Machine machine) {
        // do nothing
        Log.info("onEnter: " + name + " state");
        this.time = new Date();
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

/**
 *  Server state machine
 */
class StateMachine extends AutoMachine<ServerState> {

    private Server server;

    StateMachine(Server server) {
        super(ServerState.DEFAULT);

        this.server = server;
        setDelegate(server);

        // add states
        addState(ServerState.DEFAULT, getDefaultState());
        addState(ServerState.CONNECTING, getConnectingState());
        addState(ServerState.CONNECTED, getConnectedState());
        addState(ServerState.HANDSHAKING, getHandshakingState());
        addState(ServerState.RUNNING, getRunningState());
        addState(ServerState.ERROR, getErrorState());
    }

    @Override
    public void stop() {
        super.stop();
        server = null;
    }

    //---- States

    private ServerState getDefaultState() {
        ServerState state = new ServerState(ServerState.DEFAULT);

        // target state: Connecting
        state.addTransition(new Transition(ServerState.CONNECTING) {
            @Override
            protected boolean evaluate(Machine machine) {
                if (server == null || server.getCurrentUser() == null) {
                    return false;
                }
                StarStatus status = server.getStatus();
                return status == StarStatus.Connecting || status == StarStatus.Connected;
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
                assert server != null && server.getCurrentUser() != null : "server/user error";
                StarStatus status = server.getStatus();
                return status == StarStatus.Connected;
            }
        });

        // target state: Error
        state.addTransition(new Transition(ServerState.ERROR) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status == StarStatus.Error;
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
                assert server != null : "server error";
                return server.getCurrentUser() != null;
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
                assert server != null : "server error";
                // when current user changed, the server will clear this session, so
                // if it's set again, it means handshake accepted
                return server.session != null;
            }
        });

        // target state: Connected
        state.addTransition(new Transition(ServerState.CONNECTED) {
            @Override
            protected boolean evaluate(Machine machine) {
                ServerState state = (ServerState) machine.getCurrentState();
                long expired = state.time.getTime() + 120 * 1000;
                long now = (new Date()).getTime();
                if (now < expired) {
                    return false;
                }
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status == StarStatus.Connected;
            }
        });

        // target state: Error
        state.addTransition(new Transition(ServerState.ERROR) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
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
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        // target state: Default
        state.addTransition(new Transition(ServerState.DEFAULT) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                // user switched?
                return server.session == null;
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
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status != StarStatus.Error;
            }
        });

        return state;
    }
}
