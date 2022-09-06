/*
 * Copyright 2017-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ysb33r.gradle.terraform.remotestate;

/**
 * Represents a text template for creating a backend configuration file.
 *
 * @author Schalk W. Cronjé
 *
 * @since 0.12
 */
public interface BackendTextTemplate {

    /**
     * Returns a template based upon backend attributes.
     *
     * @param backendAttributes Attributes can be used to generate the template.
     *
     * @return Text template
     */
    String template(BackendAttributesSpec backendAttributes);
}
