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

import chat.dim.User;
import chat.dim.fsm.AutoMachine;
import chat.dim.fsm.BaseTransition;
import chat.dim.fsm.Context;
import chat.dim.startrek.StarGate;

/**
 *  Server state machine
 */
class StateMachine extends AutoMachine<StateMachine, BaseTransition<StateMachine>, ServerState> implements Context {

    private String sessionKey = null;

    StateMachine(Server server) {
        super(ServerState.DEFAULT);

        setDelegate(server);

        // init states
        setState(ServerState.getDefaultState());
        setState(ServerState.getConnectingState());
        setState(ServerState.getConnectedState());
        setState(ServerState.getHandshakingState());
        setState(ServerState.getRunningState());
        setState(ServerState.getErrorState());
    }

    @Override
    public StateMachine getContext() {
        return this;
    }

    private void setState(ServerState state) {
        addState(state.name, state);
    }

    @Override
    public ServerState getCurrentState() {
        ServerState state = super.getCurrentState();
        if (state == null) {
            state = getDefaultState();
        }
        return state;
    }

    Server getServer() {
        return (Server) getDelegate();
    }

    User getCurrentUser() {
        Server server = getServer();
        if (server == null) {
            return null;
        }
        return server.getCurrentUser();
    }

    StarGate.Status getStatus() {
        Server server = getServer();
        if (server == null) {
            return StarGate.Status.ERROR;
        }
        return server.getStatus();
    }

    String getSessionKey() {
        return sessionKey;
    }
    public void setSessionKey(String session) {
        sessionKey = session;
    }
}
