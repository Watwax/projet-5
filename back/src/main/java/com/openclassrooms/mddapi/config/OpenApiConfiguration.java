package com.openclassrooms.mddapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI description of the API, served by Swagger UI at {@code /api/swagger}.
 */
@Configuration
public class OpenApiConfiguration {

	private static final String BEARER = "bearerAuth";

	/**
	 * Declares the API metadata and the Bearer JWT scheme, so that protected routes can be
	 * called from Swagger UI with a token.
	 *
	 * @return the OpenAPI definition
	 */
	@Bean
	public OpenAPI mddOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("MDD API")
						.description("API du réseau social MDD (Monde de Dév) : utilisateurs, thèmes, articles et commentaires.")
						.version("0.0.1"))
				.addSecurityItem(new SecurityRequirement().addList(BEARER))
				.components(new Components()
						.addSecuritySchemes(BEARER, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
