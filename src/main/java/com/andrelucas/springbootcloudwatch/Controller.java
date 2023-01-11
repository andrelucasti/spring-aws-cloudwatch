package com.andrelucas.springbootcloudwatch;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class Controller {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @GetMapping("/hello")
    @ResponseBody
    public ResponseEntity<String> hello(@RequestBody final String name){
        var traceId = MDC.get("traceId");
        log.info("traceId: ".concat(traceId));

        return ResponseEntity.ok().body("Hello ".concat(name));
    }
}
