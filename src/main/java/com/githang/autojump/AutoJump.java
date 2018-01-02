package com.githang.autojump;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author 黄浩杭 (huanghaohang@parkingwang.com)
 */
public class AutoJump {
    private static final Logger LOG = Logger.getLogger("AutoJump");

    public static void main(String[] args) throws InterruptedException, IOException {
        new File("tmp").mkdir();
        AdbHelper helper = new AdbHelper();
        helper.waitForConnection();
        int width;
        int height;
        BufferedImage target = ImageIO.read(new File("target.png"));
        int bWidth = target.getWidth();
        int bHeight = target.getHeight();
        int failedTimes = 0;
        while (true) {
            BufferedImage image = helper.snapshot();
            width = image.getWidth();
            height = image.getHeight();
            LOG.info("查找位置");
            Point from = null;
            FINDING:
            for (int y = height * 2 / 5, endY = height * 4 / 5; y < endY; y++) {
                for (int x = 0, endX = width - bWidth; x < endX; x++) {
                    if (isJumpFrom(image, target, x, y)) {
                        final int fromX = x + bWidth / 2;
                        final int fromY = y + bHeight * 5;
                        from = new Point(fromX, fromY);
                        LOG.info("找到位置: " + fromX + ", " + fromY);
                        break FINDING;
                    }
                }
            }
            if (from == null) {
                failedTimes++;
                if (failedTimes >= 3) {
                    break;
                }
            } else {
                final boolean isLeft = from.x > width / 2;
                final Point to = findTargetPoint(width, height, image, isLeft);
                if (isLeft) {
                    Point toLeftPoint = findLeftTargetLeftPoint(image, 0, to.x, to.y, from.y);
                    if (toLeftPoint != null) {
                        to.y = toLeftPoint.y;
                    }
                } else {
                    Point toRightPoint = findRightTargetRightPoint(image, width, to.x, to.y, from.y);
                    if (toRightPoint != null) {
                        to.y = toRightPoint.y;
                    }
                }
                LOG.info("跳到：" + to.x + ", " + to.y);
                final int distance = (int) Math.sqrt(Math.pow(to.x - from.x, 2) + Math.pow(to.y - from.y, 2));
                final int time;
                if (distance < 80) {
                    time = (int) Math.max(200, distance * 2.099f);
                } else if (distance < 95) {
                    time = (int) Math.max(220, distance * 2.5f);
                } else if (distance < 100) {
                    time = (int) (distance * 2.4f);
                } else if (distance < 230) {
                    time = (int) (distance * 2.06f);
                } else if (distance < 250) {
                    time = (int) (distance * 2.02f);
                } else if (distance < 300) {
                    time = (int) (distance * 2.04f);
                } else if (distance < 320) {
                    time = (int) (distance * 2.055f);
                } else if (distance < 360){
                    time = (int) (distance * 2.05f);
                } else if (distance < 400){
                    time = (int) (distance * 2.035f);
                } else if (distance < 420){
                    time = (int) (distance * 2.02f);
                } else {
                    time = (int) (distance * 2.01f);
                }
                LOG.info("距离：" + distance + "  按下时间：" + time + "ms");
                helper.press(from.x, from.y, time);
                drawAssistantLineAndSave(image, from, to, distance, time);
                failedTimes = 0;
                Thread.sleep(time);
            }
            Thread.sleep(2000);
        }
        helper.disconnect();
        System.exit(0);
    }

    private static Point findTargetPoint(int width, int height, BufferedImage image, boolean isLeft) {
        int padding = 10;
        int startX = isLeft ? padding : width / 2 + padding;
        int endX = (isLeft ? width / 2 : width) - padding;
        for (int y = height / 5, endY = height / 2; y < endY; y++) {
            int x1 = -1;
            for (int x = startX; x < endX; x++) {
                Color background = new Color(image.getRGB(x - 2, y));
                Color color = new Color(image.getRGB(x, y));
                if (isDifferentColor(background, color)) {
                    x1 = x;
                    break;
                }
            }
            if (x1 != -1) {
                for (int x = endX; x > startX; x--) {
                    Color background = new Color(image.getRGB(x + 2, y));
                    Color color = new Color(image.getRGB(x, y));
                    if (isDifferentColor(background, color)) {
                        return new Point((x + x1) / 2, y);
                    }
                }
            }
        }
        throw new RuntimeException("Target point not found!");
    }

    /**
     * 目标在右边，从右往左扫描，找到目标的平面的右边点，用于找出中心点 y 坐标。
     */
    private static Point findRightTargetRightPoint(BufferedImage image, int fromX, int toX, int fromY, int toY) {
        Point lastDifferentPoint = null;
        int pointCount = 1;
        for (int y = fromY; y < toY; y++) {
            Color background = new Color(image.getRGB(fromX - 1, y));
            for (int x = fromX - 1; x > toX; x--) {
                Color color = new Color(image.getRGB(x - 1, y));
                if (isDifferentColor(background, new Color(image.getRGB(fromX - 1, y - 1)))) {
                    return new Point(fromX, y);
                }
                if (isDifferentColor(color, background)) {
                    if (lastDifferentPoint != null && lastDifferentPoint.x == x - 1) {
                        pointCount++;
                        if (pointCount > 3) {
                            return lastDifferentPoint;
                        }
                    } else {
                        lastDifferentPoint = new Point(x - 1, y);
                        pointCount = 1;
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static Point findLeftTargetLeftPoint(BufferedImage image, int fromX, int toX, int fromY, int toY) {
        Point lastDifferentPoint = null;
        int pointCount = 1;
        for (int y = fromY; y < toY; y++) {
            Color background = new Color(image.getRGB(fromX + 1, y));
            for (int x = fromX + 1; x < toX; x++) {
                Color color = new Color(image.getRGB(x + 1, y));
                if (isDifferentColor(color, background)) {
                    if (lastDifferentPoint != null && lastDifferentPoint.x == x + 1) {
                        pointCount++;
                        if (pointCount > 3) {
                            return lastDifferentPoint;
                        }
                    } else {
                        lastDifferentPoint = new Point(x + 1, y);
                        pointCount = 1;
                    }
                    break;
                }
            }
        }
        return null;
    }

    private static boolean isDifferentColor(Color background, Color color) {
        return Math.abs(color.getRed() - background.getRed()) > 5
                || Math.abs(color.getGreen() - background.getGreen()) > 5
                || Math.abs(color.getBlue() - background.getBlue()) > 5;
    }

    private static boolean isJumpFrom(BufferedImage source, BufferedImage target, int x, int y) {
        for (int i = 0, width = target.getWidth(); i < width; i++) {
            for (int j = 0, height = target.getHeight(); j < height; j++) {
                int colorValue = target.getRGB(i, j);
                if (colorValue == 0) {
                    continue;
                }
                int tempX = x + i;
                int tempY = y + j;
                Color targetColor = new Color(colorValue);
                Color sourceColor = new Color(source.getRGB(tempX, tempY));
                if (Math.abs(targetColor.getRed() - sourceColor.getRed()) > 10
                        || Math.abs(targetColor.getGreen() - sourceColor.getGreen()) > 10
                        || Math.abs(targetColor.getBlue() - sourceColor.getBlue()) > 10) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void drawAssistantLineAndSave(BufferedImage source, Point point1, Point point2, int distance, int time) throws IOException {
        long fileTimestamp = System.currentTimeMillis();
        ImageIO.write(source, "png", new File("tmp/" + fileTimestamp + "_0.png"));
        final int width = source.getWidth();
        final int height = source.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setData(source.getData());
        for (int i = 0; i < width; i++) {
            image.setRGB(i, point1.y, Color.RED.getRGB());
            image.setRGB(i, point2.y, Color.GREEN.getRGB());
        }
        for (int i = 0; i < height; i++) {
            image.setRGB(point1.x, i, Color.RED.getRGB());
            image.setRGB(point2.x, i, Color.GREEN.getRGB());
        }
        image.getGraphics().setFont(new Font("宋体", Font.PLAIN, 50));
        image.getGraphics().setColor(Color.BLACK);
        image.getGraphics().drawString("distance:" + distance + "   time:" + time, 20, height - 100);
        ImageIO.write(image, "png", new File("tmp/" + fileTimestamp + "_1.png"));
    }

    private static void clipTarget() throws IOException {
        BufferedImage image = ImageIO.read(new File("screen.png"));
        BufferedImage target = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 24; i++) {
            for (int j = 0; j < 24; j++) {
                target.setRGB(i, j, image.getRGB(212 + i, 620 + j));
            }
        }
        ImageIO.write(target, "png", new File("target.png"));
    }
}
