/*
 * Copyright 2007 the original author or authors.
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

package org.gradle.api.internal

import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author Hans Dockter
 */
abstract class ConventionTask extends DefaultTask implements IConventionAware {
    ConventionAwareHelper conventionAwareHelper
    
    ConventionTask(Project project, String name) {
        super(project, name)
        conventionAwareHelper = new ConventionAwareHelper(this)
        conventionAwareHelper.convention = project.convention
    }

    Task conventionMapping(Map mapping) {
        conventionAwareHelper.conventionMapping(mapping)
    }

    def getProperty(String name) {
        conventionAwareHelper.getValue(name)
    }

    void setConventionMapping(Map conventionMapping) {
        conventionAwareHelper.conventionMapping = conventionMapping
    }

    def getConventionMapping() {
        conventionAwareHelper.conventionMapping
    }

}
