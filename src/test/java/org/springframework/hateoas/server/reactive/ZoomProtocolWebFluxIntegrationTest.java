/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.server.reactive;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.springframework.hateoas.IanaLinkRelations.*;
import static org.springframework.hateoas.MappingTestUtils.*;
import static org.springframework.hateoas.MediaTypes.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.ModelBuilder;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.MappingTestUtils.*;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.config.HypermediaWebTestClientConfigurer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Use {@link ModelBuilder} to demonstrate the ZOOM protocol.
 *
 * @author Greg Turnquist
 * @author Oliver Gierke
 */
public class ZoomProtocolWebFluxIntegrationTest {

	private ContextualMapper contextualMapper;

	private WebTestClient testClient;

	private static Map<Integer, Product> PRODUCTS;

	@BeforeEach
	void setUp() {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(TestConfig.class);
		ctx.refresh();

		HypermediaWebTestClientConfigurer configurer = ctx.getBean(HypermediaWebTestClientConfigurer.class);

		this.testClient = WebTestClient.bindToApplicationContext(ctx).build().mutateWith(configurer);
		this.contextualMapper = createMapper(getClass());

		PRODUCTS = new TreeMap<>();

		PRODUCTS.put(998, new Product("someValue", true, true));
		PRODUCTS.put(777, new Product("someValue", true, false));
		PRODUCTS.put(444, new Product("someValue", false, true));
		PRODUCTS.put(333, new Product("someValue", false, true));
		PRODUCTS.put(222, new Product("someValue", false, true));
		PRODUCTS.put(111, new Product("someValue", false, true));
		PRODUCTS.put(555, new Product("someValue", false, true));
		PRODUCTS.put(666, new Product("someValue", false, true));
	}

	@Test // #175 #864
	void modelBuilderCanAssembleZoomProtocol() throws Exception {

		String results = this.testClient.get().uri("/products").accept(HAL_JSON) //
				.exchange() //
				.expectStatus().isOk() //
				.expectHeader().contentType(HAL_JSON_VALUE) //
				.expectBody(String.class) //
				.returnResult().getResponseBody();

		assertThat(results).isEqualTo(contextualMapper.readFile("zoom-hypermedia.json"));
	}

	@RestController
	static class ProductController {

		LinkRelation favoriteProducts = LinkRelation.of("favorite products");
		LinkRelation purchasedProducts = LinkRelation.of("purchased products");

		@GetMapping("/products")
		public RepresentationModel<?> all() {

			List<EntityModel<Product>> products = PRODUCTS.keySet().stream() //
					.map(id -> EntityModel.of(PRODUCTS.get(id), Link.of("http://localhost/products/{id}").expand(id))) //
					.collect(Collectors.toList());

			ModelBuilder.ComplexBuilder builder = new ModelBuilder.ComplexBuilder();
			builder.link(linkTo(methodOn(ProductController.class).all()).withSelfRel());

			for (EntityModel<Product> productEntityModel : products) {

				if (productEntityModel.getContent().isFavorite()) {

					builder //
							.entity(favoriteProducts, productEntityModel) //
							.link(productEntityModel.getRequiredLink(SELF).withRel(favoriteProducts));
				}

				if (productEntityModel.getContent().isPurchased()) {

					builder //
							.entity(purchasedProducts, productEntityModel) //
							.link(productEntityModel.getRequiredLink(SELF).withRel(purchasedProducts));
				}
			}

			return builder.build();
		}
	}

	@Data
	@AllArgsConstructor
	static class Product {

		private String someProductProperty;
		@Getter(onMethod = @__({ @JsonIgnore })) private boolean favorite = false;
		@Getter(onMethod = @__({ @JsonIgnore })) private boolean purchased = false;
	}

	@Configuration
	@EnableWebFlux
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class TestConfig {

		@Bean
		ProductController employeeController() {
			return new ProductController();
		}

		@Bean
		ObjectMapper testMapper() {

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			return objectMapper;
		}
	}
}
