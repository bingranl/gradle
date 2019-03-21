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

package org.gradle.plugins.ide.internal.tooling;

import org.gradle.api.Project;
import org.gradle.plugins.ide.eclipse.internal.EclipseSyncTaskCommandResult;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import java.util.ArrayList;
import java.util.List;

public class ExecuteEclipseSynchronizationTasks implements ToolingModelBuilder {
    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals("org.gradle.tooling.model.eclipse.ExecuteTasksUponEclipseModelLoading");
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        List<String> taskNames = new ArrayList<String>();
        for (Project p : project.getAllprojects()) {
            EclipseModel model = p.getExtensions().findByType(EclipseModel.class);
            if (model != null) {
                taskNames.addAll(model.getSynchronizationTasks());
            }
        }

        if (taskNames.isEmpty()) {
            // If no tasks is specified then the default tasks will be executed.
            // To work around this, we assign a new empty task for execution.
            String placeHolderTaskName = placeHolderTaskName(project, "nothing");
            project.task(placeHolderTaskName);
            taskNames.add(placeHolderTaskName);
        }

        project.getGradle().getStartParameter().setTaskNames(taskNames);
        return new EclipseSyncTaskCommandResult(); // TODO (donat) do we need this?
    }

    private String placeHolderTaskName(Project project, String baseName) {
        if (project.getTasksByName(baseName, true).isEmpty()) {
            return baseName;
        } else {
            return placeHolderTaskName(project, baseName + "_");
        }
    }
}
