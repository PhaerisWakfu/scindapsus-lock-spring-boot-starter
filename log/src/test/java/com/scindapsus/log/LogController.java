package com.scindapsus.log;

import brave.Tracer;
import brave.propagation.TraceIdContext;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author wyh
 * @since 2021/10/11
 */
@RestController
@RequestMapping("/log")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    @Autowired
    private Tracer tracer;

    @GetMapping
    public String test(Request request) {
        log.info("toJson {},haha", JSON.toJSONString(request));
        log.info(JSON.toJSONString(request));
        log.info("{}", tracer.currentSpan().context().traceIdString());
        return "ok";
    }

    @Data
    public static class Request {

        private String name;

        private String phone;

        private String idCard;

        private String email;
    }
}
