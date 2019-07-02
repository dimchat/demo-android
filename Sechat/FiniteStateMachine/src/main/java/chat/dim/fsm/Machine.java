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
package chat.dim.fsm;

import java.util.HashMap;
import java.util.Map;

public class Machine {

    private String defaultStateName;
    private State currentState;
    private Map<String, State> stateMap;

    enum Status {
        Stopped (0),
        Running(1),
        Paused(2);

        final int value;

        Status(int value) {
            this.value = value;
        }
    }
    private Status status;

    public Machine(String defaultStateName) {
        super();
        this.defaultStateName = defaultStateName;
        this.currentState = null;
        this.stateMap = new HashMap<>();
        this.status = Status.Stopped;
    }

    /**
     *  add state with transition(s)
     *
     * @param state - finite state
     */
    public void addState(State state, String name) {
        stateMap.put(name, state);
    }

    void changeState(String stateName) {
        // exit current state
        if (currentState != null) {
            currentState.onExit(this);
            currentState = null;
        }

        // enter new state
        State newState = stateMap.get(stateName);
        if (newState != null) {
            newState.onEnter(this);
            currentState = newState;
        }
    }

    /**
     *  start machine from default state
     */
    public void start() {
        assert status == Status.Stopped && currentState == null;
        changeState(defaultStateName);
        status = Status.Running;
    }

    /**
     *  stop machine and set current state to null
     */
    public void stop() {
        assert status != Status.Stopped && currentState != null;
        status = Status.Stopped;
        changeState(null);
    }

    /**
     *  pause machine, current state not change
     */
    public void pause() {
        assert status == Status.Running && currentState != null;
        status = Status.Paused;
        currentState.onPause(this);
    }

    /**
     *  resume machine with current state
     */
    public void resume() {
        assert status == Status.Paused && currentState != null;
        currentState.onResume(this);
        status = Status.Running;
    }

    /**
     *  Drive the machine running forward
     */
    public synchronized void tick() {
        if (status == Status.Running) {
            currentState.tick(this);
        }
    }
}
