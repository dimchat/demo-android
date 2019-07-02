package chat.dim.common;

import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.Transition;
import chat.dim.mkm.User;
import chat.dim.stargate.StarStatus;

class ServerStateMachine extends Machine {

    static final String defaultState     = "default";
    static final String connectingState  = "connecting";
    static final String connectedState   = "connected";
    static final String handshakingState = "handshaking";
    static final String runningState     = "running";
    static final String errorState       = "error";
    static final String stoppedState     = "stopped";

    public Server server;
    public String session;

    public ServerStateMachine(String defaultStateName) {
        super(defaultStateName);
        server = null;
        session = null;

        // add states
        addState(defaultState, getDefaultState());
        addState(connectingState, getConnectingState());
    }

    abstract class ServerState extends State {

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
        State state = new ServerState() {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
            }
        };

        // target state: Connecting
        state.addTransition(new Transition(connectingState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                User user = server.currentUser;
                if (user == null) {
                    return false;
                }
                StarStatus status = server.getStatus();
                return status == StarStatus.Connecting || status == StarStatus.Connected;
            }
        });

        return state;
    }

    private State getConnectingState() {
        State state = new ServerState() {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
            }
        };

        // target state: Connected
        state.addTransition(new Transition(connectedState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                StarStatus status = server.getStatus();
                return status == StarStatus.Connected;
            }
        });

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                StarStatus status = server.getStatus();
                return status == StarStatus.Error;
            }
        });

        return state;
    }

    private State getConnectedState() {
        State state = new ServerState() {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
            }
        };

        // target state: Handshaking
        state.addTransition(new Transition(handshakingState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                User user = server.currentUser;
                return user != null;
            }
        });

        return state;
    }

    private State getHandshakingState() {
        State state = new ServerState() {
            @Override
            protected void onEnter(Machine machine) {
                // start handshake
                ServerStateMachine ssm = (ServerStateMachine) machine;
                Server server = ssm.server;
                String session = ssm.session;
                server.handshake(session);
            }
        };

        // target state: Running
        state.addTransition(new Transition(runningState) {
            @Override
            protected boolean evaluate(Machine machine) {
                // when current user changed, the server will clear this session, so
                // if it's set again, it means handshake accepted
                String session = ((ServerStateMachine) machine).session;
                return session != null;
            }
        });

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        return state;
    }

    private State getRunningState() {
        State state = new ServerState() {
            @Override
            protected void onEnter(Machine machine) {
                // TODO: send all packages waiting
            }
        };

        // target state: Error
        state.addTransition(new Transition(errorState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                StarStatus status = server.getStatus();
                return status != StarStatus.Connected;
            }
        });

        // target state: Default
        state.addTransition(new Transition(defaultState) {
            @Override
            protected boolean evaluate(Machine machine) {
                String session = ((ServerStateMachine) machine).session;
                // user switched?
                return session == null;
            }
        });

        return state;
    }

    private State getErrorState() {
        State state = new ServerState() {
            @Override
            protected void onEnter(Machine machine) {
                // do nothing
            }
        };

        // target state: Default
        state.addTransition(new Transition(defaultState) {
            @Override
            protected boolean evaluate(Machine machine) {
                Server server = ((ServerStateMachine) machine).server;
                StarStatus status = server.getStatus();
                return status != StarStatus.Error;
            }
        });

        return state;
    }
}
