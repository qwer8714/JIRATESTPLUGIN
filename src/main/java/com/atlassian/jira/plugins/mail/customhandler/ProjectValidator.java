package com.atlassian.jira.plugins.mail.customhandler;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.service.util.handler.MessageHandlerErrorCollector;
import com.atlassian.plugin.spring.scanner.annotation.component.JiraComponent;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.inject.Inject;

@JiraComponent("ProjectValidator")
public class ProjectValidator {
    @ComponentImport
    private final ProjectManager projectManager;

    @Inject
    public ProjectValidator(ProjectManager projectManager){
        this.projectManager = projectManager;
    }

    public Project validateProject(String projectKey, MessageHandlerErrorCollector errorCollector, String param){



        final Project project = projectManager.getProjectObjByKey(projectKey);
        if(project == null){
            errorCollector.error("cannot add issue from mail to project " + projectKey + ", param: " + param);
            return null;
        }

        return project;
    }
}
