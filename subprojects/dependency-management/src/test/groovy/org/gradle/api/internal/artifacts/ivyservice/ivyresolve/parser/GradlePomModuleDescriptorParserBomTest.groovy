/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser

import org.gradle.internal.component.external.descriptor.MavenScope

import static org.gradle.api.internal.component.ArtifactType.MAVEN_POM

class GradlePomModuleDescriptorParserBomTest extends AbstractGradlePomModuleDescriptorParserTest {

    def "a pom file with packaging=pom is a bom - dependencies declared in dependencyManagement block are treated as optional dependencies"() {
        given:
        pomFile << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>bom</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>group-b</groupId>
                <artifactId>module-b</artifactId>
                <version>1.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"""

        when:
        parsePom()

        then:
        def dep = single(metadata.dependencies)
        dep.selector == moduleId('group-b', 'module-b', '1.0')
        dep.scope == MavenScope.Compile
        hasDefaultDependencyArtifact(dep)
        dep.optional
    }

    def "a pom with dependencies block is not a bom - dependencyManagement block is ignored"() {
        given:
        pomFile << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>bom</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>
    
    <dependencies>
        <dependency>
            <groupId>group-b</groupId>
            <artifactId>module-b</artifactId>
            <version>1.0</version>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>group-b</groupId>
                <artifactId>module-b</artifactId>
                <version>2.0</version>
            </dependency>
            <dependency>
                <groupId>group-d</groupId>
                <artifactId>module-d</artifactId>
                <version>2.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"""

        when:
        parsePom()

        then:
        def dep = single(metadata.dependencies)
        dep.selector == moduleId('group-b', 'module-b', '1.0')
        !dep.optional
    }

    def "a parent pom is not a bom - dependencies declared in dependencyManagement block are ignored"() {
        given:
        pomFile << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>module-a</artifactId>
    <version>1.0</version>

    <parent>
        <groupId>group-a</groupId>
        <artifactId>parent</artifactId>
        <version>1.0</version>
    </parent>
</project>
"""
        def parent = tmpDir.file("parent.xml") << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>parent</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>group-b</groupId>
                <artifactId>module-b</artifactId>
                <version>1.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"""
        parseContext.getMetaDataArtifact({ it.selector.module == 'parent' }, MAVEN_POM) >> asResource(parent)

        when:
        parsePom()

        then:
        metadata.dependencies.empty
    }

    def "a bom version can be relocated"() {
        given:
        pomFile << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>bom</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>
    
    <distributionManagement>
        <relocation>
            <version>2.0</version>
        </relocation>
    </distributionManagement>
</project>
"""

        def relocatedToPomFile = tmpDir.file("relocated.xml") << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>bom</artifactId>
    <version>2.0</version>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>group-b</groupId>
                <artifactId>module-b</artifactId>
                <version>1.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"""
        parseContext.getMetaDataArtifact({ it.version == '2.0' }, MAVEN_POM) >> asResource(relocatedToPomFile)

        when:
        parsePom()

        then:
        def dep = single(metadata.dependencies)
        dep.selector == moduleId('group-b', 'module-b', '1.0')
        dep.scope == MavenScope.Compile
        hasDefaultDependencyArtifact(dep)
        dep.optional
    }

    def "a bom can be composed of children and parents"() {
        given:
        pomFile << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>bom</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>
    
    <parent>
        <groupId>group-a</groupId>
        <artifactId>parent</artifactId>
        <version>1.0</version>
    </parent>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>group-b</groupId>
                <artifactId>module-b</artifactId>
                <version>1.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"""
        def parent = tmpDir.file("parent.xml") << """
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>group-a</groupId>
    <artifactId>parent</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>group-c</groupId>
                <artifactId>module-c</artifactId>
                <version>2.0</version>
            </dependency>
            <dependency>
                <groupId>group-d</groupId>
                <artifactId>module-d</artifactId>
                <version>2.0</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
"""
        parseContext.getMetaDataArtifact({ it.selector.module == 'parent' }, MAVEN_POM) >> asResource(parent)
        when:
        parsePom()

        then:
        metadata.dependencies.size() == 3
        metadata.dependencies.each { assert it.optional }
    }
}
