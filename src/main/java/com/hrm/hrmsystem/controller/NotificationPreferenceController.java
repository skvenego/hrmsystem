package com.hrm.hrmsystem.controller;

import com.hrm.hrmsystem.model.NotificationPreference;
import com.hrm.hrmsystem.model.User;
import com.hrm.hrmsystem.repository.NotificationPreferenceRepository;
import com.hrm.hrmsystem.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceRepository preferenceRepo;
    private final UserRepository userRepo;

    public NotificationPreferenceController(NotificationPreferenceRepository preferenceRepo, UserRepository userRepo) {
        this.preferenceRepo = preferenceRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public ResponseEntity<NotificationPreference> getPreferences(org.springframework.security.core.Authentication auth) {
        User user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        NotificationPreference pref = preferenceRepo.findByUser(user)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setUser(user);
                    return preferenceRepo.save(newPref);
                });
        
        return ResponseEntity.ok(pref);
    }

    @PutMapping
    public ResponseEntity<NotificationPreference> updatePreferences(
            org.springframework.security.core.Authentication auth,
            @RequestBody NotificationPreference prefDetails) {
        
        User user = userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        NotificationPreference pref = preferenceRepo.findByUser(user)
                .orElseGet(() -> {
                    NotificationPreference newPref = new NotificationPreference();
                    newPref.setUser(user);
                    return newPref;
                });
        
        if (prefDetails.getEmailNotifications() != null) pref.setEmailNotifications(prefDetails.getEmailNotifications());
        if (prefDetails.getPushNotifications() != null) pref.setPushNotifications(prefDetails.getPushNotifications());
        if (prefDetails.getSmsNotifications() != null) pref.setSmsNotifications(prefDetails.getSmsNotifications());
        if (prefDetails.getLeaveUpdates() != null) pref.setLeaveUpdates(prefDetails.getLeaveUpdates());
        if (prefDetails.getPayrollUpdates() != null) pref.setPayrollUpdates(prefDetails.getPayrollUpdates());
        if (prefDetails.getAttendanceUpdates() != null) pref.setAttendanceUpdates(prefDetails.getAttendanceUpdates());
        if (prefDetails.getMonthlyReports() != null) pref.setMonthlyReports(prefDetails.getMonthlyReports());
        
        return ResponseEntity.ok(preferenceRepo.save(pref));
    }
}
