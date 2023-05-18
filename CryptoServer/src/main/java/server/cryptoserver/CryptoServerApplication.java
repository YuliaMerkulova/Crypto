package server.cryptoserver;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@SpringBootApplication
@Configuration
public class CryptoServerApplication {

	@Bean
	MultipartConfigElement multipartConfigElement() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofMegabytes(3));
		factory.setMaxRequestSize(DataSize.ofMegabytes(3));
		return factory.createMultipartConfig();
	}

	public static void main(String[] args) {
		SpringApplication.run(CryptoServerApplication.class, args);
	}

}
