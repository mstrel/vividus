package org.vividus.bdd.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.vividus.bdd.model.MailServerConfiguration;
import org.vividus.util.Sleeper;
import org.vividus.util.wait.WaitMode;
import org.vividus.util.wait.Waiter;

import jakarta.mail.Authenticator;
import jakarta.mail.FetchProfile;
import jakarta.mail.FetchProfile.Item;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import jakarta.mail.search.SearchTerm;

public class MessageAccessService implements IMessageAccessService
{
    private final Duration pollingDuration;
    private final int poolingTimes;

    private final Duration messageWaitToArriveDuration;

    private final String folder;

    public MessageAccessService(Duration pollingDuration, int poolingTimes, Duration messageWaitToArriveDuration,
            String folder)
    {
        this.pollingDuration = pollingDuration;
        this.poolingTimes = poolingTimes;
        this.messageWaitToArriveDuration = messageWaitToArriveDuration;
        this.folder = folder;
    }

    @Override
    public List<Message> find(List<Predicate<Message>> messageFilters, MailServerConfiguration configuration)
            throws MessagingException
    {
        Authenticator authenticator = new PasswordAuthenticator(configuration.getUsername(),
                configuration.getPassword());

        Properties properties = new Properties();
        properties.putAll(asImapsProperties(configuration.getProperties()));

        Session session = Session.getInstance(properties, authenticator);
        Store store = session.getStore("imaps");
        store.connect();

        try(Folder folder = store.getFolder("Inbox");)
        {
            SearchTerm searchTerm = new PredicateSearchTerm(messageFilters);
            BlockingMessageListener listener = new BlockingMessageListener(() -> folder.isOpen(), searchTerm);

            folder.addMessageCountListener(listener);
            folder.open(Folder.READ_ONLY);

            Message[] messages = fetchMessages(folder, Set.of(Item.ENVELOPE));

            Message[] filtered = folder.search(searchTerm, messages);

            if (filtered.length > 0)
            {
                fetchMessages(folder, filtered, Set.of(Item.CONTENT_INFO));
                return List.of(filtered);
            }
            else
            {
                WaitMode waitMode = new WaitMode(pollingDuration, poolingTimes);
                Waiter waiter = new Waiter(waitMode);

                return waiter.wait(() -> listener.getMessages(), msgs -> !msgs.isEmpty());
            }
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private Message[] fetchMessages(Folder folder, Set<Item> fetchSettings) throws MessagingException
    {
        return fetchMessages(folder, folder.getMessages(), fetchSettings);
    }

    private Message[] fetchMessages(Folder folder, Message[] messages, Set<Item> fetchSettings)
            throws MessagingException
    {
        FetchProfile profile = new FetchProfile();
        fetchSettings.forEach(profile::add);
        folder.fetch(messages, profile);
        return messages;
    }

    private Map<String, String> asImapsProperties(Map<String, String> properties)
    {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(e -> "mail.imaps." + e.getKey(), Map.Entry::getValue));
    }

    private static class PredicateSearchTerm extends SearchTerm
    {
        private static final long serialVersionUID = 1163386376061414046L;

        private final List<Predicate<Message>> messageFilters;

        private PredicateSearchTerm(List<Predicate<Message>> messageFilters)
        {
            this.messageFilters = messageFilters;
        }

        @Override
        public boolean match(Message msg)
        {
            return messageFilters.stream().allMatch(p -> p.test(msg));
        }
    }

    private static class PasswordAuthenticator extends Authenticator
    {
        private final String username;
        private final String password;

        private PasswordAuthenticator(String username, String password)
        {
            this.username = username;
            this.password = password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(username, password);
        }
    }

    private class BlockingMessageListener implements MessageCountListener
    {
        private final Runnable refresh;
        private final SearchTerm searchTerm;
        private final List<Message> messages;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition messageArrivedCondition = lock.newCondition();
        private final Condition messageHandleCondition = lock.newCondition();

        private BlockingMessageListener(Runnable refresh, SearchTerm searchTerm)
        {
            this.refresh = refresh;
            this.searchTerm = searchTerm;
            this.messages = new CopyOnWriteArrayList<>();
        }

        @Override
        public void messagesAdded(MessageCountEvent event)
        {
            try
            {
                lock.lock();
                messageArrivedCondition.signal();
                messageHandleCondition.await();
                Message[] newMessages = event.getMessages();
                for (int index = 0; index < newMessages.length; index++)
                {
                    Message message = newMessages[index];
                    if (searchTerm.match(message))
                    {
                        messages.add(newMessages[index]);
                    }
                }
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
            finally
            {
                messageHandleCondition.signal();
                lock.unlock();
            }
        }

        @Override
        public void messagesRemoved(MessageCountEvent event)
        {
        }

        public List<Message> getMessages() throws InterruptedException
        {
            while (lock.isLocked())
            {
                Sleeper.sleep(Duration.ofSeconds(1));
            }
            try
            {
                lock.lock();
                refresh.run();
                boolean arrived = messageArrivedCondition.await(messageWaitToArriveDuration.toMillis(),
                        TimeUnit.MILLISECONDS);
                if (arrived)
                {
                    messageHandleCondition.signal();
                    messageHandleCondition.await();
                }
                return new ArrayList<Message>(this.messages);
            }
            finally
            {
                lock.unlock();
            }
        }
    }
}
