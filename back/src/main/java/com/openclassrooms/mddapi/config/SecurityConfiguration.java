package com.openclassrooms.mddapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security of the API: stateless, every route requires authentication except the
 * authentication endpoints and the API documentation.
 * <p>
 * The JWT authentication filter is added by the authentication feature; until then every
 * protected route answers 401.
 */
@Configuration
public class SecurityConfiguration {

	private static final String[] PUBLIC_PATHS = {
			"/api/auth/**",
			"/api/swagger", "/api/swagger-ui/**",
			"/api/openapi", "/api/openapi/**"
	};

	/**
	 * Builds the security filter chain.
	 *
	 * @param http the Spring Security HTTP configuration
	 * @return the configured filter chain
	 * @throws Exception if the configuration cannot be built
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				// No cookie-based authentication: the JWT travels in the Authorization header.
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, ex) -> {
					response.setStatus(HttpStatus.UNAUTHORIZED.value());
					response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
					response.getWriter().write("{\"title\":\"Unauthorized\",\"status\":401,"
							+ "\"detail\":\"Authentification requise\",\"instance\":\"" + request.getRequestURI() + "\"}");
				}))
				.build();
	}

	/**
	 * Hashes passwords with BCrypt.
	 *
	 * @return the password encoder used for registration and login
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
