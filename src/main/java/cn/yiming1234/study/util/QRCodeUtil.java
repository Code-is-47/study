package cn.yiming1234.study.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
public class QRCodeUtil {

    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 50;

    /**
     * 生成二维码并保存到项目根目录下的 temp 文件夹
     */
    public static void generateQRCode(String url) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BufferedImage image = null;

        File tempDir = new File("temp");
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }

        File tempFile = new File(tempDir, "qr_code.png");
        if (tempFile.exists()) {
            tempFile.delete();
        }

        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            image = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < DEFAULT_HEIGHT; y++) {
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ImageIO.write(image, "png", tempFile);
            log.info("在IDEA中点击该链接 " + tempFile.getAbsolutePath());

            // Windows打开图片

        } catch (WriterException e) {
            log.error("生成二维码失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("保存或打开二维码失败: " + e.getMessage());
            printQRCode(url);
        }
    }

    /**
     * 在控制台输出二维码，利用 ANSI 转义序列显示黑白颜色
     */
    public static void printQRCode(String url) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, DEFAULT_WIDTH, DEFAULT_HEIGHT);

            for (int y = 0; y < DEFAULT_HEIGHT; y++) {
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    boolean isSet = bitMatrix.get(x, y);
                    if (isSet) {
                        System.out.print("\033[40m  \033[0m");
                    } else {
                        System.out.print("\033[47m  \033[0m");
                    }
                }
                System.out.println();
            }
        } catch (WriterException e) {
            log.error("生成二维码失败: " + e.getMessage());
        }
    }
}
