package com.example.collab_spire_.listeners;

import com.example.collab_spire_.models.User;

public interface UsersListener {

    void initiateVideoMeeting(User user);

    void initiateAudioMeeting(User user);

    void onMultipleUserSelection(Boolean ismultipleUsersSelected);

}