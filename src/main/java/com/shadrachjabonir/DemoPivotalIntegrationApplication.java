package com.shadrachjabonir;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler;
import org.springframework.integration.http.support.DefaultHttpHeaderMapper;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.MessageHandler;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;


@RestController
@EnableAutoConfiguration
@SpringBootApplication
@IntegrationComponentScan
public class DemoPivotalIntegrationApplication {

    @Autowired
    BankIntegration bankIntegration;

    public static void main(String[] args) {
        SpringApplication.run(DemoPivotalIntegrationApplication.class, args);
//		ConfigurableApplicationContext ctx = SpringApplication.run(DemoPivotalIntegrationApplication.class, args);
//		TempConverter converter = ctx.getBean(TempConverter.class);
//		System.out.println(converter.fahrenheitToCelcius(68.0f));
//		ctx.close();
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/uploadAccountData", produces = "application/json", consumes = "multipart/form-data")
    Account uploadAccountData(@RequestParam("file") MultipartFile file) {
        System.out.println("sini");
        Account res = null;
        try {
            File input = new File("D:\\programming\\demo-pivotal-integration\\upload\\"+file.getOriginalFilename());
            file.transferTo(input);
            JAXBContext jaxbContext = JAXBContext.newInstance(Account.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            Account account = (Account) jaxbUnmarshaller.unmarshal(input);
            Gson gson = new Gson();
            String json = gson.toJson(account).replace("json:","");
            String jsonRes = bankIntegration.sendToBankService(json);
            res = gson.fromJson(jsonRes,Account.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    @MessagingGateway
    public interface BankIntegration {
        @Gateway(requestChannel = "sendToBankService")
        String sendToBankService(String json);
    }

    @Bean
    public MessageHandler httpOutboundGateway(){

        try {
            URI uri = null;
            HeaderMapper<HttpHeaders> mapHeader = DefaultHttpHeaderMapper.outboundMapper();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            mapHeader.toHeaders(headers);
            uri = new URI("https://demo-pivotal-bankservice.cfapps.io/updateAccountBalance/");
            HttpRequestExecutingMessageHandler httpHandler = new HttpRequestExecutingMessageHandler(uri);
            httpHandler.setHeaderMapper(mapHeader);

            httpHandler.setExpectedResponseType(String.class);
            httpHandler.setHttpMethod(HttpMethod.POST);
//            httpHandler.setExtractPayload(true);
            return httpHandler;
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    @Bean
    public IntegrationFlow httpFlow() {
        return IntegrationFlows.from("sendToBankService")
                .enrichHeaders(s -> s.header("Content-Type", "application/json"))
                .handle(Http.outboundGateway("https://demo-pivotal-bankservice.cfapps.io/updateAccountBalance/")
                        .charset("UTF-8")
                        .httpMethod(HttpMethod.POST)
                        .expectedResponseType(String.class))
                .get();
    }

}

@XmlRootElement
class Account implements Serializable {

    private Long id;
    private String name;
    private String number;
    private Double amount;

    public Account() {
    }

    public Account(String name, String number, Double amount) {
        this.name = name;
        this.number = number;
        this.amount = amount;
    }

    @XmlElement
    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    @XmlElement
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlElement
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}