package cn.yiming1234.study.service;

import cn.yiming1234.study.util.MailUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
                .toBodilessEntity()  // 不需要处理响应体
                .flatMap(responseEntity -> {
                    // 获取响应头中的 "Set-Cookie"
                    List<String> cookies = responseEntity.getHeaders().get("Set-Cookie");
                    if (cookies != null && !cookies.isEmpty()) {
                        // 提取 token
                        for (String cookie : cookies) {
                            if (cookie.startsWith("token=")) {
                                String token = cookie.substring("token=".length(), cookie.indexOf(";"));
                                log.info("token: {}", token);
                                return Mono.justOrEmpty(token);  // 返回提取的 token
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

}
