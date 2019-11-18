package org.hkijena.deconvolve_rif;

import io.scif.config.SCIFIOConfig;
import io.scif.formats.tiff.TiffCompression;
import io.scif.formats.tiff.TiffSaver;
import net.imglib2.img.Img;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {

    public static void ensureDirectory(Path path) throws IOException {
        if(!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    public static <T> void writeAsCompressedTIFF(Img<T> img, Path filename) {
        SCIFIOConfig config = new SCIFIOConfig();
        config.writerSetCompression("LZW");
        Main.IMGSAVER.saveImg(filename.toString(), img, config);
    }
}
