package com.optum.me.util;

import javax.mail.internet.MimeMessage;
import com.optum.c360.configuration.email.EmailProperties;
import freemarker.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Configuration;

import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Component
public class EmailUtil {

    public static final String HTML_BREAK = "<br />";
    public static final String HTML_TAB = "&nbsp;&nbsp;&nbsp;&nbsp;";

    Logger LOGGER = LoggerFactory.getLogger(EmailUtil.class);

    public EmailUtil(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    @Autowired
    Environment env;

    EmailProperties emailProperties;

    @Autowired
    JavaMailSender emailSender;

    @Autowired
    freemarker.template.Configuration freemarkerConfig;

    /**
     *
     * @param openshiftProject
     * @param podName
     * @param status
     * @param errorMessage
     * @param toMail
     */
    public void sendThrottledEmail(String openshiftProject, String podName, String status, String errorMessage, String toMail) {
        Map<String, Object> content = new HashMap<>();
        try {
            LOGGER.info("*********** Composing Error Email *********** ");
            StringBuilder sb = new StringBuilder(250)
                    .append("Hello Team,")
                    .append(HTML_BREAK)
                    .append(HTML_BREAK)
                    .append("Error occurred in the Openshift Project :  ").append(openshiftProject).append(" and the Pod ").append(podName).append(" is down.")
                    .append(HTML_BREAK)
                    .append(HTML_BREAK)
                    .append(errorMessage)
                    .append(HTML_TAB)
                    .append("<pre>")
                    .append(status)
                    .append("</pre>")
                    .append("Please check respective openshift pod logs for more details.")
                    .append(HTML_BREAK)
                    .append(HTML_BREAK)
                    .append("Thanks,")
                    .append(HTML_BREAK);
            content.put("title", "Openshift Pods HealthCheck Error Notification");
            content.put("inlinetext", sb.toString());
            content.put("count", 0);
        } catch (RuntimeException e) {
            LOGGER.error("Unable to send email", e);
        }
        if (emailProperties.isEnabled()) {
            sendEmail("Openshift Pods HealthCheck Error Notification", content, toMail);
        }
    }

    /**
     *
     * @param subject
     * @param content
     * @param toMail
     */
    public void sendEmail(String subject, Map<String, Object> content, String toMail) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/");
            Template t = freemarkerConfig.getTemplate("templates/error-notification.txt");
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(t, content);
            helper.setTo(toMail);
            helper.setFrom(emailProperties.getFrom());
            //helper.setText(html, true);
            helper.setText((String)content.get("inlinetext"), true);
            helper.setSubject(subject);
            //helper.addInline("logo", new ClassPathResource("/templates/optum_logo.png"));
            //helper.addInline("seperator", new ClassPathResource("/templates/optum_seperator.png"));
            emailSender.send(message);
        } catch (Exception e) {
            LOGGER.error("Exception occurred in sendEmail ", e);
        }
    }
}
