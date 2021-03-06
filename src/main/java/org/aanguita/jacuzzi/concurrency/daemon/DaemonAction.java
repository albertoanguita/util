package org.aanguita.jacuzzi.concurrency.daemon;

/**
 * Action that a daemon must perform to solve a state problem. Synchronization issues must be solved by the class implementing this interface
 */
public interface DaemonAction {

    /**
     * Performs an action to solve the state
     *
     * @return true if the state is now in the desired point, false otherwise. No exceptions must be raised.
     */
    boolean solveState();
}
