package org.hkijena.deconvolve_rif;

import com.github.dexecutor.core.DefaultDexecutor;
import com.github.dexecutor.core.DexecutorConfig;
import com.github.dexecutor.core.ExecutionConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.imagej.ImageJ;
import io.scif.img.ImgOpener;
import io.scif.img.ImgSaver;
import org.apache.commons.cli.*;
import org.hkijena.deconvolve_rif.tasks.*;

import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {

    public static ImgOpener IMGOPENER = new ImgOpener();
    public static ImgSaver IMGSAVER = new ImgSaver();
    public static ImageJ IMAGEJ = new ImageJ();

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input file path");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file");
        output.setRequired(true);
        options.addOption(output);

        Option threads = new Option("t", "threads", true, "number of threads");
        threads.setRequired(false);
        options.addOption(threads);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        Path inputFilePath = Paths.get(cmd.getOptionValue("input"));
        Path outputFilePath = Paths.get(cmd.getOptionValue("output"));

        int numThreads = 1;
        if(cmd.hasOption("threads")) {
            numThreads = Integer.parseInt(cmd.getOptionValue("threads"));
        }

        System.out.println("Running with " + numThreads + " threads");

        long startTime = System.currentTimeMillis();

        Map<String, VoxelSize> dataVoxelSizes = new HashMap<>();
        Map<String, VoxelSize> psfVoxelSizes = new HashMap<>();
        {
            Gson gson = (new GsonBuilder()).create();
            JsonObject obj = gson.fromJson(new String(Files.readAllBytes(inputFilePath.resolve("samples.json")), Charset.defaultCharset()), JsonObject.class);
            for(String key : obj.keySet()) {
                float vsData = obj.getAsJsonObject(key).get("voxel-size-sample-xyz").getAsFloat();
                float vsPSFXZ = obj.getAsJsonObject(key).getAsJsonObject("voxel-size-psf").getAsJsonPrimitive("xz").getAsFloat();
                float vsPSFY = obj.getAsJsonObject(key).getAsJsonObject("voxel-size-psf").getAsJsonPrimitive("y").getAsFloat();
                dataVoxelSizes.put(key, new VoxelSize(vsData, vsData, vsData));
                psfVoxelSizes.put(key, new VoxelSize(vsPSFXZ, vsPSFY, vsPSFXZ));
            }
        }

        // Load data interfaces
        List<DataInterface> dataInterfaces = new ArrayList<>();

        for(Path samplePath : Files.list(inputFilePath).filter(path -> Files.isDirectory(path)).collect(Collectors.toList())) {
            System.out.println("Generating data interface for " + samplePath.toString());
            String key = samplePath.getFileName().toString();
            DataInterface dataInterface = new DataInterface(samplePath, outputFilePath.resolve(samplePath.getFileName()), dataVoxelSizes.get(key), psfVoxelSizes.get(key));
            dataInterfaces.add(dataInterface);
        }

        // Generate main DAG
        Map<Integer, DAGTask> dagTasks = new HashMap<>();

        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        DexecutorConfig<Integer, Integer> dexecutorConfig = new DexecutorConfig<>(executorService, integer -> dagTasks.get(integer));
        DefaultDexecutor<Integer, Integer> dexecutor = new DefaultDexecutor<>(dexecutorConfig);

        for(DataInterface dataInterface : dataInterfaces) {
            int tid = dagTasks.size();
            DAGTask task = new Deconvolve(tid, dataInterface);
            dagTasks.put(tid, task);
            dexecutor.addIndependent(tid);
        }

        dexecutor.execute(ExecutionConfig.TERMINATING);

        System.out.println("Task finished.");

        long endTime = System.currentTimeMillis();
        long runtime = endTime - startTime;
        try(FileWriter writer = new FileWriter(outputFilePath.resolve("runtime.log").toFile())) {
            writer.write("" + runtime);
        }

        System.exit(0);
    }
}
