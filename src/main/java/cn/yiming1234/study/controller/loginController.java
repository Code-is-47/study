package cn.yiming1234.study.controller;

import cn.yiming1234.study.service.MainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Controller
@Slf4j
public class loginController {

    @Autowired
    private MainService mainService;

    /**
     * 后端传递二维码
     */
    @GetMapping("/login")
    public String login() {
        try {
            String qrCodeUrl = mainService.QRCodeService();
            log.info("QR code generated: {}", qrCodeUrl);
        } catch (Exception e) {
            log.error("Error generating QR code", e);
        }
        return "index";
    }

    /**
     * 前端生成二维码
     */
    @GetMapping("/qrcode")
    public Mono<Map<String, String>> getQRCode() {
        log.info("Received request, generating QR code...");
        return Mono.fromCallable(() -> {
            String qrCodeUrl = mainService.QRCodeService();
            Map<String, String> result = new HashMap<>();
            result.put("qrCodeUrl", qrCodeUrl);
            return result;
        });
    }
}
