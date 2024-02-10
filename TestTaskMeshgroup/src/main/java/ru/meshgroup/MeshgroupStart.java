package ru.meshgroup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MeshgroupStart {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(MeshgroupStart.class, args);
        System.out.println("Hello World!!!");
    }
}
