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
        AdbHelper helper = new AdbHelper();
        helper.waitForConnection();
        int width;
        int height;
        BufferedImage target = ImageIO.read(new File("target.png"));
        int bWidth = target.getWidth();
        int bHeight = target.getHeight();
        int failedTimes = 0;
        while (failedTimes <= 3) {
            BufferedImage image = helper.snapshot();
            width = image.getWidth();
            height = image.getHeight();
            LOG.info("查找位置");
            final Point point = findTargetPoint(width, height, image);
            LOG.info("跳到：" + point.x + ", " + point.y);
            failedTimes++;
            FINDING:
            for (int y = height * 2 / 5, endY = height * 4 / 5; y < endY; y++) {
                for (int x = 0, endX = width - bWidth; x < endX; x++) {
                    if (isJumpFrom(image, target, x, y)) {
                        final int fromX = x + bWidth / 2;
                        final int fromY = y + bHeight * 3 / 2;
                        LOG.info("找到位置: " + fromX + ", " + fromY);
                        final int distance = (int) Math.sqrt(Math.pow(point.x - fromX, 2) + Math.pow(point.y - fromY, 2));
                        final int time;
                        final int min;
                        if (distance < 40) {
                            min = 260;
                        } else if (distance < 60) {
                            min = (int) (220 + distance * 1.2f);
                        } else if (distance < 90) {
                            min = 280 + (distance - 60) * 3 / 5;
                        } else if (distance < 120){
                            min = (int) (295 + (distance - 90) * 1.2f);
                        } else {
                            min = 330;
                        }
                        time = (int) Math.max(min, distance * (2.2 - distance / 4000f));
                        LOG.info("距离：" + distance + "  按下时间：" + time + "ms");
                        helper.press(fromX, fromY, time);
                        Thread.sleep(time);
                        failedTimes = 0;
                        break FINDING;
                    }
                }
            }
            Thread.sleep(2000);
        }
        helper.disconnect();
        System.exit(0);
    }

    private static Point findTargetPoint(int width, int height, BufferedImage image) {
        for (int y = height / 5, endY = height / 2; y < endY; y++) {
            Color background = new Color(image.getRGB(2, y - 5));
            int topStartX = -1;
            for (int x = 0; x < width; x++) {
                Color color = new Color(image.getRGB(x, y));
                if (Math.abs(color.getRed() - background.getRed()) > 10
                        || Math.abs(color.getGreen() - background.getGreen()) > 10
                        || Math.abs(color.getBlue() - background.getBlue()) > 10) {
                    if (topStartX == -1) {
                        topStartX = x;
                    }
                } else if (topStartX != -1) {
                    return new Point((topStartX + x - 1) / 2, y);
                }
            }
        }
        throw new RuntimeException("ToX not found!");
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
