package rainmaker.services;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import rainmaker.services.RandomGenerator;

import java.util.ArrayList;

public class BezierOval extends Group {
    private double radiusX;
    private double radiusY;
    private Shape shape;
    Ellipse oval;
    ArrayList<Point2D> randPointsOnOval = new ArrayList<>();
    ArrayList<Integer> angles = new ArrayList<>();
    ArrayList<QuadCurve> quadCurves = new ArrayList<>();

    public BezierOval(double radiusX, double radiusY) {
        oval = new Ellipse(radiusX, radiusY);
        oval.setFill(Color.WHITE);

        this.radiusX = radiusX;
        this.radiusY = radiusY;

        createShape();
    }

    private void createShape() {
        int startAngle = 0;
        int offsetAngle = RandomGenerator.getRandomInt(0, 100);
        int angle = startAngle;
        while (angle <= 360 + startAngle + offsetAngle) {
            int currentAngle = angle;
            if (angle >= 360 + startAngle + offsetAngle) {
                currentAngle = 360 + startAngle + offsetAngle;
            }

            double x = radiusX * Math.sin(Math.toRadians(currentAngle));
            double y = radiusY * Math.cos(Math.toRadians(currentAngle));

            randPointsOnOval.add(new Point2D(x, y));
            angles.add(currentAngle);
            angle += 72;


            if (randPointsOnOval.size() == 1) continue;

            Point2D curr = randPointsOnOval.get(randPointsOnOval.size() - 1);
            Point2D prev = randPointsOnOval.get(randPointsOnOval.size() - 2);

            int angle1 = angles.get(angles.size() - 1);
            int angle2 = angles.get(angles.size() - 2);

            QuadCurve curve = new QuadCurve();
            curve.setStartX(prev.getX());
            curve.setStartY(prev.getY());
            curve.setEndX(curr.getX());
            curve.setEndY(curr.getY());

            double angleControlInRadians =
                    Math.toRadians((angle1 + angle2) / 2);

            int offset = RandomGenerator.getRandomInt(5, 25);

            // x = (a + alpha) * cos(theta)
            double controlX =
                    (oval.getRadiusX() + offset) * Math.sin(angleControlInRadians);
            // y = (b + alpha) * sin(theta)
            double controlY =
                    (oval.getRadiusY() + offset) * Math.cos(angleControlInRadians);

            curve.setControlX(controlX);
            curve.setControlY(controlY);
            curve.setStroke(Color.BLACK);
            curve.setStrokeWidth(1);
            quadCurves.add(curve);
        }

        this.shape = generateShape();
        getChildren().addAll(this.shape);

    }

    public void setFill(Color color) {
        shape.setFill(color);
    }

    public void setStroke(Color color) {
        shape.setStroke(color);
    }

    public void setStrokeWidth(double width) {
        shape.setStrokeWidth(width);
    }

    private Shape generateShape() {
        //combine all quad curves into one path
        Path path = new Path();
        path.getElements().add(new MoveTo(randPointsOnOval.get(0).getX(), randPointsOnOval.get(0).getY()));
        for (QuadCurve curve : quadCurves) {
            path.getElements().add(new QuadCurveTo(curve.getControlX(), curve.getControlY(), curve.getEndX(), curve.getEndY()));
        }
        //path.getElements().add(new ClosePath());
        return path;
    }


    public void showControlPoints() {
        for (QuadCurve curve : quadCurves) {
            Circle circle = new Circle(curve.getControlX(), curve.getControlY(), 2);
            circle.setFill(Color.RED);
            getChildren().add(circle);
        }
    }
}
