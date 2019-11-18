package org.hkijena.deconvolve_rif.caches;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import org.hkijena.deconvolve_rif.Main;
import org.hkijena.deconvolve_rif.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TIFFImageCache<T extends RealType<T> & NativeType<T>> {

    private Path file;
    private T imgDataType;
    private ImgFactory<T> factory;
    private long width;
    private long height;

    public TIFFImageCache(Path file, T imgDataType) {
        this.file = file;
        this.imgDataType = imgDataType;
        this.factory = new ArrayImgFactory<>(imgDataType);

        // Load width and height
        Img<T> referenceImage = Main.IMGOPENER.openImgs(file.toString(), imgDataType).get(0);
        this.width = referenceImage.dimension(0);
        this.height = referenceImage.dimension(1);
    }

    public TIFFImageCache(Path file, T imgDataType, TIFFImageCache<?> reference) {
        this.file = file;
        this.imgDataType = imgDataType;
        this.factory = new ArrayImgFactory<>(imgDataType);
        this.width = reference.width;
        this.height = reference.height;

        try {
            Utils.ensureDirectory(file.getParent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Img<T> getOrCreate() {
        if(Files.exists(file)) {
            return Main.IMGOPENER.openImgs(file.toString(), imgDataType).get(0);
        }
        else {
            return factory.create(getXSize(), getYSize());
        }
    }

    public void set(Img<T> img) {
        Utils.writeAsCompressedTIFF(img, file);
    }

    public long getXSize() {
        return width;
    }

    public long getYSize() {
        return height;
    }
}
