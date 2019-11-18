package org.hkijena.deconvolve_rif;

import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.hkijena.deconvolve_rif.caches.TIFFImageCache;

import java.nio.file.Path;

public class DataInterface {

    private Path outputDirectory;
    private TIFFImageCache<UnsignedByteType> inputImage;
    private TIFFImageCache<UnsignedShortType> psfImage;
    private TIFFImageCache<FloatType> outputImage;
    private VoxelSize inputVoxelSize;
    private VoxelSize psfVoxelSize;

    public DataInterface(Path inputDirectory, Path outputDirectory, VoxelSize inputVoxelSize, VoxelSize psfVoxelSize) {
        this.outputDirectory = outputDirectory;
        inputImage = new TIFFImageCache<>(inputDirectory.resolve("in").resolve("data.tif"), new UnsignedByteType());
        psfImage = new TIFFImageCache<>(inputDirectory.resolve("psf").resolve("psf.tif"), new UnsignedShortType());
        outputImage = new TIFFImageCache<>(outputDirectory.resolve("deconvolved.tif"), new FloatType(), getInputImage());
        this.inputVoxelSize = inputVoxelSize;
        this.psfVoxelSize = psfVoxelSize;
    }

    @Override
    public String toString() {
        return outputDirectory.toString();
    }

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public TIFFImageCache<UnsignedByteType> getInputImage() {
        return inputImage;
    }

    public TIFFImageCache<UnsignedShortType> getPsfImage() {
        return psfImage;
    }

    public TIFFImageCache<FloatType> getOutputImage() {
        return outputImage;
    }

    public VoxelSize getInputVoxelSize() {
        return inputVoxelSize;
    }

    public VoxelSize getPsfVoxelSize() {
        return psfVoxelSize;
    }
}
