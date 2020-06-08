package org.vividus.bdd.steps.mail;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jbehave.core.annotations.When;
import org.vividus.bdd.context.IBddVariableContext;
import org.vividus.bdd.model.MailParameterFilterFactory;
import org.vividus.bdd.model.MailServerConfiguration;
import org.vividus.bdd.service.IMessageAccessService;
import org.vividus.bdd.steps.ComparisonRule;
import org.vividus.bdd.variable.VariableScope;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public class MailSteps
{
    private final Map<String, MailServerConfiguration> serverConfigurations;
    private final IMessageAccessService messageAccessService;
    private final IBddVariableContext bddVariableContext;

    public MailSteps(Map<String, MailServerConfiguration> serverConfigurations,
            IMessageAccessService messageAccessService, IBddVariableContext bddVariableContext)
    {
        this.messageAccessService = messageAccessService;
        this.bddVariableContext = bddVariableContext;
        this.serverConfigurations = serverConfigurations;
    }

    @When("I fetch mail message from `serverKey` server filtered by $filters and save message content to $scopes variable `$variableName`")
    public void saveMessageContent(String serverKey, List<Predicate<Message>> messageFilters, Set<VariableScope> scopes,
            String variableName) throws MessagingException
    {
        // Optional<Message> message = messageAccessService.find(messageFilters, serverConfigurations.get(serverKey));
    }

    @When("I do something with mail")
    public void func() throws MessagingException
    {
        MailServerConfiguration config = serverConfigurations.get("gmail");
        Predicate<Message> pred = MailParameterFilterFactory.SUBJECT.createFilter(ComparisonRule.EQUAL_TO,
                "Test message");
        List<Message> message = messageAccessService.find(List.of(pred), config);
        System.out.println(message.size());
    }
}
