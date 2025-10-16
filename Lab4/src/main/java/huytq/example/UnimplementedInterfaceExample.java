package huytq.example;

import java.util.logging.Logger;

public class UnimplementedInterfaceExample implements Drawable {

    private static final Logger logger = Logger.getLogger(UnimplementedInterfaceExample.class.getName());
    private final double radius;

    public UnimplementedInterfaceExample(double radius) {
        this.radius = radius;
    }

    @Override
    public void draw() {
        logger.info(() -> String.format("Drawing circle with radius %.2f", radius));
    }

    public double getRadius() {
        return radius;
    }
}
