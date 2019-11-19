package org.hkijena.deconvolve_rif.tasks;

import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.algorithm.fft2.FFT;
import net.imglib2.algorithm.fft2.FFTConvolution;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Pair;
import org.hkijena.deconvolve_rif.DataInterface;
import org.hkijena.deconvolve_rif.Filters;
import org.hkijena.deconvolve_rif.Main;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Deconvolve extends DAGTask {

    private float rifLambda = 0.001f;
    private ExecutorService service = Executors.newFixedThreadPool(1);

    public Deconvolve(Integer tid, DataInterface dataInterface) {
        super(tid, dataInterface);
    }

    private double[] getPSFScale(Img<FloatType> img, Img<FloatType> psf) {
        long[] psfDims = Filters.getDimensions(psf);
        long[] imgDims = Filters.getDimensions(img);
        double[] result = new double[3];
        result[0] = (imgDims[0] * getDataInterface().getPsfVoxelSize().x / getDataInterface().getInputVoxelSize().x) / psfDims[0];
        result[1] = (imgDims[1] * getDataInterface().getPsfVoxelSize().z / getDataInterface().getInputVoxelSize().z) / psfDims[1];
        result[2] = (imgDims[2] * getDataInterface().getPsfVoxelSize().y / getDataInterface().getInputVoxelSize().y) / psfDims[2];
        return result;
    }

    private long[] getPSFShape(Img<FloatType> img, Img<FloatType> psf) {
        long[] result = new long[3];
        double[] scale = getPSFScale(img, psf);
        long[] dims = Filters.getDimensions(psf);
        for (int i = 0; i < 3; ++i) {
            result[i] = (long) (dims[i] * scale[i]);
        }
        return result;
    }

    private Img<ComplexFloatType> getLaplacianFFT(long[] fftDims) {
        Img<FloatType> kernel = (new ArrayImgFactory<>(new FloatType())).create(3,3,3);
        {
            Filters.setTo(kernel, new FloatType(1.0f / 26));
            RandomAccess<FloatType> access = kernel.randomAccess();
            access.setPosition(new long[]{ 1, 1, 1 });
            access.get().set(-1);
        }

        return Filters.fft(kernel, fftDims);
    }

    private Img<FloatType> getPSF(Img<FloatType> img) {
        final ImageJ ij = Main.IMAGEJ;
        Img<FloatType> psf = ImageJFunctions.convertFloat(ImageJFunctions.wrap(getDataInterface().getPsfImage().getOrCreate(), "psf"));
        // Resize PSF to match the image voxel size
        Img<FloatType> psfScaled = psf.factory().create(getPSFShape(img, psf));
        Filters.copy(ij.op().transform().scaleView(psf, getPSFScale(img, psf), new NLinearInterpolatorFactory<>()), psfScaled);
        return psfScaled;
    }

    private long[] getFFTDimensions(Img<FloatType> img, Img<FloatType> psf) {
        // Determine target dimensions
        long[] fftDims = new long[3];
        for(int i = 0; i < fftDims.length; ++i) {
            fftDims[i] = Math.max(Filters.getDimensions(img)[i], Filters.getDimensions(psf)[i]);
        }
        return fftDims;
    }

    @Override
    public void work() {
        final ImageJ ij = Main.IMAGEJ;
        System.out.println("Running Deconvolve on " + getDataInterface().toString());

        Img<FloatType> img = ImageJFunctions.convertFloat(ImageJFunctions.wrap(getDataInterface().getInputImage().getOrCreate(), "img"));
        Img<FloatType> psf = getPSF(img);

        // Transform into Fourier space
        long[] fftDims = getFFTDimensions(img, psf);
        Img<ComplexFloatType> imgFFT = Filters.fft(img, fftDims);
        Img<ComplexFloatType> psfFFT = Filters.fft(psf, fftDims);

        // Apply RIF
        // Adapted from DeconvolutionLab2 code
        // See https://github.com/Biomedical-Imaging-Group/DeconvolutionLab2/blob/master/src/main/java/deconvolution/algorithm/RegularizedInverseFilter.java
        Img<ComplexFloatType> Y = imgFFT;
        Img<ComplexFloatType> H = psfFFT;
        Img<ComplexFloatType> L = getLaplacianFFT(fftDims);
        Img<ComplexFloatType> X = Y.factory().create(Filters.getDimensions(Y));

        // Apply calculations
        {
            Cursor<ComplexFloatType> cY = Y.localizingCursor();
            RandomAccess<ComplexFloatType> cH = H.randomAccess();
            RandomAccess<ComplexFloatType> cL = L.randomAccess();
            RandomAccess<ComplexFloatType> cX = X.randomAccess();

            // Buffer variables
            ComplexFloatType H2 = new ComplexFloatType();
            ComplexFloatType L2 = new ComplexFloatType();
            ComplexFloatType FA = new ComplexFloatType();

            while(cY.hasNext()) {
                cY.fwd();
                cH.setPosition(cY);
                cL.setPosition(cY);
                cX.setPosition(cY);

                // H2 = H * H
                H2.setReal(cH.get().getRealDouble());
                H2.setImaginary(cH.get().getImaginaryDouble());
                H2.mul(cH.get());

                // L2 = L * lambda * L
                L2.setReal(cL.get().getRealDouble());
                L2.setImaginary(cL.get().getImaginaryDouble());
                L2.mul(rifLambda);
                L2.mul(cL.get());

                // FA = H2 + L2
                FA.setReal(H2.getRealDouble());
                FA.setImaginary(H2.getImaginaryDouble());
                FA.add(L2);

                // X = Y * H / FA
                cX.get().set(cY.get().copy());
                cX.get().mul(cH.get());
                cX.get().div(FA);
            }
        }

        // Inverse FFT
        Img<FloatType> deconvolved = img.factory().create(Filters.getDimensions(img));
        FFT.complexToRealUnpad(X, deconvolved);

        Filters.normalizeByMax(deconvolved);

        getDataInterface().getOutputImage().set(deconvolved);
    }
}
