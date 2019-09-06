package com.atlassian.jira.plugins.mail.customhandler;

import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.jira.bulkedit.operation.ProgressAwareBulkOperation;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.plugin.ComponentClassManager;
import com.atlassian.jira.plugins.mail.webwork.AbstractEditHandlerDetailsWebAction;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.service.JiraServiceContainer;
import com.atlassian.jira.service.services.file.AbstractMessageHandlingService;
import com.atlassian.jira.service.util.ServiceUtils;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Scanned
public class EditDemoHandlerDetailsWebAction extends AbstractEditHandlerDetailsWebAction {
    private final ProjectValidator projectValidator;
    private final Logger log = Logger.getLogger(this.getClass());

    public EditDemoHandlerDetailsWebAction(@ComponentImport PluginAccessor pluginAccessor, ProjectValidator projectValidator) {
        super(pluginAccessor);
        this.projectValidator = projectValidator;
    }

    private String project;
    private String issuetype;
    private String parameterMapStr;
    private String test;

    // this method is called to let us populate our variables (or action state)
    // with current handler settings managed by associated service (file or mail).
    @Override
    protected void copyServiceSettings(JiraServiceContainer jiraServiceContainer) throws ObjectConfigurationException {
        final String params = jiraServiceContainer.getProperty(AbstractMessageHandlingService.KEY_HANDLER_PARAMS);
        final Map<String, String> parameterMap = ServiceUtils.getParameterMap(params);
        test = params;
        parameterMapStr = parameterMap.toString();
        project = parameterMap.get(CustomHandler.PROJECT_KEY);
        issuetype = parameterMap.get(CustomHandler.ISSUETYPE_ID);

    }

    @Override
    protected Map<String, String> getHandlerParams() {
        Map<String, String> res = Maps.newLinkedHashMap();

        if(StringUtils.isNotBlank(this.project))
            res.put(CustomHandler.PROJECT_KEY, this.project);
        if(StringUtils.isNotBlank(this.issuetype))
            res.put(CustomHandler.ISSUETYPE_ID, this.issuetype);

        return res;
    }

    @Override
    protected void doValidation() {
        project = getHttpRequest().getParameter("project");
        issuetype = getHttpRequest().getParameter("issuetype");
        if (configuration == null) {
            return; // short-circuit in case we lost session, goes directly to doExecute which redirects user
        }
        super.doValidation();
        projectValidator.validateProject(project, new WebWorkErrorCollector(), parameterMapStr);
    }
    public List<Project> getProjects(){
        return ComponentAccessor.getProjectManager().getProjects();
    }
    public List<IssueType> getIssueTypes(String projKey){
        Project project1 = ComponentAccessor.getProjectManager().getProjectObjByKey(projKey);
        return (List<IssueType>) ComponentAccessor.getIssueTypeSchemeManager().getIssueTypesForProject(project1);
    }
    public List<ProgressAwareBulkOperation> getBulks(){
        return (List<ProgressAwareBulkOperation>) ComponentAccessor.getBulkOperationManager().getProgressAwareBulkOperations();
    }

    public String getProject(){
        return this.project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getIssueType() {
        return this.issuetype;
    }

    public void setissueType() {
        this.issuetype = issuetype;
    }
}