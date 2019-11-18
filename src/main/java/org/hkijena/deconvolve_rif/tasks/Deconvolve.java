package org.hkijena.deconvolve_rif.tasks;

import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.logic.NativeBoolType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.hkijena.deconvolve_rif.DataInterface;
import org.hkijena.deconvolve_rif.Filters;

public class Deconvolve extends DAGTask {

    private float rifLambda = 0.001f;

    public Deconvolve(Integer tid, DataInterface dataInterface) {
        super(tid, dataInterface);
    }

    @Override
    public void work() {
        System.out.println("Running Deconvolve on " + getDataInterface().toString());
    }
}
