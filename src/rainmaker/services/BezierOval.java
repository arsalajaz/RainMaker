package rainmaker.services;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.QuadCurve;

import java.util.ArrayList;

public class BezierOval extends Group {
    Ellipse oval;
    ArrayList<Point2D> randPointsOnOval = new ArrayList<>();
    ArrayList<Integer> angles = new ArrayList<>();
    ArrayList<QuadCurve> quadCurves = new ArrayList<>();

    public BezierOval(double radiusX, double radiusY) {
        oval = new Ellipse(radiusX, radiusY);
        oval.setFill(Color.WHITE);

        createShape();
    }

    private void createShape() {
        int startAngle = RandomGenerator.getRandomInt(0, 360);
        int offsetAngle = 72;
        int angle = startAngle;
        int prevAngle = -1;
        Point2D prevPoint = null;
        while (angle <= 360 + startAngle + offsetAngle) {

            double x = oval.getRadiusX() * Math.sin(Math.toRadians(angle));
            double y = oval.getRadiusY() * Math.cos(Math.toRadians(angle));

            Point2D currentPoint = new Point2D(x, y);

            randPointsOnOval.add(new Point2D(x, y));
            angles.add(angle);

            angle += RandomGenerator.getRandomInt(60,72);

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

            double angleControlInRadians =
                    Math.toRadians((angle1 + angle2) / 2);

            int offset = RandomGenerator.getRandomInt(10, 25);

            // x = (a + alpha) * cos(theta)
            double controlX =
                    (oval.getRadiusX() + offset) * Math.sin(angleControlInRadians);
            // y = (b + alpha) * sin(theta)
            double controlY =
                    (oval.getRadiusY() + offset) * Math.cos(angleControlInRadians);

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


    public void showControlPoints() {
        for (QuadCurve curve : quadCurves) {
            Circle circle = new Circle(curve.getControlX(), curve.getControlY(), 2);
            circle.setFill(Color.RED);
            getChildren().add(circle);
        }
    }
}
