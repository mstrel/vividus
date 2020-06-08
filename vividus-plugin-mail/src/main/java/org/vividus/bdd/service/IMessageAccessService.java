package org.vividus.bdd.service;

import java.util.List;
import java.util.function.Predicate;

import org.vividus.bdd.model.MailServerConfiguration;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public interface IMessageAccessService
{
    List<Message> find(List<Predicate<Message>> messageFilters, MailServerConfiguration configuration)
            throws MessagingException;
}
