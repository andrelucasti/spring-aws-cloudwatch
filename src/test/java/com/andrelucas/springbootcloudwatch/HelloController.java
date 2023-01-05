package com.andrelucas.springbootcloudwatch;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hello")
public class HelloController {

    @GetMapping
    public ResponseEntity<String> hello(){
        return ResponseEntity.ok("Hello ECS Application");
    }

    @PostMapping
    public ResponseEntity<String> helloWithName(@RequestBody final String name){
        return ResponseEntity.accepted().body("Hello ".concat(name));
    }
}
