package com.atlassian.jira.plugins.mail.customhandler;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.service.util.handler.MessageHandler;
import com.atlassian.jira.service.util.handler.MessageHandlerContext;
import com.atlassian.jira.service.util.handler.MessageHandlerErrorCollector;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import com.atlassian.jira.service.util.handler.MessageUserProcessor;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.mail.MailUtils;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang.StringUtils;


@Scanned
public class CustomHandler implements MessageHandler {
    private final ProjectValidator projectValidator;
    private final MessageUserProcessor messageUserProcessor;

    private String projKey;
    private Project project;
    private String issueTypeKey;
    private Long securityLevel;
    private Priority defaultPriority;
    private Long workflowId;

    public static final String PROJECT_KEY = "project";
    public static final String ISSUETYPE_ID = "issuetype";
    public CustomHandler(@ComponentImport MessageUserProcessor messageUserProcessor, ProjectValidator projectValidator){
        this.messageUserProcessor = messageUserProcessor;
        this.projectValidator = projectValidator;
    }

    @Override
    public void init(Map<String, String> params, MessageHandlerErrorCollector monitor) {
        projKey = params.get(PROJECT_KEY);
        if(StringUtils.isBlank(projKey)){
            monitor.error("Project key has not been specified ('" + PROJECT_KEY + "' parameter). This handler will not work correctly.");
        }
        project = getProject(projKey);

        issueTypeKey = params.get(ISSUETYPE_ID);

        Collection<Priority> priorities = ComponentAccessor.getConstantsManager().getPriorities();
        int times = (int)Math.ceil((double)priorities.size() / 2.0D);
        Iterator<Priority> priorityIt = priorities.iterator();
        for(int i = 0; i < times; ++i) {
            defaultPriority = (Priority)priorityIt.next();
        }

        securityLevel = ComponentAccessor.getIssueSecurityLevelManager().getDefaultSecurityLevel(project);

        workflowId = ComponentAccessor.getWorkflowSchemeManager().getDefaultWorkflowScheme().getId();

        projectValidator.validateProject(projKey, monitor, params.toString());
    }

    @Override
    public boolean handleMessage(Message message, MessageHandlerContext context) throws MessagingException {


        if(project == null){
            context.getMonitor().error("This ProjectKey(" + projKey + ") is invalid Key.");
            return false;
        }

        final String summary = message.getSubject();
        final String body = MailUtils.getBody(message);


        final ApplicationUser reporter = ComponentAccessor.getUserManager().getUserByKey(project.getLeadUserKey());

        final ApplicationUser assignee = getFirstValidAssignee(message.getAllRecipients(), project);

        MutableIssue issue = ComponentAccessor.getIssueFactory().getIssue();
        issue.setSummary(summary);
        issue.setDescription(body);
        issue.setIssueTypeId(issueTypeKey);
        issue.setReporter(reporter);
        issue.setAssignee(assignee);
        issue.setPriority(defaultPriority);
        issue.setSecurityLevelId(securityLevel);
        issue.setProjectObject(project);
        issue.setWorkflowId(workflowId);

        try {
            context.createIssue(reporter, issue);
        } catch (CreateException e) {
            e.printStackTrace();
        }


        return true;
    }

    protected Project getProject(String projKey) {
        if (projKey == null) {
            return null;
        } else {
            return this.getProjectManager().getProjectObjByKey(projKey.toUpperCase(Locale.getDefault()));
        }
    }

    protected ProjectManager getProjectManager() {
        return ComponentAccessor.getProjectManager();
    }

    private static ApplicationUser getFirstValidAssignee(Address[] addresses, Project project) {
        if (addresses != null && addresses.length != 0) {
            Address[] var2 = addresses;
            int var3 = addresses.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Address address = var2[var4];
                if (address instanceof InternetAddress) {
                    InternetAddress email = (InternetAddress)address;
                    ApplicationUser validUser = UserUtils.getUserByEmail(email.getAddress());
                    if (validUser != null && (ComponentAccessor.getPermissionManager()).hasPermission(new ProjectPermissionKey("17"), project, validUser)) {
                        return validUser;
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

}

