package com.example.reactive;

import com.example.reactive.domain.entity.Person;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
class SpringCloudReactorDemoApplicationTests {

    @Test
    void contextLoads() {
    }

    /**
     * 创建WebClient
     */
    @Test
    public void Test1() {
        //初始化webclient 方式1
        //WebClient webClient = WebClient.create();

        //初始化webclient 方式2
        WebClient webClient = WebClient.builder().build();

        //构造URL-传参方式1
        webClient.get().uri("http://localhost:8081/account/{id}", 1);

        //多个参数传递
        webClient.get().uri("http://localhost:8081/account/{p1}/{p2}", "var1", "var2");

        //使用map进行赋值
        Map<String, Object> uri = new HashMap<>();
        uri.put("p1", "var1");
        uri.put("p2", "var2");
        webClient.get().uri("http://localhost:8081/account/{p1}/{p2}", uri);
    }

    /**
     * 发送Post raw json请求
     */
    @Test
    public void testPostRawJson() {
        Mono<String> resp = WebClient.create().post()
                .uri("http://localhost:8080/demo/json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject("{\n" +
                        "  \"title\" : \"this is title\",\n" +
                        "  \"author\" : \"this is author\"\n" +
                        "}"))
                .retrieve().bodyToMono(String.class);
        log.info("result:{}", resp.block());
    }


    /**
     * retrieve()方法
     * 获取响应主体的内容
     */
    @Test
    public void testRetrieve() {
        WebClient webClient = WebClient.builder().build();
        Mono<Object> objectMono = webClient.get().uri("http://localhost:8081/v1/account/{id}", 1).
                accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Object.class);
    }

    /**
     * exchange()方法已弃用,存在内存泄露的问题.高版本换成exchangeToMono()或者exchangeToFlux()
     * 主要是用来获取ClientResponse,可以获取响应的状态码 cookie 等等
     */
    @Test
    public void TestExchange() {
        WebClient webClient = WebClient.builder().build();
        Flux<Object> entityMono = webClient.get()
                .uri("/persons")
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToFlux(response -> {
                    if (response.statusCode().equals(HttpStatus.OK)) {
                        return response.bodyToFlux(Object.class);
                    } else {
                        return response.createException().flatMapMany(Mono::error);
                    }
                });
    }


    /**
     * 构建请求报文
     * 有两种方法body()或者bodyValue()
     */
    @Test
    public void TestBody() {
        //Mono<Person> objectMono = mybatis.select();
        Mono<Person> objectMono = null;
        WebClient webClient = WebClient.builder().build();
        Mono<Void> result = webClient.post().uri("/person").contentType(MediaType.APPLICATION_JSON).body(objectMono, Person.class).retrieve().bodyToMono(Void.class);

        //如果请求对象是一个实际值,不是一个Publisher(Mono/Flux) 对象则可以直接使用syncBody()作为一种快捷方式.
        //syncBody()被移除了,使用bodyValue()
        //Person person=mybatis.select();
        //Mono<Void> result = webClient.post().uri("/person").contentType(MediaType.APPLICATION_JSON).bodyValue(person).retrieve().bodyToMono(Void.class);
    }

    /**
     * webclient通过表单提交文本
     */
    @Test
    public void TestForm() {
        WebClient webClient = WebClient.builder().build();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", "noknok110");
        map.add("password", "666");
        Mono<String> stringMono = webClient.post().uri("/login").bodyValue(map).retrieve().bodyToMono(String.class);
    }

    /**
     * webclient通过表单提交多项part
     */
    @Test
    public void TestMultipartBody() {
        WebClient webClient = WebClient.builder().build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA); // 多部件表单体

        MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
        // ----------------- 表单 part
        multipartBodyBuilder.part("name", "KevinBlandy");
        /**
         * 每个表单项都有独立的header
         * 在这个表单项后额外添加了一个header
         */
        multipartBodyBuilder.part("skill", Arrays.asList("Java", "Python", "Javascript")).header("myHeader", "myHeaderVal");

        // ----------------- 文件 part
        // 从磁盘读取文件
        multipartBodyBuilder.part("file", new FileSystemResource(Paths.get("D:\\17979625.jpg")), MediaType.IMAGE_JPEG);
        // 从classpath读取文件
        multipartBodyBuilder.part("file", new ClassPathResource("app.log"), MediaType.TEXT_PLAIN);

        // ----------------- json part
        // json表单项
        multipartBodyBuilder.part("json", "{\"website\": \"SpringBoot中文社区\"}", MediaType.APPLICATION_JSON);

        // build完整的消息体
        MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();

        Mono<String> stringMono = webClient.post().uri("/login").bodyValue(multipartBody).retrieve().bodyToMono(String.class);
    }

    /**
     * post二进制--上传文件
     */
    @Test
    public void testUploadFile() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        HttpEntity<ClassPathResource> entity = new HttpEntity<>(new ClassPathResource("parallel.png"), headers);
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", entity);
        Mono<String> resp = WebClient.create().post()
                .uri("http://localhost:8080/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(parts))
                .retrieve().bodyToMono(String.class);
        log.info("result:{}", resp.block());
    }

    /**
     * 可以通过设置filter拦截器，统一修改拦截请求，
     * 比如认证的场景，如下示例，filter注册单个拦截器，filters可以注册多个拦截器，basicAuthentication是系统内置的用于basicAuth的拦截器，limitResponseSize是系统内置用于限制响值byte大小的拦截器
     */
    @Test
    public void TestFilter() {
        WebClient.builder()
                .baseUrl("http://www.kailing.pub")
                .filter((request, next) -> {
                    ClientRequest filtered = ClientRequest.from(request)
                            .header("foo", "bar")
                            .build();
                    return next.exchange(filtered);
                })
                .filters(filters -> {
                    filters.add(ExchangeFilterFunctions.basicAuthentication("username", "password"));
                    filters.add(ExchangeFilterFunctions.limitResponseSize(800));
                })
                .build().get()
                .uri("/article/index/arcid/{id}.html", 254)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(System.err::println);
    }

    /**
     * WebClient不支持websocket请求，请求websocket接口时需要使用WebSocketClient
     */
    @Test
    public void TestWebcosket() throws URISyntaxException {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        URI url = new URI("ws://localhost:8080/path");
        client.execute(url, session ->
                session.receive()
                        .doOnNext(System.out::println)
                        .then());
    }


    /**
     * 下载图片
     *
     * @throws IOException
     */
    @Test
    public void testDownloadImage() throws IOException {
        Mono<Resource> resp = WebClient.create().get()
                .uri("http://www.toolip.gr/captcha?complexity=99&size=60&length=9")
                .accept(MediaType.IMAGE_PNG)
                .retrieve().bodyToMono(Resource.class);
        Resource resource = resp.block();
        BufferedImage bufferedImage = ImageIO.read(resource.getInputStream());
        ImageIO.write(bufferedImage, "png", new File("captcha.png"));
    }

    /**
     * 下载文件
     *
     * @throws IOException
     */
    @Test
    public void testDownloadFile() throws IOException {
        Mono<ClientResponse> resp = WebClient.create().get()
                .uri("http://localhost:8080/file/download")
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(ClientResponse.class));
        ClientResponse response = resp.block();
        String disposition = response.headers().asHttpHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        String fileName = disposition.substring(disposition.indexOf("=") + 1);
        Resource resource = response.bodyToMono(Resource.class).block();
        File out = new File(fileName);
        FileUtils.copyInputStreamToFile(resource.getInputStream(), out);
        log.info(out.getAbsolutePath());
    }


    @Test
    public void testRetrieve4xx() {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.github.v3+json")
                .defaultHeader(HttpHeaders.USER_AGENT, "Spring 5 WebClient")
                .build();
        WebClient.ResponseSpec responseSpec = webClient.method(HttpMethod.GET)
                .uri("/user/repos?sort={sortField}&direction={sortDirection}",
                        "updated", "desc")
                .retrieve();
        Mono<String> mono = responseSpec
                .onStatus(e -> e.is4xxClientError(), resp -> {
                    log.error("error:{},msg:{}", resp.statusCode().value(), resp.statusCode().getReasonPhrase());
                    return Mono.error(new RuntimeException(resp.statusCode().value() + " : " + resp.statusCode().getReasonPhrase()));
                })
                .bodyToMono(String.class)
                .doOnError(WebClientResponseException.class, err -> {
                    log.info("ERROR status:{},msg:{}", err.getRawStatusCode(), err.getResponseBodyAsString());
                    throw new RuntimeException(err.getMessage());
                })
                .onErrorReturn("fallback");
        String result = mono.block();
        log.info("result:{}", result);
    }
}
