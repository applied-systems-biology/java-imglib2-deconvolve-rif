package org.hkijena.deconvolve_rif.tasks;

import deconvolution.algorithm.RegularizedInverseFilter;
import deconvolutionlab.Lab;
import org.hkijena.deconvolve_rif.DataInterface;
import signal.RealSignal;

public class DeconvolveWithDeconvolutionLab extends DAGTask {

    public DeconvolveWithDeconvolutionLab(Integer tid, DataInterface dataInterface) {
        super(tid, dataInterface);
    }

    @Override
    public void work() {
        RealSignal img = Lab.openFile(getDataInterface().getConvolvedImage().getFile().toString());
        RealSignal psf = Lab.openFile(getDataInterface().getPsfImage().getFile().toString());
        RegularizedInverseFilter rif = new RegularizedInverseFilter(0.001);
        RealSignal x = rif.run(img, psf);
        Lab.save(x, getDataInterface().getDeconvolvedImage().getFile().toString());
    }
}
