package org.hkijena.deconvolve_rif;

import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.hkijena.deconvolve_rif.caches.TIFFImageCache;

import java.nio.file.Path;

public class DataInterface {

    private Path outputDirectory;
    private TIFFImageCache<UnsignedShortType> inputImage;
    private TIFFImageCache<FloatType> psfImage;
    private TIFFImageCache<FloatType> convolvedImage;
    private TIFFImageCache<FloatType> deconvolvedImage;

    public DataInterface(Path inputDirectory, Path outputDirectory) {
        this.outputDirectory = outputDirectory;
        inputImage = new TIFFImageCache<>(inputDirectory.resolve("in").resolve("data.tif"), new UnsignedShortType());
        psfImage = new TIFFImageCache<>(inputDirectory.resolve("psf").resolve("psf.tif"), new FloatType());
        convolvedImage = new TIFFImageCache<>(outputDirectory.resolve("convolved.tif"), new FloatType(), getInputImage());
        deconvolvedImage = new TIFFImageCache<>(outputDirectory.resolve("deconvolved.tif"), new FloatType(), getInputImage());
    }

    @Override
    public String toString() {
        return outputDirectory.toString();
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public TIFFImageCache<UnsignedShortType> getInputImage() {
        return inputImage;
    }

    public TIFFImageCache<FloatType> getPsfImage() {
        return psfImage;
    }

    public TIFFImageCache<FloatType> getDeconvolvedImage() {
        return deconvolvedImage;
    }

    public TIFFImageCache<FloatType> getConvolvedImage() {
        return convolvedImage;
    }
}
