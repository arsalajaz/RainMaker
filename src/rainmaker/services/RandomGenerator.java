package rainmaker.services;

import javafx.geometry.Point2D;

import java.util.Random;

public class RandomGenerator {
    private static Random random = new Random();

    public static int getRandomInt(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    public static double getRandomDouble(double min, double max) {
        return random.nextDouble() * (max - min) + min;
    }

    public static CoinSide flipCoin() {
        return random.nextBoolean() ? CoinSide.HEADS : CoinSide.TAILS;
    }

    public static Point2D generateRandomBetween(Point2D start, Point2D end) {
        return new Point2D(RandomGenerator.getRandomDouble(
                start.getX(), end.getX()),
                RandomGenerator.getRandomDouble(start.getY(), end.getY()));
    }

    public static Point2D getRandomPointAroundLine(Point2D start, Point2D end
            , double minDistance, double maxDistance, boolean aboveLine) {
        double x1 = start.getX();
        double y1 = start.getY();
        double x2 = end.getX();
        double y2 = end.getY();

        double distance = RandomGenerator.getRandomDouble(minDistance, maxDistance);
        double angle = Math.atan2(y2 - y1, x2 - x1);
        if (aboveLine) {
            angle += Math.PI / 2;
        } else {
            angle -= Math.PI / 2;
        }

        double x = x1 + distance * Math.cos(angle);
        double y = y1 + distance * Math.sin(angle);

        return new Point2D(x, y);
    }
}
