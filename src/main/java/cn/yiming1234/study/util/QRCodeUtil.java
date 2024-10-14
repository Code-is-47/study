package cn.yiming1234.study.util;

import cn.yiming1234.study.service.MainService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Component
@Slf4j
public class QRCodeUtil {

    private static final int DEFAULT_WIDTH = 50;
    private static final int DEFAULT_HEIGHT = 50;

    @Autowired
    private MainService mainService;

    /**
     * 生成二维码并保存到文件夹
     */
    public static void generateQRCode(String url) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BufferedImage image = null;

        File staticDirSrc = new File("src/main/resources/static");
        if (!staticDirSrc.exists()) {
            staticDirSrc.mkdir();
        }

        File staticDirTarget = new File("target/classes/static");
        if (!staticDirTarget.exists()) {
            staticDirTarget.mkdir();
        }

        File qrCodeFileSrc = new File(staticDirSrc, "qr_code.png");
        File qrCodeFileTarget = new File(staticDirTarget, "qr_code.png");

        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, DEFAULT_WIDTH, DEFAULT_HEIGHT);
            image = new BufferedImage(DEFAULT_WIDTH, DEFAULT_HEIGHT, BufferedImage.TYPE_INT_RGB);

            for (int y = 0; y < DEFAULT_HEIGHT; y++) {
                for (int x = 0; x < DEFAULT_WIDTH; x++) {
                    image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            ImageIO.write(image, "png", qrCodeFileSrc);
            ImageIO.write(image, "png", qrCodeFileTarget);
            log.info("QR code generated at: " + qrCodeFileSrc.getAbsolutePath());
            log.info("QR code generated at: " + qrCodeFileTarget.getAbsolutePath());

        } catch (WriterException e) {
            log.error("Failed to generate QR code: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to save or open QR code: " + e.getMessage());
            printQRCode(url);
        }
    }

    /**
     * 在控制台输出二维码，利用 ANSI 转义序列显示黑白颜色
     * 备选方案
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
