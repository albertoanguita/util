package jacz.util.AI.search;

import jacz.util.bool.SynchedBoolean;
import jacz.util.concurrency.ThreadUtil;
import jacz.util.concurrency.daemon.Daemon;
import jacz.util.concurrency.daemon.DaemonAction;
import jacz.util.concurrency.task_executor.SequentialTaskExecutor;
import jacz.util.concurrency.timer.SimpleTimerAction;
import jacz.util.concurrency.timer.Timer;

/**
 * State and goal must be mutable objects that are never re-assigned
 */
public class DynamicState<S, G> implements DaemonAction, SimpleTimerAction {

    public interface Transitions<S, G> {

        /**
         * @param state current state
         * @param goal  current goal
         * @return null if no transition required
         */
        Transition<S, G> getTransition(S state, G goal);
    }

    public interface Transition<S, G> {

        /**
         * @return null if synchronous, no active wait required. Non-null and positive if asynchronous and active
         * wait required, or unexpected result that requires waiting for a subsequent try
         */
        Long actOnState(DynamicState<S, G> dynamicState);
    }

    public interface TransitionWithEvents<S, G> extends Transition<S, G> {

        void beforeTransition(S state, G goal);

        void afterTransition(S state, G goal);
    }

    private static class TransitionWithEventsExecutor<S, G> implements Transition<S, G> {

        private final TransitionWithEvents<S, G> transitionWithEvents;

        public TransitionWithEventsExecutor(TransitionWithEvents<S, G> transitionWithEvents) {
            this.transitionWithEvents = transitionWithEvents;
        }

        @Override
        public Long actOnState(DynamicState<S, G> dynamicState) {
            transitionWithEvents.beforeTransition(dynamicState.state(), dynamicState.goal());
            Long result = transitionWithEvents.actOnState(dynamicState);
            transitionWithEvents.afterTransition(dynamicState.state(), dynamicState.goal());
            return result;
        }
    }

    protected S state;

    private G goal;

    private final Transitions<S, G> transitions;

    protected final Daemon daemon;

    protected final SequentialTaskExecutor hookExecutor;

    private final SynchedBoolean alive;

    public DynamicState(S initialState, G initialGoal, Transitions<S, G> transitions) {
        this.state = initialState;
        this.goal = initialGoal;
        this.transitions = transitions;
        daemon = new Daemon(this);
        hookExecutor = new SequentialTaskExecutor();
        alive = new SynchedBoolean(true);
    }

    public void setGeneralTimer(long millis) {
        // todo
    }

    public synchronized S state() {
        return state;
    }

    public synchronized G goal() {
        return goal;
    }

    public synchronized void setGoal(G newGoal) {
        if (!goal.equals(newGoal)) {
            goal = newGoal;
            daemon.stateChange();
        }
    }

    public synchronized boolean hasReachedGoal() {
        return transitions.getTransition(state(), goal()) != null;
    }

    @Override
    public boolean solveState() {
        if (alive.isValue()) {
            Transition<S, G> transition = transitions.getTransition(state(), goal());
            if (transition != null) {
                if (transition instanceof TransitionWithEvents) {
                    TransitionWithEvents<S, G> transitionWithEvents = (TransitionWithEvents<S, G>) transition;
                    transition = new TransitionWithEventsExecutor<>(transitionWithEvents);
                }
                Long wait = transition.actOnState(this);
                if (wait != null) {
                    ThreadUtil.safeSleep(wait);
                }
                return false;
            } else {
                // goal reached
                return true;
            }
        } else {
            // stopped
            return true;
        }
    }

    @Override
    public Long wakeUp(Timer timer) {
        // some reminder timer woke up
        daemon.stateChange();
        return null;
    }

    public void blockUntilStateIsSolved() {
        daemon.blockUntilStateIsSolved();
    }

    public synchronized void stop() {
        daemon.stop();
        hookExecutor.stopAndWaitForFinalization();
    }


}