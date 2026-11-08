package com.uap.proiv.jobs.service.impl;

import com.uap.proiv.jobs.client.UserApiRepository;
import com.uap.proiv.jobs.dto.User;
import com.uap.proiv.jobs.dto.UserApiResponse;
import com.uap.proiv.jobs.dto.UserRequest;
import com.uap.proiv.jobs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserApiRepository userApiRepository;

    @Autowired
    public UserServiceImpl(UserApiRepository userApiRepository) {
        this.userApiRepository = userApiRepository;
    }


    @Override
    public UserApiResponse search(int page) {
        UserApiResponse userApiResponse = userApiRepository.getUsers(page);
        int id = 1;
       for (User user:userApiResponse.getData()){
            user.setJobId(id);
            id++;
        };
        return userApiResponse;
    }

    @Override
    public User searchById(int id) {
        User user =  userApiRepository.getUserById(id);
        user.setJobId(1);
        return user;
    }

    @Override
    public void update(User user) {
        try {
            userApiRepository.updateUser( user);
        } catch (Exception e) {
            throw new RuntimeException("Error al crear el usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public User add(UserRequest request) {
        User user = new User();
        user.setId(99);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setJobId(request.getJobId());
        return user;
    }
}
