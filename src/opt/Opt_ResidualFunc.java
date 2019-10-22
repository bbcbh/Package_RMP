package opt;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import optimisation.AbstractResidualFunc;

public class Opt_ResidualFunc extends AbstractResidualFunc {

    File[] popFiles;
    File exportDir;
    ExecutorService executor = null;

    int NUM_STEPS = 360 * 50;
    int NUM_THREADS = Runtime.getRuntime().availableProcessors();

    private final File[] OPT_RES_DIR_COLLECTION;
    private final double[] OPT_RES_SUM_SQS;
    private final double[] TARGET_PREVAL;
    private final String[] propModelInitStr;

    public Opt_ResidualFunc(File[] popFiles, File exportDir,
            int numSteps, int NUM_THREADS,  double[] targetPreval,
            File[] OPT_RES_DIR_COLLECTION, double[] OPT_RES_SUM_SQS, 
            String[] propModelInitStr) {
        this.popFiles = popFiles;        
        this.exportDir = exportDir;
        this.NUM_STEPS = numSteps;
        this.NUM_THREADS = NUM_THREADS;        
        this.OPT_RES_DIR_COLLECTION = OPT_RES_DIR_COLLECTION;
        this.OPT_RES_SUM_SQS = OPT_RES_SUM_SQS;
        this.TARGET_PREVAL = targetPreval;
        this.propModelInitStr = propModelInitStr;
         
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public double[] generateResidual(double[] param) {
        double[] res = null;
        boolean hasPresetExe = executor != null;

        int numInExe = 0;

        File optOutputDir = null;

        if (OPT_RES_SUM_SQS.length > 0 && OPT_RES_DIR_COLLECTION.length > 0) {
            optOutputDir = new File(exportDir, Long.toString(System.currentTimeMillis()));

            // Skip until a new folder is generated.
            while (optOutputDir.exists()) {
                try {
                    Runtime.getRuntime().wait(1);
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
                optOutputDir = new File(exportDir, Long.toString(System.currentTimeMillis()));
            }

            optOutputDir.mkdirs();
        }

        Future<double[]>[] res_collection = new Future[popFiles.length];

        for (int i = 0; i < popFiles.length; i++) {
            if (executor == null) {
                executor = Executors.newFixedThreadPool(NUM_THREADS);
                numInExe = 0;
            }
            Callable_Opt_Prevalence opRun = new Callable_Opt_Prevalence(
                    optOutputDir, popFiles[i], i, NUM_STEPS, param, propModelInitStr); 
            
            
            opRun.setPrintOutput(popFiles.length == 1);
            opRun.setOutputAsFile(popFiles.length > 1);
            opRun.setTarget_preval(TARGET_PREVAL);

            Future<double[]> popOut = executor.submit(opRun);
            res_collection[i] = popOut;

            if (!hasPresetExe && numInExe == NUM_THREADS) {
                try {
                    executor.shutdown();
                    if (!executor.awaitTermination(2, TimeUnit.DAYS)) {
                        System.err.println("Inf Thread time-out!");
                    }
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.err);
                }
                executor = null;
            }
        }

        if (!hasPresetExe && executor != null) {
            try {
                executor.shutdown();
                if (!executor.awaitTermination(2, TimeUnit.DAYS)) {
                    System.err.println("Inf Thread time-out!");
                }
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
            executor = null;
        }

        for (int i = 0; i < popFiles.length; i++) {
            try {
                double[] res_single = res_collection[i].get();

                if (res == null) {
                    res = new double[res_single.length];
                }

                for (int r = 0; r < res.length; r++) {
                    res[r] += res_single[r] / popFiles.length;
                }

            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace(System.err);
            }
        }

        // Check if need to store this result
        if (res != null
                && OPT_RES_SUM_SQS.length > 0 && OPT_RES_DIR_COLLECTION.length > 0) {

            double resTotal = 0;
            File toBeClearDir = optOutputDir;

            for (int r = 0; r < res.length; r++) {
                resTotal += res[r] * res[r];
            }

            int insertIndex = Arrays.binarySearch(OPT_RES_SUM_SQS, resTotal);

            if (insertIndex < 0) {
                insertIndex = -(insertIndex + 1);
                if (insertIndex < OPT_RES_SUM_SQS.length) {
                    toBeClearDir = OPT_RES_DIR_COLLECTION[OPT_RES_DIR_COLLECTION.length - 1];
                    for (int kr = OPT_RES_SUM_SQS.length - 1; kr > insertIndex; kr--) {
                        OPT_RES_SUM_SQS[kr] = OPT_RES_SUM_SQS[kr - 1];
                        OPT_RES_DIR_COLLECTION[kr] = OPT_RES_DIR_COLLECTION[kr - 1];
                    }
                    OPT_RES_SUM_SQS[insertIndex] = resTotal;
                    OPT_RES_DIR_COLLECTION[insertIndex] = optOutputDir;
                }
            }

            if (toBeClearDir != null) {
                try {
                    Files.walkFileTree(toBeClearDir.toPath(), new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path t, BasicFileAttributes bfa) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path t, BasicFileAttributes bfa) throws IOException {
                            Files.delete(t);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path t, IOException ioe) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path t, IOException ioe) throws IOException {
                            Files.delete(t);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    //Files.delete(toBeClearDir.toPath());
                } catch (IOException ex) {
                    ex.printStackTrace(System.err);
                }
            }
        }

        return res;
    }

}
