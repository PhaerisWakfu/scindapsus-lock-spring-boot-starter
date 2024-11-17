

# Scindapsus Log

[toc]


## 说明

- 日志脱敏是拦截日期打印前，并脱敏日志中的内容，不会影响打印对象的实际内容



## 开始使用

### 依赖

```xml
<dependency>
    <groupId>com.phaeris.scindapsus</groupId>
    <artifactId>scindapsus-log</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-logging</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
    <version>2.2.2.RELEASE</version>
</dependency>
```

### 日志脱敏

logback配置文件中添加配置

```xml
    <!--scindapsus日志脱敏-->
    <include resource="scindapsus-desensitize.xml"/>
    <!--开关,默认false-->
    <property scope="context" name="DesensitizeEnabled" value="true"/>
    <!--format支持:
        //用户id
        USER_ID,
        //中文名
        CHINESE_NAME,
        //身份证号
        ID_CARD,
        //座机号
        FIXED_PHONE,
        //手机号
        MOBILE_PHONE,
        //地址
        ADDRESS,
        //电子邮件
        EMAIL,
        //密码
        PASSWORD,
        //中国大陆车牌，包含普通车辆、新能源车辆
        CAR_LICENSE,
        //银行卡
        BANK_CARD
    -->
    <property scope="context" name="SensitiveDataKeys" value='[{"fieldName":"name","format":"CHINESE_NAME"}]'/>
```
#### TraceId 链路追踪
```
[%X{traceId:-}]
```


#### 使用

```java
@RestController
@RequestMapping("/log")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    @GetMapping
    public String test(Request request) {
        log.info("request is {}", JSON.toJSONString(request));
        log.info(JSON.toJSONString(request));
        return "ok";
    }

    public static class Request {

        private String name;

        private String age;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAge() {
            return age;
        }

        public void setAge(String age) {
            this.age = age;
        }
    }
}
```

```http
POST http://localhost:8080/scindapsus
Accept: application/json
Content-Type: application/json

{
  "name": "wyh",
  "age": "18"
}
```



