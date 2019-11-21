package org.hkijena.deconvolve_rif.tasks;

import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.convolution.Convolution;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;
import org.hkijena.deconvolve_rif.DataInterface;
import org.hkijena.deconvolve_rif.Filters;
import org.hkijena.deconvolve_rif.Main;

public class Convolve extends DAGTask {

    public Convolve(Integer tid, DataInterface dataInterface) {
        super(tid, dataInterface);
    }

    @Override
    public void work() {
        System.out.println("Running Convolve on " + getDataInterface().toString());

        final ImageJ ij = Main.IMAGEJ;
        Img<FloatType> psf = getDataInterface().getPsfImage().getOrCreate();
        Img<FloatType> img = ImageJFunctions.convertFloat(ImageJFunctions.wrap(getDataInterface().getInputImage().getOrCreate(), "img"));
        RandomAccessibleInterval<FloatType> convolved_ = ij.op().filter().convolve(img, psf);
        Img<FloatType> convolved = (new ArrayImgFactory<>(new FloatType())).create(Filters.getDimensions(img));
        Filters.copy(convolved_, convolved);

        getDataInterface().getConvolvedImage().set(convolved);
    }
}
