package com.llenroctech.customerconnect.query;

public class UserQuery {

    public static final String COUNT_USER_EMAIL_QUERY =
            "SELECT COUNT(*) FROM Users WHERE email = :email";

    public static final String INSERT_USER_QUERY =
            "INSERT INTO Users (first_name, last_name, email, password) " +
                    "VALUES (:firstName, :lastName, :email, :password)";

    public static final String INSERT_ACCOUNT_VERIFICATION_URL_QUERY =
            "INSERT INTO AccountVerifications (user_id, url, `date`) " +
                    "VALUES (:userId, :url, NOW())";

    public static final String SELECT_USER_BY_EMAIL_QUERY =
            "SELECT * FROM Users WHERE email = :email";
}