package cn.yiming1234.study.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailUtil {

    @Autowired
    private JavaMailSender mailSender;

    /**
     * 发送文本邮件
     */
    public void sendMail(String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("pleasurecruise@qq.com");
        message.setTo("pleasure@yiming1234.cn");
        message.setSubject("今日运行日志");
        message.setText("当前步骤运行结果：" + content);
        mailSender.send(message);
    }

}
