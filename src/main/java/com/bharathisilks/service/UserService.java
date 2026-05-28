package com.bharathisilks.service;

import com.bharathisilks.domain.AppUser;
import com.bharathisilks.error.NotFoundException;
import com.bharathisilks.repo.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String OWNER_ROLE = "OWNER";
    private static final String STAFF_ROLE = "STAFF";

    private final AppUserRepository users;

    public UserService(AppUserRepository users) {
        this.users = users;
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
            user.setRole(defaultRoleForNewUser());
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
            user.setRole(defaultRoleForNewUser());
            user.setName("Member " + phone);
            user.setCreated(System.currentTimeMillis());
        }
        user.setPhone(phone);
        return users.save(user);
    }

    private String defaultRoleForNewUser() {
        return users.count() == 0 ? OWNER_ROLE : STAFF_ROLE;
    }
}
