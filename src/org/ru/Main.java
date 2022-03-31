package org.ru;

import org.ru.concurrent.ImageClassificationResult;
import org.ru.concurrent.ThreadableImageClassification;
import org.ru.drawing.PointCloud;
import org.ru.img.AbstractPixel;
import org.ru.img.ImgReader;
import org.ru.pso.objectives.ImageTranslationAndRotation;
import org.ru.pso.PSO;
import org.ru.pso.PSOConfig;
import org.ru.pso.Solution;
import org.ru.pso.strategies.Placement;
import org.ru.pso.strategies.Topology;
import org.ru.vec.Vec3D;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Starting PSO ===\n");
        long start = System.nanoTime();

        try {
            testPair();
        } catch(Exception e) {
            System.out.println("Something went wrong!");
            System.err.println(e);
        }

        System.out.println("\n=== PSO is complete ===\n");
        long end = System.nanoTime();
        double timeInMs = (end - start) / 1_000_000.0;
        double timeInSec = timeInMs / 1_000.0;
        System.out.printf("PSO took %.2f ms (%.5f s)", timeInMs, timeInSec);
    }

    public static void testPair() throws IOException {
        /*
        * Candidate 2:
        * - Reference 1
        * - Reference 2
        *
        * Candidate 5:
        * - Reference 6
        * - Reference 5
        *
        * Candidate 8:
        * - Reference 3
        * - Reference 8
        * */
        ImgReader reader = new ImgReader("img/moderate-tests");
        BufferedImage ref = reader.getImage("reference-8.bmp");
        BufferedImage cand = reader.getImage("candidate-8.bmp");

        ImageTranslationAndRotation objectiveFunction = new ImageTranslationAndRotation(ref, cand, false);

        PSOConfig<Vec3D> config = new PSOConfig<>(
                15,
                0.75,
                1.3,
                1.5,
                Topology.COMPLETE,
                Placement.RANDOM,
                5.0,
                new Vec3D(new double[]{10.0, 10.0, 3.0})
        );

        PSO<Vec3D> pso = new PSO<>(config, objectiveFunction::compute, Vec3D::new);
        Solution<Vec3D> foundMinimum = pso.run();
        System.out.printf("ref8 to cand8 was: %.05f%n", foundMinimum.fitnessScore());

        List<AbstractPixel> finalCandidatePoints = objectiveFunction.getLastSetOfCandidatePoints();
        List<AbstractPixel> referencePoints = objectiveFunction.getLastSetOfReferencePoints();

        PointCloud.drawDigitComparison(referencePoints, "ref8.txt", finalCandidatePoints, "cand8.txt");
    }

    public static void testDigitRecognition() throws InterruptedException, ExecutionException {
        ImgReader reader = new ImgReader("img/moderate-tests");

        // TODO: More thorough inspection of EVERYTHING. Something is no right with these fitness scores

        ArrayList<BufferedImage> references = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            references.add(reader.getImage(String.format("reference-%d.bmp", i)));
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);

        /*
        * It might feel kind of strange to put the candidate in the outer loop and then the reference.
        * Think of it this way:
        * 1. We have some candidate, we don't know what it is
        * 2. We're going to compare that candidate to all available reference images.
        *
        * Thus, we are trying to identify each candidate in turn. Then to identify each candidate we
        * select each reference in turn.
        *
        * Just remember this convention: The Candidate is ALWAYS ALWAYS ALWAYS the image that moves.
        * The reference image is ALWAYS stationary.
        * */

        for (int i = 0; i < 10; i++) {
            System.out.printf("Candidate: %d%n", i);
            BufferedImage cand = reader.getImage(String.format("candidate-%d.bmp", i));

            ArrayList<ThreadableImageClassification> comparisons = new ArrayList<>();
            ArrayList<ImageClassificationResult<Vec3D>> answers = new ArrayList<>();

            for (int j = 0; j < 10; j++) {
                BufferedImage ref = references.get(j);

                ThreadableImageClassification comparison = new ThreadableImageClassification(j, ref, i, cand);
                comparisons.add(comparison);
            }

            List<Future<ImageClassificationResult<Vec3D>>> futureResults = pool.invokeAll(comparisons);
            for (Future<ImageClassificationResult<Vec3D>> ans : futureResults) {
                answers.add(ans.get());
            }

            for (ImageClassificationResult<Vec3D> ans : answers) {
                System.out.printf(
                        "Similarity between %d and %d : %.5f%n",
                        ans.candidateDigit(),
                        ans.referenceDigit(),
                        ans.result().fitnessScore());
            }
        }

        pool.shutdown();
    }
}
