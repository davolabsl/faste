package io.github.davolabsl.web.support;

import io.github.davolabsl.support.FasteResourceDetails;
import io.github.davolabsl.support.SwaggerResourceHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author - Shehara
 * @date - 1/28/2022
 */

@RestController
@RequestMapping("/faste")
public class FasteSwaggerResources {

    @Autowired
    private SwaggerResourceHandler swaggerResourceHandler;

    @GetMapping("/resources")
    public ResponseEntity<List<Map>> getAllResources() {
        List<Map> allEntityNames = swaggerResourceHandler.getAllEntityNames();
        return new ResponseEntity<>(allEntityNames, HttpStatus.OK);
    }

    @GetMapping("/contract/{resource}")
    public ResponseEntity<List<FasteResourceDetails>> getAllContracts(@PathVariable("resource") String resource) {
        List<FasteResourceDetails> response = swaggerResourceHandler.getHttpDetails(resource);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
