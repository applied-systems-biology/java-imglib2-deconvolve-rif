package org.hkijena.deconvolve_rif.tasks;

import com.github.dexecutor.core.task.Task;
import org.hkijena.deconvolve_rif.DataInterface;

public abstract class DAGTask extends Task<Integer, Integer> {

    private Integer tid;
    private DataInterface dataInterface;

    protected DAGTask(Integer tid, DataInterface dataInterface) {
        this.tid = tid;
        this.dataInterface = dataInterface;
    }

    public DataInterface getDataInterface() {
        return dataInterface;
    }

    public Integer getTid() {
        return tid;
    }

    public abstract void work();

    public Integer execute() {
        work();
        return getTid();
    }
}
