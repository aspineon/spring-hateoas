/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.hateoas.server.mvc;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.springframework.hateoas.MappingTestUtils.*;
import static org.springframework.hateoas.MediaTypes.*;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.ModelBuilder;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class ModelBuilderWebMvcIntegrationTest {

	private @Autowired WebApplicationContext context;
	private MockMvc mockMvc;
	private ContextualMapper contextualMapper;

	@BeforeEach
	public void setUp() {

		this.mockMvc = webAppContextSetup(this.context).build();
		this.contextualMapper = createMapper(getClass());

	}

	@Test // #864
	public void embeddedSpecUsingAPIs() throws Exception {

		String results = this.mockMvc.perform(get("/author/1").accept(HAL_JSON)) //
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON_VALUE)) //
				.andReturn() //
				.getResponse() //
				.getContentAsString();

		assertThat(results).isEqualTo(contextualMapper.readFile("hal-embedded-author-illustrator.json"));
	}

	@Test // #864
	public void singleItem() throws Exception {

		String results = this.mockMvc.perform(get("/other-author").accept(HAL_JSON)) //
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON_VALUE)) //
				.andReturn() //
				.getResponse() //
				.getContentAsString();

		assertThat(results).isEqualTo(contextualMapper.readFile("hal-single-item.json"));
	}

	@Test // #864
	public void collection() throws Exception {

		String results = this.mockMvc.perform(get("/authors").accept(HAL_JSON)) //
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON_VALUE)) //
				.andReturn() //
				.getResponse() //
				.getContentAsString();

		assertThat(results).isEqualTo(contextualMapper.readFile("hal-embedded-collection.json"));
	}

	@Test // #193
	public void differentlyTypedItems() throws Exception {

		String results = this.mockMvc.perform(get("/machine-shop").accept(HAL_JSON)) //
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON_VALUE)) //
				.andReturn() //
				.getResponse() //
				.getContentAsString();

		assertThat(results).isEqualTo(contextualMapper.readFile("hal-multiple-types.json"));
	}

	@Test // #193
	void embeddedAndNormal() throws Exception {

		String results = this.mockMvc.perform(get("/explicit-and-implicit-rels").accept(HAL_JSON)) //
				.andExpect(header().string(HttpHeaders.CONTENT_TYPE, HAL_JSON_VALUE)) //
				.andReturn() //
				.getResponse() //
				.getContentAsString();

		assertThat(results).isEqualTo(contextualMapper.readFile("hal-explicit-and-implicit-relations.json"));
	}

	@RestController
	static class EmbeddedController {

		@GetMapping("/other-author")
		RepresentationModel<?> singleItem() {

			return ModelBuilder //
					.entity(new Author("Alan Watts", "January 6, 1915", "November 16, 1973")) //
					.link(Link.of("/people/alan-watts")) //
					.build();
		}

		@GetMapping("/authors")
		RepresentationModel<?> collection() {

			return ModelBuilder //
					.entity( //
							ModelBuilder //
									.entity(new Author("Greg L. Turnquist", null, null)) //
									.link(linkTo(methodOn(EmbeddedController.class).authorDetails(1)).withSelfRel()) //
									.link(linkTo(methodOn(EmbeddedController.class).collection()).withRel("authors")) //
									.build())
					.entity( //
							ModelBuilder //
									.entity(new Author("Craig Walls", null, null)) //
									.link(linkTo(methodOn(EmbeddedController.class).authorDetails(2)).withSelfRel()) //
									.link(linkTo(methodOn(EmbeddedController.class).collection()).withRel("authors")) //
									.build())
					.entity( //
							ModelBuilder //
									.entity(new Author("Oliver Drotbhom", null, null)) //
									.link(linkTo(methodOn(EmbeddedController.class).authorDetails(2)).withSelfRel()) //
									.link(linkTo(methodOn(EmbeddedController.class).collection()).withRel("authors")) //
									.build())
					.link(linkTo(methodOn(EmbeddedController.class).collection()).withSelfRel()) //
					.build();
		}

		@GetMapping("/author/{id}")
		RepresentationModel<?> authorDetails(@PathVariable int id) {

			return ModelBuilder //
					.entity(LinkRelation.of("author"), ModelBuilder //
							.entity(new Author("Alan Watts", "January 6, 1915", "November 16, 1973")) //
							.link(Link.of("/people/alan-watts")) //
							.build())
					.entity(LinkRelation.of("illustrator"), ModelBuilder //
							.entity(new Author("John Smith", null, null)) //
							.link(Link.of("/people/john-smith")) //
							.build())
					.link(Link.of("/books/the-way-of-zen")) //
					.link(Link.of("/people/alan-watts", LinkRelation.of("author"))) //
					.link(Link.of("/people/john-smith", LinkRelation.of("illustrator"))) //
					.build();
		}

		@GetMapping("/machine-shop")
		RepresentationModel<?> differentlyTypedItems() {

			Staff staff1 = new Staff("Frodo Baggins", "ring bearer");
			Staff staff2 = new Staff("Bilbo Baggins", "burglar");

			Product product1 = new Product("ring of power", 999.99);
			Product product2 = new Product("Saruman's staff", 9.99);

			return ModelBuilder //
					.entity(staff1) //
					.entity(staff2) //
					.entity(product1) //
					.entity(product2) //
					.link(Link.of("/people/alan-watts")) //
					.build();
		}

		@GetMapping("/explicit-and-implicit-rels")
		RepresentationModel<?> explicitAndImplicitRelations() {

			Staff staff1 = new Staff("Frodo Baggins", "ring bearer");
			Staff staff2 = new Staff("Bilbo Baggins", "burglar");

			Product product1 = new Product("ring of power", 999.99);
			Product product2 = new Product("Saruman's staff", 9.99);

			return ModelBuilder //
					.entity(staff1) //
					.entity(staff2) //
					.entity(product1) //
					.entity(product2) //
					.link(Link.of("/people/alan-watts")) //
					.entity(LinkRelation.of("ring bearers"), staff1) //
					.entity(LinkRelation.of("burglars"), staff2) //
					.link(Link.of("/people/frodo-baggins", LinkRelation.of("frodo"))) //
					.build();
		}
	}

	@Value
	@AllArgsConstructor
	static class Author {

		private String name;

		@Getter(onMethod = @__({ @JsonInclude(JsonInclude.Include.NON_NULL) })) private String born;

		@Getter(onMethod = @__({ @JsonInclude(JsonInclude.Include.NON_NULL) })) private String died;
	}

	@Value
	@AllArgsConstructor
	static class Staff {

		private String name;
		private String role;
	}

	@Value
	@AllArgsConstructor
	static class Product {

		private String name;
		private Double price;
	}

	@Configuration
	@EnableWebMvc
	@EnableHypermediaSupport(type = HypermediaType.HAL)
	static class TestConfig {

		@Bean
		EmbeddedController controller() {
			return new EmbeddedController();
		}

		@Bean
		ObjectMapper testMapper() {

			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
			return objectMapper;
		}
	}
}
