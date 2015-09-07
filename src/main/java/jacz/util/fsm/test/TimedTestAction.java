package jacz.util.fsm.test;

import jacz.util.fsm.TimedFSMAction;

/**
 * Class description
 * <p/>
 * User: Alberto<br>
 * Date: 29-abr-2010<br>
 * Last Modified: 29-abr-2010
 */
public class TimedTestAction extends TestAction implements TimedFSMAction<String, Integer> {

    @Override
    public void timedOut(String currentState) {
        System.out.println("Time out!");
    }
}
