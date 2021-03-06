/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.api

class NamedDomainObjectContainerIntegrationTest extends AbstractDomainObjectContainerIntegrationTest {
    @Override
    String getContainerUnderTest() {
        return "aContainer"
    }

    @Override
    String getBaseElementType() {
        return "SomeType"
    }

    @Override
    String disallowMutationMessage(String assertingMethod) {
        return "NamedDomainObjectContainer#$assertingMethod on $baseElementType container cannot be executed in the current context."
    }

    def setup() {
        buildFile << """
            class $baseElementType implements Named {
                final String name

                ${baseElementType}(String name) {
                    this.name = name
                }
            }

            def ${containerUnderTest} = project.container($baseElementType)
        """
    }
}
