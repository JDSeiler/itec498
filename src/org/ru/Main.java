package org.ru;

import org.ru.img.AbstractPixel;
import org.ru.img.ImgReader;
import org.ru.pso.objectives.ImageTranslation;
import org.ru.pso.objectives.ImageTranslationAndRotation;
import org.ru.pso.objectives.WavyParabola;
import org.ru.pso.PSO;
import org.ru.pso.PSOConfig;
import org.ru.pso.Solution;
import org.ru.pso.strategies.Placement;
import org.ru.pso.strategies.Topology;
import org.ru.vec.Vec2D;
import org.ru.vec.Vec3D;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        testImageComparison();
    }

    public static void testReadingImages() {
        ImgReader reader = new ImgReader("img");
        BufferedImage bmpImg = reader.getImage("orientation-check.bmp");
        System.out.println("BMP Image");
        System.out.printf("Size is %d by %d%n", bmpImg.getWidth(), bmpImg.getHeight());
        int[][][] pixels = ImgReader.convertToPixelGrid(bmpImg, 3);
        for (int[][] row : pixels) {
            for (int[] pixel : row) {
                System.out.print(Arrays.toString(pixel));
            }
            System.out.println();
        }
        List<AbstractPixel> apixels = ImgReader.convertToAbstractPixels(bmpImg, 1.0);
        System.out.println(apixels);
    }

    public static void testImageComparison() {
        ImgReader reader = new ImgReader("img");
        BufferedImage ref = reader.getImage("test_5/reference.bmp");
        BufferedImage candidate = reader.getImage("test_5/candidate.bmp");

        ImageTranslationAndRotation objectiveFunction = new ImageTranslationAndRotation(ref, candidate, true);

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
        System.out.println("Particle Locations:");
        pso.printSwarm();
        System.out.println("Best solution at end:");
        double[] solution = foundMinimum.solution().components();
        System.out.printf("Coordinates: %f, %f%n", solution[0], solution[1]);
        System.out.printf("Rotation in radians: %f%n", solution[2]);
        System.out.printf("Rotation in degrees mod 360: %f%n", (solution[2] * 180 / Math.PI) % 360);
    }

    public static void testWavyParabola() {
        PSOConfig<Vec2D> config = new PSOConfig<>(
                15,
                0.75,
                1.3,
                1.5,
                Topology.COMPLETE,
                Placement.RANDOM,
                15.0,
                new Vec2D(new double[]{100.0, 100.0})
        );
        PSO<Vec2D> pso = new PSO<>(config, WavyParabola::compute, Vec2D::new);
        System.out.println("Before:");
        pso.printSwarm();
        Solution<Vec2D> foundMinimum = pso.run();
        System.out.println("After:");
        pso.printSwarm();
        System.out.printf("Best solution at end: %s%n", foundMinimum);

    }
}
