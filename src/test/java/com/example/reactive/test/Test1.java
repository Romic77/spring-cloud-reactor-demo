package com.example.reactive.test;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author romic
 * @date 2022-08-06
 * @description
 */
@SpringBootTest
public class Test1 {
    Flux<String> helloworld = Flux.just("heelo", "world");

    @Test
    public void testStepVerifier() {
        StepVerifier.create(helloworld).expectNext("helllo").expectNext("world").expectComplete().verify();
    }

    @Test
    public void testStepVerifierException() {
        Flux<String> helloException = helloworld.concatWith(Mono.error(new IllegalArgumentException("exception")));

        StepVerifier.create(helloException).expectNext("helllo").expectNext("world").expectErrorMessage("exception").verify();
    }
}
