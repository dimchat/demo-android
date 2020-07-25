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

import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.Transition;

import chat.dim.stargate.StarStatus;

import chat.dim.utils.Log;

class ServerState extends State {

    public final String name;

    ServerState(String name) {
        super();
        this.name = name;
    }

    @Override
    protected void onEnter(Machine machine) {
        // do nothing
        Log.info("onEnter: " + name + " state");
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
public class StateMachine extends Machine implements Runnable {

    public static final String defaultState     = "default";
    public static final String connectingState  = "connecting";
    public static final String connectedState   = "connected";
    public static final String handshakingState = "handshaking";
    public static final String runningState     = "running";
    public static final String errorState       = "error";
    public static final String stoppedState     = "stopped";

    StateMachine() {
        this(defaultState);
    }

    private StateMachine(String defaultStateName) {
        super(defaultStateName);

        // add states
        addState(defaultState, getDefaultState());
        addState(connectingState, getConnectingState());
        addState(connectedState, getConnectedState());
        addState(handshakingState, getHandshakingState());
        addState(runningState, getRunningState());
        addState(errorState, getErrorState());
        addState(stoppedState, getStoppedState());
    }

    Server server = null;

    //---- Auto running

    private Thread thread = null;

    public void start() {
        super.start();

        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        thread.interrupt();

        super.stop();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (!isStopped()) {
            sleep(500);
            tick();
        }
    }

    private boolean isStopped() {
        ServerState state = (ServerState) getCurrentState();
        if (state == null) {
            return false;
        }
        return state.name.equals(StateMachine.stoppedState);
    }

    //---- States

    private State getDefaultState() {
        State state = new ServerState(defaultState);

        // target state: Connecting
        state.addTransition(new Transition(connectingState) {
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

    private State getConnectingState() {
        State state = new ServerState(connectingState);

        // target state: Connected
        state.addTransition(new Transition(connectedState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null && server.getCurrentUser() != null : "server/user error";
                StarStatus status = server.getStatus();
                return status == StarStatus.Connected;
            }
        });

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status == StarStatus.Error;
            }
        });

        return state;
    }

    private State getConnectedState() {
        State state = new ServerState(connectedState);

        // target state: Handshaking
        state.addTransition(new Transition(handshakingState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                return server.getCurrentUser() != null;
            }
        });

        return state;
    }

    private State getHandshakingState() {
        State state = new ServerState(handshakingState);

        // target state: Running
        state.addTransition(new Transition(runningState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                // when current user changed, the server will clear this session, so
                // if it's set again, it means handshake accepted
                return server.session != null;
            }
        });

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        return state;
    }

    private State getRunningState() {
        State state = new ServerState(runningState);

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        // target state: Default
        state.addTransition(new Transition(defaultState) {
            @Override
            protected boolean evaluate(Machine machine) {
                assert server != null : "server error";
                // user switched?
                return server.session == null;
            }
        });

        return state;
    }

    private State getErrorState() {
        State state = new ServerState(errorState);

        // target state: Default
        state.addTransition(new Transition(defaultState) {
            @Override
            protected boolean evaluate(Machine machine) {
                /*
                assert server != null : "server error";
                StarStatus status = server.getStatus();
                return status != StarStatus.Error;
                 */
                return true;
            }
        });

        return state;
    }

    private State getStoppedState() {
        State state = new ServerState(errorState);
        // TODO: add transition
        return state;
    }
}
