package com.optum.me.service;

import com.optum.c360.security.AESCryptoException;
import com.optum.c360.security.AESUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.optum.me.exception.GenericException;
import com.optum.c360.streamsupport.utility.CommandUtility;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import com.optum.me.util.EmailUtil;
import org.springframework.mail.MailException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.FileReader;

@Component
@EnableAutoConfiguration
@EnableConfigurationProperties
public class HealthCheckService {

    Logger LOGGER = LoggerFactory.getLogger(HealthCheckService.class);

    private static final List<String> listOfExceptions =
            Arrays.asList(
                    "KafkaException",
                    "IOException",
                    "SSL handshake failed",
                    "NoHostAvailableException",
                    "ReadTimeoutException",
                    "WriteTimeoutException",
                    "UnavailableException",
                    "UnauthorizedException",
                    "AuthenticationException",
                    "InvalidTopicException",
                    "TopicAuthorizationException",
                    "BrokerNotAvailableException",
                    "SerializationException");

    @Autowired
    Environment env;

    @Autowired
    EmailUtil emailUtil;

    /**
     *
     * @throws AESCryptoException
     */
    public void notifyEvent() throws AESCryptoException {
        List<String> projectsList =  new ArrayList<>();
        try {
            FileReader reader = new FileReader("src/main/resources/OpenshiftProjectsInfo.txt");
            BufferedReader bufferedReader = new BufferedReader(reader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                projectsList.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(!CollectionUtils.isEmpty(projectsList)) {
            for (String project : projectsList) {
                String[] str = project.split(",");
                LOGGER.info("PROJECT NAME --> " + str[1]);
                String loginOpenshift = "oc login " + str[0] + " -u=" + str[2] + " -p=" + getDecryptedPassword(str[3]) + " --insecure-skip-tls-verify";
                runCommand(loginOpenshift);
                String selectProject = "oc project " + str[1];
                runCommand(selectProject);
                String podDetails = "oc get pods";
                List<String> podNames = runCommandForPod(podDetails);
                if(!CollectionUtils.isEmpty(podNames)) {
                    for (String podNameAndStatus : podNames) {
                        String podName = podNameAndStatus.split("\\|")[0];
                        String status = podNameAndStatus.split("\\|")[1];
                        try {
                            if(status!=null && !(status.equals("Running") || status.equals("ContainerCreating"))) {
                                LOGGER.info("ErrorPodName : " + podName + ", Status : " + status);
                                emailUtil.sendThrottledEmail(str[1], podName, status, "Status of the pod : ", str[4]);
                            } else {
                                String getPodLogs = "oc logs --since=30m " + podName;
                                String logs = runCommandToGetLogs(getPodLogs);
                                LOGGER.info("logs : " + logs);
                                if(Optional.ofNullable(logs).isPresent()) {
                                    for (String exception : listOfExceptions) {
                                        if(logs.contains(exception)){
                                            LOGGER.info("match : ");
                                            emailUtil.sendThrottledEmail(str[1], podName, exception, "Please find below exception : ", str[4]);
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (MailException mailException) {
                            mailException.printStackTrace();
                        }
                    }
                }
                String logoutOpenshift = "oc logout";
                runCommand(logoutOpenshift);
            }
        } else {
            LOGGER.info("Openshift projects list is empty");
        }
    }

    /**
     *
     * @param command
     * @return
     */
    public List<String> runCommand(String command) {
        List<String> podNames = new ArrayList<>();
        try {
            Process p = excutecommand(command);
            while (p.isAlive()) {
                // do nothing, just wait till the process is completed
            }
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                getErrorStream(p, command);
            }

        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            throw new GenericException("error while executing command", ex);
        }

        return podNames;
    }

    /**
     *
     * @param command
     * @return
     * @throws IOException
     */
    private Process excutecommand(String command) throws IOException {
        return CommandUtility.executeCommandOnOC(command);
    }

    /**
     *
     * @param command
     * @return
     */
    public List<String> runCommandForPod(String command) {
        List<String> podNames = new ArrayList<>();
        try {
            Process p = excutecommand(command);
            while (p.isAlive()) {
                // do nothing, just wait till the process is completed
            }
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                getErrorStream(p, command);
            }

            podNames = getInputStream(p);

        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            throw new GenericException("error while executing command", ex);
        }

        return podNames;
    }

    /**
     *
     * @param p
     * @return
     */
    public List<String> getInputStream(Process p) {
        List<String> podNames;
        try (BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            podNames = getPodNames(bri);
        } catch (IOException ex) {
            throw new GenericException("error while getting inputStream", ex);
        }
        return podNames;
    }

    /**
     *
     * @param bri
     * @return
     * @throws IOException
     */
    public List<String> getPodNames(BufferedReader bri) throws IOException {
        List<String> podNames = new ArrayList();
        String line;
        while ((line = bri.readLine()) != null) {
            String podName = Arrays.asList(line.split(" ")).get(0);
            String status = Arrays.asList(line.replaceAll("\\s+", " ").split(" ")).get(2);
            if(!"NAME".equalsIgnoreCase(podName)) {
                LOGGER.info("PodName : "+podName + ", Status : " + status);
                podNames.add(podName + "|" + status);
            }
        }
        return podNames;
    }

    /**
     *
     * @param p
     * @param command
     */
    public void getErrorStream(Process p, String command) {
        String line;
        try (BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream(),StandardCharsets.UTF_8))) {
            while ((line = bre.readLine()) != null) {
                LOGGER.error(line);
                if(line.contains("No such file or directory")|| line.contains("pod does not exist")){
                    return;
                }
            }
            throw new GenericException("unable to exceute"+ command);
        } catch (IOException ex) {
            throw new GenericException("Error Occured While getting ErrorStream",ex);
        }
    }

    /**
     *
     * @param command
     * @return
     */
    public String runCommandToGetLogs(String command) {
        String podNames = null;
        try {
            Process p = excutecommand(command);
            while (p.isAlive()) {
                // do nothing, just wait till the process is completed
            }
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                getErrorStream(p, command);
            }
            podNames = getLogsInputStream(p);

        } catch (IOException | IllegalArgumentException | SecurityException ex) {
            throw new GenericException("error while executing command", ex);
        }
        return podNames;
    }

    /**
     *
     * @param p
     * @return
     */
    public String getLogsInputStream(Process p) {
        String podNames;
        try (BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            podNames =  getLogs(bri);
        } catch (IOException ex) {
            throw new GenericException("error while getting inputStream", ex);
        }
        return podNames;
    }

    /**
     *
     * @param bri
     * @return
     * @throws IOException
     */
    public String getLogs(BufferedReader bri) throws IOException {
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = bri.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }

    /**
     *
     * @param pass
     * @return
     * @throws AESCryptoException
     */
    public String getDecryptedPassword(String pass) throws AESCryptoException {
        char[] passwordArray = AESUtilities.decrypt(this.env.getProperty("AES_KEY").toCharArray(), pass);
        return new String(passwordArray);
    }
}
