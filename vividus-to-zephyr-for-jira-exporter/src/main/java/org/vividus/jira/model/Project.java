package org.vividus.jira.model;

import java.util.List;

public class Project extends JiraEntity
{
    private List<Version> versions;

    public List<Version> getVersions()
    {
        return versions;
    }

    public void setVersions(List<Version> versions)
    {
        this.versions = versions;
    }
}
