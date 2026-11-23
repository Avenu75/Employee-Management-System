package com.rainbow.springboot.thymeleafdemo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rainbow.springboot.thymeleafdemo.dao.AppUserRepository;
import com.rainbow.springboot.thymeleafdemo.dao.PrivateMessageRepository;
import com.rainbow.springboot.thymeleafdemo.dto.Conversation;
import com.rainbow.springboot.thymeleafdemo.entity.AppUser;
import com.rainbow.springboot.thymeleafdemo.entity.PrivateMessage;
import com.rainbow.springboot.thymeleafdemo.exception.UserNotFoundException;

/**
 * Private one-to-one messages.
 *
 * Anyone with an enabled account can write to anyone else. Nobody, not even the Super Admin,
 * can read a conversation they are not part of.
 */
@Service
public class MessageService {

    public static final int MAX_LENGTH = 2000;

    private final PrivateMessageRepository messageRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    public MessageService(PrivateMessageRepository theMessageRepository,
                          AppUserRepository theAppUserRepository,
                          NotificationService theNotificationService) {
        messageRepository = theMessageRepository;
        appUserRepository = theAppUserRepository;
        notificationService = theNotificationService;
    }

    /** Everyone this user is allowed to start or continue a conversation with. */
    @Transactional(readOnly = true)
    public List<AppUser> recipientsFor(AppUser user) {
        List<AppUser> result = new ArrayList<>();
        for (AppUser candidate : appUserRepository.findByEnabledTrueOrderByFullNameAsc()) {
            if (candidate.getId() != user.getId()) {
                result.add(candidate);
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean canMessage(AppUser sender, int recipientId) {
        for (AppUser candidate : recipientsFor(sender)) {
            if (candidate.getId() == recipientId) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<Conversation> conversations(AppUser me) {
        Map<Integer, PrivateMessage> lastByOther = new LinkedHashMap<>();
        Map<Integer, Long> unreadByOther = new LinkedHashMap<>();
        Map<Integer, AppUser> others = new LinkedHashMap<>();

        for (PrivateMessage m : messageRepository.findInvolving(me, PageRequest.of(0, 500))) {
            boolean incoming = m.getRecipient().getId() == me.getId();
            AppUser other = incoming ? m.getSender() : m.getRecipient();
            others.putIfAbsent(other.getId(), other);
            lastByOther.putIfAbsent(other.getId(), m);
            if (incoming && m.getReadAt() == null) {
                unreadByOther.merge(other.getId(), 1L, Long::sum);
            }
        }

        List<Conversation> list = new ArrayList<>();
        for (Map.Entry<Integer, PrivateMessage> e : lastByOther.entrySet()) {
            list.add(new Conversation(others.get(e.getKey()), e.getValue(), unreadByOther.getOrDefault(e.getKey(), 0L)));
        }
        return list;
    }

    /** The messages between me and one other person; marks the incoming ones as read. */
    @Transactional
    public List<PrivateMessage> thread(AppUser me, AppUser other) {
        messageRepository.markRead(me, other, LocalDateTime.now());
        return messageRepository.findThread(me, other);
    }

    @Transactional(readOnly = true)
    public AppUser findUser(int id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("That person could not be found."));
    }

    @Transactional
    public void send(AppUser sender, int recipientId, String body) {
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("Write a message first.");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalStateException("That message is too long (maximum " + MAX_LENGTH + " characters).");
        }
        if (!canMessage(sender, recipientId)) {
            throw new IllegalStateException("You cannot send a message to that person.");
        }
        AppUser recipient = findUser(recipientId);

        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setBody(text);
        message.setCreatedAt(LocalDateTime.now());
        messageRepository.save(message);

        notificationService.notify(recipient, "New message from " + sender.getFullName(),
                "/messages/with/" + sender.getId());
    }

    /**
     * Super Admin only: sends the same private message to several people at once.
     * Each person gets an ordinary one-to-one message from the sender, so replies stay private.
     *
     * @return how many people received it
     */
    @Transactional
    public int broadcast(AppUser sender, List<Integer> recipientIds, String body) {
        if (sender == null || !sender.isSuperAdmin()) {
            throw new IllegalStateException("Only the Super Admin can send a message to several people.");
        }
        if (recipientIds == null || recipientIds.isEmpty()) {
            throw new IllegalStateException("Choose at least one person.");
        }
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("Write a message first.");
        }
        if (text.length() > MAX_LENGTH) {
            throw new IllegalStateException("That message is too long (maximum " + MAX_LENGTH + " characters).");
        }

        Set<Integer> wanted = new HashSet<>(recipientIds);
        LocalDateTime now = LocalDateTime.now();
        int sent = 0;
        for (AppUser recipient : recipientsFor(sender)) {
            if (!wanted.contains(recipient.getId())) {
                continue;
            }
            PrivateMessage message = new PrivateMessage();
            message.setSender(sender);
            message.setRecipient(recipient);
            message.setBody(text);
            message.setCreatedAt(now);
            messageRepository.save(message);
            notificationService.notify(recipient, "New message from " + sender.getFullName(),
                    "/messages/with/" + sender.getId());
            sent++;
        }
        if (sent == 0) {
            throw new IllegalStateException("None of the chosen people can receive messages.");
        }
        return sent;
    }

    @Transactional(readOnly = true)
    public long unreadCount(AppUser me) {
        return me == null ? 0 : messageRepository.countByRecipientAndReadAtIsNull(me);
    }
}
