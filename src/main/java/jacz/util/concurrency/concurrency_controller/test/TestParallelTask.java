package jacz.util.concurrency.concurrency_controller.test;

import jacz.util.concurrency.task_executor.ParallelTask;

/**
 * Class description
 * <p/>
 * User: Admin<br>
 * Date: 09-may-2008<br>
 * Last Modified: 09-may-2008
 */
public class TestParallelTask implements ParallelTask {

    private String name;

    private int limit;

    private String concurrentActivity;

    public TestParallelTask(String name, int limit, String concurrentActivity) {
        this.name = name;
        this.limit = limit;
        this.concurrentActivity = concurrentActivity;
        System.out.println(name + " (" + concurrentActivity + "): ready to execute");
    }

    public void performTask() {
        System.out.println(name + " (" + concurrentActivity + "): starts executing");
        for (int i = 0; i < limit; i++) {
            for (int j = 0; j < limit; j++) {
                System.out.print("");
            }
        }
        System.out.println(name + " (" + concurrentActivity + "): ends");
    }
}
