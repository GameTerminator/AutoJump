package com.githang.autojump;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
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
        Scanner scan = new Scanner(System.in);
        LOG.info("准备");
        while (scan.next() != null) {
            LOG.info("截图中");
            long startTime = System.currentTimeMillis();
            BufferedImage image = helper.snapshot();
            LOG.info("截图耗时" + (System.currentTimeMillis() - startTime) + "ms");
            width = image.getWidth();
            height = image.getHeight();
            LOG.info("查找位置");
            Point from = null;
            long startCalcTime = System.currentTimeMillis();
            FINDING:
            for (int y = height * 11 / 28, endY = height * 4 / 5; y < endY; y++) {
                for (int x = width / 5, endX = width * 17 / 20; x < endX; x++) {
                    if (isJumpFrom(image, target, x, y)) {
                        final int fromX = x + bWidth / 2;
                        final int fromY = y + bHeight * 5;
                        from = new Point(fromX, fromY);
                        LOG.info("找到位置: " + fromX + ", " + fromY + " 耗时: "
                                + (System.currentTimeMillis() - startCalcTime) + "ms");
                        break FINDING;
                    }
                }
            }
            if (from == null) {
                failedTimes++;
                if (failedTimes >= 3) {
                    break;
                }
                helper.press(300, 300, 1);
                LOG.info("找不到，策略点一下");
            } else {
                final boolean isLeft = from.x > width / 2;
                final Point to = findTargetPoint(width, height, image, isLeft);
                if (isLeft) {
                    Point toLeftPoint = findLeftTargetLeftPoint(image, 0, to.x, to.y, from.y);
                    if (toLeftPoint != null) {
                        to.y = toLeftPoint.y;
                    } else {
                        to.y += bHeight;
                    }
                } else {
                    Point toRightPoint = findRightTargetRightPoint(image, width, to.x, to.y, from.y);
                    if (toRightPoint != null) {
                        to.y = toRightPoint.y;
                    } else {
                        to.y += bHeight;
                    }
                }
                LOG.info("跳到：" + to.x + ", " + to.y);
                // 俯视角度，19/11是测量出来的正方形中左边点到中心点的距离与顶部点到中心点的距离
                final int distance = (int) Math.sqrt(Math.pow(to.x - from.x, 2) + Math.pow((to.y - from.y) * 19 / 11, 2));
                final int time;
                if (distance < 60) {
                    time = 150 + (distance - 35) / 2;
                } else if (distance < 80) {
                    time = 160 + (distance - 60) * 2;
                } else if (distance < 90) {
                    time = 165 + (distance - 80);
                } else if (distance < 95) {
                    time = 165 + (distance - 80) * 2;
                } else if (distance < 100) {
                    time = (int) (distance * 1.8f);
                } else if (distance < 180) {
                    time = (int) (160 + (distance - 100) * 1.65f);
                } else if (distance < 190) {
                    time = (int) (160 + (distance - 100) * 1.65f);
                } else if (distance < 200) {
                    time = (int) (160 + (distance - 100) * 1.7f);
                } else if (distance < 220) {
                    time = (int) (170 + (distance - 100) * 1.65f);
                } else if (distance < 240) {
                    time = (int) (1.67 * distance);
                } else if (distance < 280) {
                    time = (int) (1.67 * distance);
                } else if (distance < 300) {
                    time = (int) (1.66 * distance);
                } else if (distance < 350) {
                    time = (int) (1.66 * distance);
                } else if (distance < 430) {
                    time = (int) (1.66 * distance);
                } else if (distance < 440) {
                    time = (int) (1.66 * distance);
                } else if (distance < 450) {
                    time = (int) (1.64 * distance);
                } else if (distance < 500) {
                    time = (int) (1.6 * distance + 25);
                } else if (distance < 550) {
                    time = (int) (1.57 * distance + 45);
                } else {
                    time = (int) (1.58 * distance + 25);
                }
                long calcTime = System.currentTimeMillis() - startCalcTime;
                LOG.info("距离：" + distance + "  按下时间：" + time + "ms" + " 分析总耗时: " + calcTime + "ms");
                helper.press(from.x, from.y, time);
                drawAssistantLineAndSave(image, from, to, distance, time);
                failedTimes = 0;
            }
            LOG.info("总耗时" + (System.currentTimeMillis() - startTime) + "ms");
        }
        helper.disconnect();
        System.exit(0);
    }

    private static Point findTargetPoint(int width, int height, BufferedImage image, boolean isLeft) {
        int offset = 20;
        int startX = isLeft ? width / 5 : (width / 2 + offset);
        int endX = isLeft ? width / 2 : width * 9 / 10;
        for (int y = height * 9 / 28, endY = height / 2; y < endY; y++) {
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
        LOG.warning("Target point not found!");
        return new Point(0, 0);
    }

    /**
     * 目标在右边，从右往左扫描，找到目标的平面的右边点，用于找出中心点 y 坐标。
     */
    private static Point findRightTargetRightPoint(BufferedImage image, int fromX, int toX, int fromY, int toY) {
        Point lastDifferentPoint = null;
        int pointCount = 0;
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
        int pointCount = 0;
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
        return Math.abs(color.getRed() - background.getRed()) > 4
                || Math.abs(color.getGreen() - background.getGreen()) > 4
                || Math.abs(color.getBlue() - background.getBlue()) > 4;
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
