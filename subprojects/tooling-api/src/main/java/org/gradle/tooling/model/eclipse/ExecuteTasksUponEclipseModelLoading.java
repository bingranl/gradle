/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.tooling.model.eclipse;

import org.gradle.api.Incubating;

/**
 * TODO (donat) add documentation.
 *
 * @since 5.4
 */
// TODO (donat) find better naming
@Incubating
public interface ExecuteTasksUponEclipseModelLoading {
    /**
     * TODO (donat)
     *
     * Later, we want to support two use-cases:
     *  - execute tasks when before loading the Eclipse model
     *  - execute tasks during Eclipse auto-build
     *
     *  Maybe we should refactor the corresponding model builder accept a parameter describing
     *  which tasks to execute, the one for model loading or the one for autobuild.
     */
}
