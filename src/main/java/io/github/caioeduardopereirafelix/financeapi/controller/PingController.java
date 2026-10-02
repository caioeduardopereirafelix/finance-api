package io.github.caioeduardopereirafelix.financeapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
public class PingController {

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body("ok");
    }
}
