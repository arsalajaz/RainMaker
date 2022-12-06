package Helper;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

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
        showControlPoints();
    }

    private void createShape() {
        int angle = 0;
        while(angle <= 360 ) {
            double x = oval.getRadiusX() * Math.sin(Math.toRadians(angle));
            double y = oval.getRadiusY() * Math.cos(Math.toRadians(angle));

            randPointsOnOval.add(new Point2D(x,y));
            angles.add(angle);
            angle += RandomGenerator.getRandomInt(60, 72);

            if(randPointsOnOval.size() == 1) continue;

            Point2D curr = randPointsOnOval.get(randPointsOnOval.size()-1);
            Point2D prev = randPointsOnOval.get(randPointsOnOval.size()-2);

            int angle1 = angles.get(angles.size()-1);
            int angle2 = angles.get(angles.size()-2);

            QuadCurve curve = new QuadCurve();
            curve.setStartX(prev.getX());
            curve.setStartY(prev.getY());
            curve.setEndX(curr.getX());
            curve.setEndY(curr.getY());


//            double controlX =
//                    oval.getRadiusX() * Math.sin(Math.toRadians((angle1 + angle2) / 2));
//            double controlY =
//                    oval.getRadiusY() * Math.cos(Math.toRadians((angle1 + angle2) / 2));

            double controlX = (prev.getX() + curr.getX()) / 2;
            double controlY = (prev.getY() + curr.getY()) / 2;

            int randOffset = RandomGenerator.getRandomInt(10, 20);
            if(controlY > 0) {
                controlY += randOffset;
            } else {
                controlY -= randOffset;
            }
            if (controlX > 0) {
                controlX += randOffset;
            } else {
                controlX -= randOffset;
            }

            System.out.println("controlX: " + controlX + " controlY: " + controlY);


            curve.setControlX(controlX);
            curve.setControlY(controlY);
            curve.setStroke(Color.BLACK);
            curve.setStrokeWidth(1);
            curve.setFill(Color.WHITE);

            quadCurves.add(curve);
        }
        getChildren().add(oval);
        getChildren().addAll(quadCurves);


//        getElements().add(new MoveTo(randPointsOnOval.get(0).getX(), randPointsOnOval.get(0).getY()));
//        for(QuadCurve curve : quadCurves) {
//            getElements().add(new QuadCurveTo(curve.getControlX(), curve.getControlY(), curve.getEndX(), curve.getEndY()));
//        }
//        getElements().add(new ClosePath());
//
//        setFill(Color.RED);
    }

    public Shape getShape() {
        //combine all quad curves into one path
        Path path = new Path();
        path.getElements().add(new MoveTo(randPointsOnOval.get(0).getX(), randPointsOnOval.get(0).getY()));
        for(QuadCurve curve : quadCurves) {
            path.getElements().add(new QuadCurveTo(curve.getControlX(), curve.getControlY(), curve.getEndX(), curve.getEndY()));
        }
        path.getElements().add(new ClosePath());
        path.setFill(Color.RED);
        return path;
    }

    private void showControlPoints() {
        for(QuadCurve curve : quadCurves) {
            Circle circle = new Circle(curve.getControlX(), curve.getControlY(), 2);
            circle.setFill(Color.RED);
            getChildren().add(circle);
        }
    }
}

