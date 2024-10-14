package cn.yiming1234.study.service;

import cn.yiming1234.study.util.QRCodeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;
import reactor.core.publisher.Mono;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
public class MainService {

    private final ApiService apiService;
    private final View error;
    private String qrCodeResult;
    private boolean loginSuccessful = false;

    @Autowired
    public MainService(ApiService apiService, View error) {
        this.apiService = apiService;
        this.error = error;
    }

    public void checkQRCodeStatus() {
        if (loginSuccessful) {
            log.info("登录已成功，停止检查二维码状态");
            return;
        }
        log.info("开始检查二维码状态: {}", LocalDateTime.now());
        apiService.getCode(qrCodeResult).flatMap(loginTmpCode -> {
            log.info("二维码登录结果，loginTmpCode: {}", loginTmpCode);
            if (loginTmpCode != null) {
                log.info("二维码登录成功，code: {}", loginTmpCode);
                loginSuccessful = true;
                return apiService.getToken(loginTmpCode).doOnNext(token -> {
                    log.info("获取token成功，token: {}", token);
                    deleteExistingFiles();
                    // TODO 储存进数据库
                });
            }
            return Mono.empty();
        }).doOnError(error -> {
            String errorMessage = error.getMessage();
            if (errorMessage.contains("二维码已失效")) {
                log.error("二维码已失效，重新生成二维码");
            } else if (errorMessage.contains("扫码登录失败，请刷新重试或选择其他登录方式")) {
                log.info("正在等待扫码");
            } else {
                log.error("二维码登录失败", error);
            }
        }).doOnTerminate(() -> {
            if (!loginSuccessful) {
                Mono.delay(Duration.ofSeconds(3)).subscribe(ignore -> checkQRCodeStatus());
            }
        }).subscribe(
                code -> {
                    if (code != null) {
                        log.info("登录成功");
                    }
                },
                error -> log.info("正在等待扫码")
        );
    }

    public String QRCodeService() {
        log.info("应用启动，生成二维码: {}", LocalDateTime.now());
        String result = apiService.getResult().block();
        String url = "https://login.xuexi.cn/login/qrcommit?showmenu=false&appid=dingoankubyrfkttorhpou&code=" + result;
        log.info("url: {}", url);
        qrCodeResult = result;
        QRCodeUtil.generateQRCode(url);
        log.info("二维码已生成");
        checkQRCodeStatus();
        return url;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void deleteExistingFiles() {
        deleteFilesInDirectory("src/main/resources/static");
        deleteFilesInDirectory("target/classes/static");
    }

    private void deleteFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        if (file.delete()) {
                            log.info("{} has been deleted", file.getName());
                        } else {
                            log.error("Failed to delete {}", file.getName());
                        }
                    }
                }
            }
        } else {
            log.info("{} does not exist", directoryPath);
        }
    }

//    @EventListener(ApplicationReadyEvent.class)
//    @Scheduled(cron = "0 0 9 * * ?")
//    public void mainService() {
//        // TODO
//        // 从数据库获取token
//        // 请求测试，总积分，今日积分
//        // 过期发送邮件
//        // 没过期则直接使用
//        // DailyService 服务1
//        // DailyService 服务2
//        // 完成每日任务发送邮件
//    }
}
