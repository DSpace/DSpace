package org.dspace.app.rest.example;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("example")
public class ExampleController {

    @RequestMapping("")
    public String test() {
        return "Hello world";
    }
}
