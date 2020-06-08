package org.vividus.bdd.model;

import java.util.function.Predicate;

import org.vividus.bdd.steps.IComparisonRule;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public enum MailParameterFilterFactory
{
    SUBJECT
    {
        @Override
        public <T extends Comparable<T>> Predicate<Message> createFilter(IComparisonRule rule, T variable)
        {
            return m ->
            {
                String subject = getValueSafely(m, Message::getSubject);
                return rule.getComparisonRule(variable).matches(subject);
            };
        }
    };

    public abstract <T extends Comparable<T>> Predicate<Message> createFilter(IComparisonRule rule, T variable);

    static <T> T getValueSafely(Message message, MessagingExceptionFunction<Message, T> getter)
    {
        try
        {
            return getter.apply(message);
        }
        catch (MessagingException e)
        {
            throw new IllegalStateException();
        }
    }

    private static interface MessagingExceptionFunction<T, R>
    {
        R apply(T value) throws MessagingException;
    }
}
