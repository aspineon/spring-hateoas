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
package org.springframework.hateoas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.hateoas.server.core.EmbeddedWrappers;

/**
 * Provide a builder to simplify creating hypermedia representations.
 *
 * @author Greg Turnquist
 * @since 1.1
 */
public class ModelBuilder {

	/**
	 * Helper method to create a new {@link SimpleBuilder} wrapped around any object.
	 *
	 * @param entity
	 * @return
	 */
	public static SimpleBuilder entity(Object entity) {
		return new SimpleBuilder(entity);
	}

	/**
	 * Helper method to create a new {@link ComplexBuilder} based on {@link LinkRelation} and an object.
	 *
	 * @param relation
	 * @param entity
	 * @return
	 */
	public static ComplexBuilder entity(LinkRelation relation, Object entity) {
		return new ComplexBuilder(relation, entity);
	}

	/**
	 * Add a {@link Link} to the builder.
	 *
	 * @param link
	 * @return
	 */
	public static SimpleBuilder link(Link link) {
		return new SimpleBuilder(link);
	}

	/**
	 * Simple representation builder with zero or more entities and a {@link List} of {@link Link}s.
	 */
	public static class SimpleBuilder {

		private final List<Object> entities = new ArrayList<>();
		private final List<Link> links = new ArrayList<>();

        /**
         * Start creating a representation based on an object.
         *
         * @param entity
         */
		SimpleBuilder(Object entity) {
			entity(entity);
		}

        /**
         * Start creating a representation based on a {@link Link}.
         *
         * @param link
         */
		SimpleBuilder(Link link) {
			link(link);
		}

		/**
		 * Add some object to the existing representation.
		 *
		 * @param entity
		 * @return
		 */
		public SimpleBuilder entity(Object entity) {

			this.entities.add(entity);
			return this;
		}

		/**
		 * Add some object with a specific {@link LinkRelation} to the representation.
		 *
		 * @param linkRelation
		 * @param entity
		 * @return
		 */
		public ComplexBuilder entity(LinkRelation linkRelation, Object entity) {
			return new ComplexBuilder(this.entities, this.links, linkRelation, entity);
		}

		/**
		 * Add a {@link LinkRelation} to the existing representation.
		 *
		 * @param link
		 * @return
		 */
		public SimpleBuilder link(Link link) {

			this.links.add(link);
			return this;
		}

		/**
		 * Transform everything into a {@link RepresentationModel}.
		 *
		 * @return
		 */
		public RepresentationModel<?> build() {

			if (this.entities.isEmpty()) {
				return new RepresentationModel<>(this.links);
			} else if (this.entities.size() == 1) {
				Object content = this.entities.get(0);
				if (RepresentationModel.class.isInstance(content)) {
					return (RepresentationModel<?>) content;
				} else {
					return EntityModel.of(content, this.links);
				}
			} else {
				return CollectionModel.of(this.entities, this.links);
			}
		}

	}

    /**
     * Builder that supports creating more complex representations based on {@link LinkRelation}s.
     *
     * @author Greg Turnquist
     * @since 1.1
     */
	public static class ComplexBuilder {

	    private static final LinkRelation NO_RELATION = LinkRelation.of("___norel___");

		private final Map<LinkRelation, List<Object>> entityModels;
		private final List<Link> links;

        /**
         * Start with nothing.
         */
		public ComplexBuilder() {

			this.entityModels = new LinkedHashMap<>(); // maintain the original order of entries
			this.links = new ArrayList<>();
		}

        /**
         * Way to transition from {@link SimpleBuilder#entity(LinkRelation, Object)} and retain existing information.
         *
         * @param entities
         * @param links
         * @param newLinkRelation
         * @param newEntity
         */
		ComplexBuilder(List<Object> entities, List<Link> links, LinkRelation newLinkRelation, Object newEntity) {

			this();

			// Load up existing objects and links.
            entities.forEach(this::entity);
            this.links.addAll(links);

            // Add the new entity with its link relation.
            entity(newLinkRelation, newEntity);
		}

        /**
         * Start with nothing but a {@link LinkRelation} and an object.
         *
         * @param linkRelation
         * @param entity
         */
		ComplexBuilder(LinkRelation linkRelation, Object entity) {

			this();
			entity(linkRelation, entity);
		}

        /**
         * Add a new object with no defined link relation.
         *
         * @param entity
         * @return
         */
		public ComplexBuilder entity(Object entity) {
		    return entity(NO_RELATION, entity);
        }

        /**
         * Add a new object with a {@link LinkRelation}.
         *
         * @param linkRelation
         * @param entity
         * @return
         */
		public ComplexBuilder entity(LinkRelation linkRelation, Object entity) {

			this.entityModels.computeIfAbsent(linkRelation, r -> new ArrayList<>()).add(entity);
			return this;
		}

        /**
         * Add a new {@link Link}.
         *
         * @param link
         * @return
         */
		public ComplexBuilder link(Link link) {

			this.links.add(link);
			return this;
		}

        /**
         * Transform everything into a {@link RepresentationModel}.
         *
         * @return
         */
        public RepresentationModel<?> build() {

			EmbeddedWrappers wrappers = new EmbeddedWrappers(false);

			return this.entityModels.keySet().stream() //
					.flatMap(linkRelation -> this.entityModels.get(linkRelation).stream() //
							.map(model -> wrappers.wrap(model, linkRelation))) //
					.collect(Collectors.collectingAndThen(Collectors.toList(),
							embeddedWrappers -> CollectionModel.of(embeddedWrappers, this.links)));
		}
	}
}
