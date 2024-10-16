package cn.yiming1234.study.service;

import cn.yiming1234.study.entity.Token;
import cn.yiming1234.study.mapper.TokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DailyService {

    @Autowired
    private ApiService apiService;

    @Autowired
    private TokenMapper tokenMapper;


    private static WebDriver getWebDriver(Token token) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            System.setProperty("webdriver.chrome.driver", "C://Environment//chromedriver-win64//chromedriver.exe");
        } else if (os.contains("nix") || os.contains("nux")) {
            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
        } else {
            log.error("不支持的操作系统: {}", os);
            return null;
        }

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
        options.addArguments("Cookie=token=" + token.getToken());

        return new ChromeDriver(options);
    }

    /**
     * 每日学习
     */
    public void read() {
        log.info("开始执行阅读任务");

        Token token = tokenMapper.selectLatestToken();
        String firstUrl = apiService.getArticle().block().get(0);
        log.info("第一个文章URL: {}", firstUrl);

        WebDriver driver = getWebDriver(token);

        try {
            driver.get(firstUrl);
            long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

            int scrollStep = 100;
            int totalScrollTime = 144;
            int sleepTime = (totalScrollTime * 1000) / (int) Math.ceil(lastHeight / scrollStep);

            log.info("开始滚动，预计滑动时间: {} 秒", totalScrollTime);

            int currentScroll = 0;

            while (currentScroll < lastHeight) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, " + currentScroll + ");");
                Thread.sleep(sleepTime);
                currentScroll += scrollStep;
                lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

                log.info("当前滚动位置: {}", currentScroll);
            }

            log.info("滚动完成");

        } catch (InterruptedException e) {
            log.error("滑动过程中发生错误: {}", e.getMessage());
        } finally {
            driver.quit();
            log.info("浏览器已关闭");
            apiService.getTodayScore(token.getToken()).subscribe(
                    score -> log.info("今日积分: {}", score));
        }
    }

    // 视听
    // https://www.xuexi.cn/lgdata/1novbsbi47k.json
}
