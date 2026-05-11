package com.example.smart_cinema_booking_system.service;

import com.example.smart_cinema_booking_system.dto.ProfileDTO;
import com.example.smart_cinema_booking_system.model.UserProfile;
import com.example.smart_cinema_booking_system.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public UserProfile getProfileByUserId(Integer userId) {
        return userProfileRepository.findById(userId).orElse(null);
    }

    @Transactional
    public void updateProfile(ProfileDTO profileDTO) {
        UserProfile profile = userProfileRepository.findById(profileDTO.getUserId()).orElse(new UserProfile());
        
        profile.setFullName(profileDTO.getFullName());
        profile.setPhone(profileDTO.getPhone());
        profile.setAddress(profileDTO.getAddress());
        
        userProfileRepository.save(profile);
    }

    public void saveProfile(UserProfile profile) {
        userProfileRepository.save(profile);
    }
}
