package rainmaker.services;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.util.ArrayList;

public class BezierOval extends Group {
    private final Ellipse oval;
    private final ArrayList<Point2D> randPointsOnOval = new ArrayList<>();
    private final ArrayList<Integer> angles = new ArrayList<>();
    private final ArrayList<QuadCurve> quadCurves = new ArrayList<>();

    private BezierOval(double radiusX, double radiusY) {
        oval = new Ellipse(radiusX, radiusY);
        //oval.setFill(Color.WHITE);
        //createShape();
    }


    private void createShape(int startAngle, int incrementMin,
                             int incrementMax, int overlapAngle,
                             int minOffsetFromOval, int maxOffsetFromOval,
                             boolean randomizeControlAngle) {
        //int startAngle = RandomGenerator.getRandomInt(0, 360);
        //int overlapAngle = overlap;
        int angle = startAngle;
        int prevAngle = -1;
        Point2D prevPoint = null;
        while (angle <= 360 + startAngle + overlapAngle) {

            double x = oval.getRadiusX() * Math.sin(Math.toRadians(angle));
            double y = oval.getRadiusY() * Math.cos(Math.toRadians(angle));

            Point2D currentPoint = new Point2D(x, y);

            randPointsOnOval.add(new Point2D(x, y));

            angles.add(angle);

            //angle += RandomGenerator.getRandomInt(60,72);
            angle += RandomGenerator.getRandomInt(incrementMin, incrementMax);


            if (prevPoint == null) {
                prevPoint = currentPoint;
                prevAngle = angle;
                continue;
            }

            int angle1 = angles.get(angles.size() - 1);
            int angle2 = angles.get(angles.size() - 2);

            QuadCurve curve = new QuadCurve();
            curve.setStartX(prevPoint.getX());
            curve.setStartY(prevPoint.getY());
            curve.setEndX(currentPoint.getX());
            curve.setEndY(currentPoint.getY());

            double angleControlInDegrees= randomizeControlAngle ?
                    RandomGenerator.getRandomDouble(angle1, angle2) :
                    (angle1 + angle2) / 2;

            double angleControlInRadians =
                    Math.toRadians(angleControlInDegrees);


            int offset = RandomGenerator.getRandomInt(minOffsetFromOval,
                    maxOffsetFromOval);


            // x = (a + alpha) * cos(theta)
            double controlX = (oval.getRadiusX() + offset) *
                    Math.sin(angleControlInRadians);
            // y = (b + alpha) * sin(theta)
            double controlY = (oval.getRadiusY() + offset) *
                    Math.cos(angleControlInRadians);

            prevPoint = currentPoint;
            prevAngle = angle;

            curve.setControlX(controlX);
            curve.setControlY(controlY);
            quadCurves.add(curve);
        }


        //this.shape = generateShape();
        getChildren().add(oval);
        getChildren().addAll(quadCurves);
    }


    public void setFill(Color color) {
        for (QuadCurve curve : quadCurves) {
            curve.setFill(color);
        }
        oval.setFill(color);
    }

    public void setStroke(Color color) {
        for (QuadCurve curve : quadCurves) {
            curve.setStroke(color);
        }
    }

    public void setStrokeWidth(double width) {
        for (QuadCurve curve : quadCurves) {
            curve.setStrokeWidth(width);
        }
    }

    public double getRadiusX() {
        return oval.getRadiusX();
    }

    public double getRadiusY() {
        return oval.getRadiusY();
    }


    public void showControlPoints() {
        for (QuadCurve curve : quadCurves) {
            Circle circle = new Circle(curve.getControlX(), curve.getControlY(), 2);
            circle.setFill(Color.RED);
            getChildren().add(circle);

            //draw a circle on the start and end points
            Circle circle1 = new Circle(curve.getStartX(), curve.getStartY(), 2);
            circle1.setFill(Color.GREEN);
            getChildren().add(circle1);

            Circle circle2 = new Circle(curve.getEndX(), curve.getEndY(), 2);
            circle2.setFill(Color.GREEN);
            getChildren().add(circle2);
        }
    }

    public static class Builder {
        private final double radiusX;
        private final double radiusY;
        private int startAngle = 0;
        private int overlapAngle = 0;
        private int angleIncrementMin = 72;
        private int angleIncrementMax = 72;
        private int minOffsetFromOval = 0;
        private int maxOffsetFromOval = 0;
        private boolean randomizeControlAngle = false;

        public Builder(double radiusX, double radiusY) {
            this.radiusX = radiusX;
            this.radiusY = radiusY;
        }

        public Builder setStartAngle(int startAngle) {
            this.startAngle = startAngle;
            return this;
        }

        public Builder setOverlapAngle(int overlapAngle) {
            this.overlapAngle = overlapAngle;
            return this;
        }

        public Builder setAngleIncrementMin(int min) {
            this.angleIncrementMin = min;
            return this;
        }

        public Builder setAngleIncrementMax(int angleIncrement) {
            this.angleIncrementMax = angleIncrement;
            return this;
        }

        public Builder setMinOffsetFromOval(int minOffsetFromOval) {
            this.minOffsetFromOval = minOffsetFromOval;
            return this;
        }

        public Builder setMaxOffsetFromOval(int maxOffsetFromOval) {
            this.maxOffsetFromOval = maxOffsetFromOval;
            return this;
        }

        public Builder setRandomizeControlAngle(boolean randomizeControlAngle) {
            this.randomizeControlAngle = randomizeControlAngle;
            return this;
        }


        public BezierOval build() {
            BezierOval bezierOval = new BezierOval(radiusX, radiusY);
            bezierOval.createShape(startAngle, angleIncrementMin,
                    angleIncrementMax, overlapAngle, minOffsetFromOval,
                    maxOffsetFromOval, randomizeControlAngle);

            return bezierOval;
        }
    }
}


