/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.terenceproject.utils.rest;

/**
 *
 * @author pierpaolo
 */
public class Consistency {
    public static enum State { ERROR, FALSE, TRUE_STRICT, TRUE_RELAXED };
    private State state;

    /**
     *
     * @param s
     */
    public Consistency(State s) { state = s; }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isConsistent() { return !(state == State.FALSE); }

    public static Consistency ERROR() { return new Consistency(State.ERROR); }
    public static Consistency FALSE() { return new Consistency(State.FALSE); }
    public static Consistency TRUE_STRICT() { return new Consistency(State.TRUE_STRICT); }
    public static Consistency TRUE_RELAXED() { return new Consistency(State.TRUE_RELAXED); }
    
    @Override
    public String toString() {
        switch (state) {
            case FALSE        : return "false";
            case TRUE_STRICT  : return "true with strict mapping";
            case TRUE_RELAXED : return "true with relaxed mapping";
            case ERROR        : return "error";
            default           : return "error";
        }
    }
}
