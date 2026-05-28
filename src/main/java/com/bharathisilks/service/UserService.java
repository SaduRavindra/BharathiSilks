package com.bharathisilks.service;

import com.bharathisilks.domain.AppUser;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.AppUserRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    public static final String OWNER = "OWNER";
    public static final String STAFF = "STAFF";

    private final AppUserRepository users;
    private final Set<String> ownerPhones;
    private final Set<String> ownerEmails;
    private final boolean ownersConfigured;

    public UserService(AppUserRepository users,
                       @Value("${app.owner-phones:}") String ownerPhones,
                       @Value("${app.owner-emails:}") String ownerEmails) {
        this.users = users;
        this.ownerPhones = digits(ownerPhones);
        this.ownerEmails = lower(ownerEmails);
        this.ownersConfigured = !this.ownerPhones.isEmpty() || !this.ownerEmails.isEmpty();
    }

    public AppUser bySubject(String subject) {
        return users.findBySubject(subject)
                .orElseThrow(() -> new NotFoundException("Unknown user"));
    }

    @Transactional
    public AppUser upsertGoogle(String sub, String email, String name, String picture) {
        String subject = "google:" + sub;
        AppUser user = users.findBySubject(subject).orElseGet(AppUser::new);
        if (user.getId() == null) {
            user.setSubject(subject);
            user.setProvider("google");
            user.setRole(roleForEmail(email));
            user.setCreated(System.currentTimeMillis());
        }
        user.setName(name != null ? name : (email != null ? email : "Member"));
        user.setEmail(email);
        user.setPicture(picture);
        return users.save(user);
    }

    @Transactional
    public AppUser upsertPhone(String phone) {
        String subject = "phone:" + phone;
        AppUser user = users.findBySubject(subject).orElseGet(AppUser::new);
        if (user.getId() == null) {
            user.setSubject(subject);
            user.setProvider("phone");
            user.setRole(roleForPhone(phone));
            user.setName("Member " + phone);
            user.setCreated(System.currentTimeMillis());
        }
        user.setPhone(phone);
        return users.save(user);
    }

    /**
     * Owners are configured via {@code app.owner-phones} / {@code app.owner-emails}.
     * When neither is set we fall back to "first user is OWNER, everyone after is STAFF".
     */
    private String roleForPhone(String phone) {
        if (!ownersConfigured) {
            return defaultRoleForNewUser();
        }
        return ownerPhones.contains(phone) ? OWNER : STAFF;
    }

    private String roleForEmail(String email) {
        if (!ownersConfigured) {
            return defaultRoleForNewUser();
        }
        String e = email == null ? "" : email.toLowerCase();
        return ownerEmails.contains(e) ? OWNER : STAFF;
    }

    private String defaultRoleForNewUser() {
        return users.count() == 0 ? OWNER : STAFF;
    }

    private static Set<String> digits(String csv) {
        Set<String> out = new HashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String d = part.replaceAll("\\D", "");
                if (!d.isEmpty()) {
                    out.add(d);
                }
            }
        }
        return out;
    }

    private static Set<String> lower(String csv) {
        Set<String> out = new HashSet<>();
        if (csv != null) {
            for (String part : csv.split(",")) {
                String d = part.trim().toLowerCase();
                if (!d.isEmpty()) {
                    out.add(d);
                }
            }
        }
        return out;
    }
}
