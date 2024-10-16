package cn.yiming1234.study.service;

import cn.yiming1234.study.util.MailUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ApiService {

    private final WebClient webClient;
    private final MailUtil mailUtil;

    @Autowired
    public ApiService(WebClient webClient, MailUtil mailUtil) {
        this.webClient = webClient;
        this.mailUtil = mailUtil;
    }

    /**
     * 发送GET请求获取{result}
     */
    public Mono<String> getResult() {
        return webClient.get()
                .uri("https://login.xuexi.cn/user/qrcode/generate")
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(responseMap -> {
                    Boolean success = (Boolean) responseMap.get("success");
                    if (success != null && success) {
                        String result = (String) responseMap.get("result");
                        log.info("result值为: {}", result);
                        return Mono.justOrEmpty(result);
                    } else {
                        String errorMsg = (String) responseMap.get("errorMsg");
                        return Mono.error(new RuntimeException("Error: " + errorMsg));
                    }
                });
    }

    /**
     * 发送POST请求获取{code}
     */
    public Mono<String> getCode(String qrCode) {

        String body = "qrCode=" + qrCode + "&goto=https%3A%2F%2Foa.xuexi.cn&pdmToken=";

        return webClient.post()
                .uri("https://login.xuexi.cn/login/login_with_qr")
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(responseMap -> {
                    Boolean success = (Boolean) responseMap.get("success");

                    if (success != null && success) {
                        String dataUrl = (String) responseMap.get("data");
                        log.info("dataUrl: {}", dataUrl);
                        if (dataUrl != null && dataUrl.contains("loginTmpCode=")) {
                            String loginTmpCode = dataUrl.substring(dataUrl.indexOf("loginTmpCode=") + 13);
                            log.info("Login successful! loginTmpCode: {}", loginTmpCode);
                            return Mono.justOrEmpty(loginTmpCode);
                        } else {
                            return Mono.error(new RuntimeException("Cannot find loginTmpCode parameter"));
                        }
                    } else {
                        String message = (String) responseMap.get("message");
                        return Mono.error(new RuntimeException("Error: " + message));
                    }
                });
    }

    /**
     * 发送GET请求获取{token}
     */
    public Mono<String> getToken(String loginTmpCode) {
        return webClient.get()
                .uri("https://pc-api.xuexi.cn/login/secure_check?code=" + loginTmpCode + "&state=" + UUID.randomUUID())
                .retrieve()
                .toBodilessEntity()
                .flatMap(responseEntity -> {
                    List<String> cookies = responseEntity.getHeaders().get("Set-Cookie");
                    if (cookies != null && !cookies.isEmpty()) {
                        for (String cookie : cookies) {
                            if (cookie.startsWith("token=")) {
                                String token = cookie.substring("token=".length(), cookie.indexOf(";"));
                                log.info("token: {}", token);
                                return Mono.justOrEmpty(token);
                            }
                        }
                    }
                    return Mono.error(new RuntimeException("Token not found in Set-Cookie headers"));
                })
                .onErrorResume(error -> {
                    log.error("Error retrieving token: ", error);
                    return Mono.error(error);
                });
    }

    /**
     * 发送GET请求获取总积分
     */
    public Mono<String> getTotalScore(String token) {
        return webClient.get()
                .uri("https://pc-proxy-api.xuexi.cn/delegate/score/get")
                .header("Cookie", "token=" + token)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(responseMap -> {
                    Integer code = (Integer) responseMap.get("code");
                    if (code != null && code == 200) {
                        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                        if (data != null) {
                            Double score = (Double) data.get("score");
                            log.info("目前总分: {}", score);
                            return Mono.justOrEmpty(String.valueOf(score));
                        }
                    }
                    return Mono.error(new RuntimeException("Error retrieving total score"));
                });
    }

    /**
     * 发送GET请求获取今日积分
     */
    public Mono<String> getTodayScore(String token) {
        return webClient.get()
                .uri("https://pc-proxy-api.xuexi.cn/delegate/score/days/listScoreProgress?sence=score&deviceType=2")
                .header("Cookie", "token=" + token)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(responseMap -> {
                    Integer code = (Integer) responseMap.get("code");
                    if (code != null && code == 200) {
                        Map<String, Object> data = (Map<String, Object>) responseMap.get("data");
                        if (data != null) {
                            List<Map<String, Object>> taskProgress = (List<Map<String, Object>>) data.get("taskProgress");
                            if (taskProgress != null) {
                                StringBuilder result = new StringBuilder();
                                for (Map<String, Object> task : taskProgress) {
                                    String title = (String) task.get("title");
                                    if ("我要选读文章".equals(title) || "我要视听学习".equals(title)) {
                                        Integer currentScore = (Integer) task.get("currentScore");
                                        Integer dayMaxScore = (Integer) task.get("dayMaxScore");
                                        result.append(title).append(": ")
                                                .append(currentScore).append("/")
                                                .append(dayMaxScore).append("\n");
                                    }
                                }
                                log.info("今日任务进度: \n{}", result.toString());
                                return Mono.justOrEmpty(result.toString());
                            }
                        }
                    }
                    return Mono.error(new RuntimeException("Error retrieving today score"));
                });
    }

    /**
     * 发送GET请求获取头条新闻
     */
    public Mono<List<String>> getArticle() {
        log.info("获取头条新闻");
        return webClient.get()
                .uri("https://www.xuexi.cn/lgdata/1crqb964p71.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .reduce(DataBuffer::write)
                .map(dataBuffer -> {
                    //解决返回json过大的问题
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    String responseBody = new String(bytes);

                    List<String> urls = new ArrayList<>();
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode root = objectMapper.readTree(responseBody);
                        if (root.isArray()) {
                            for (JsonNode item : root) {
                                if (item.has("url")) {
                                    String url = item.get("url").asText();
                                    urls.add(url);
                                    if (urls.size() >= 5) {
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.info("文章解析结果: {}", urls);
                    return urls;
                });
    }

    /**
     * 发送GET请求获取第一频道视频
     */
    public Mono<List<String>> getVideo() {
        log.info("获取第一频道");
        return webClient.get()
                .uri("https://www.xuexi.cn/lgdata/1novbsbi47k.json")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .reduce(DataBuffer::write)
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    String responseBody = new String(bytes);

                    List<String> urls = new ArrayList<>();
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        JsonNode root = objectMapper.readTree(responseBody);
                        if (root.isArray()) {
                            for (JsonNode item : root) {
                                if (item.has("url")) {
                                    String url = item.get("url").asText();
                                    urls.add(url);
                                    if (urls.size() >= 5) {
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.info("视频解析结果: {}", urls);
                    return urls;
                })
                .doOnError(error -> log.info("获取第一频道失败", error));
    }

}
