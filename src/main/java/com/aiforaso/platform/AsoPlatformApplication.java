package com.aiforaso.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import com.aiforaso.platform.config.AiProviderProperties;
import com.aiforaso.platform.config.MilvusProperties;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties({AiProviderProperties.class, MilvusProperties.class})
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class AsoPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AsoPlatformApplication.class, args);
    }
}
