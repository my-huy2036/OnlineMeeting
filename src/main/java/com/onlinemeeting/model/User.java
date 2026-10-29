
package com.onlinemeeting.model;

public class User {
    public int userId;
    public String username;
    public String displayName;
    public String email;
    public String userRole;
    public String accountStatus;

    public User() {
    }

    public User(int userId, String username, String displayName,
                String email, String userRole, String accountStatus) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.userRole = userRole;
        this.accountStatus = accountStatus;
    }
}
