package com.rainbow.springboot.thymeleafdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@SpringBootApplication
public class ThymeleafdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThymeleafdemoApplication.class, args);
	}

	/**
	 * Required for Spring Security's SessionRegistry to be notified when an
	 * HttpSession is destroyed (e.g. on logout or timeout), so it can keep
	 * its bookkeeping accurate. Without this, expired/invalidated sessions
	 * could remain listed as "active" in the registry - see SecurityConfig's
	 * SessionRegistry bean and UserManagementServiceImpl, which uses the
	 * registry to force a user to re-authenticate immediately after their
	 * granted permissions change.
	 */
	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

}
